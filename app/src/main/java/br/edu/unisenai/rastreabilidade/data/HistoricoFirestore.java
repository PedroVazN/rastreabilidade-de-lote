package br.edu.unisenai.rastreabilidade.data;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;
import java.util.Map;

import br.edu.unisenai.rastreabilidade.model.Etapa;
import br.edu.unisenai.rastreabilidade.model.Evento;

public final class HistoricoFirestore {
    public static final String REPROVADO = "reprovado";
    // Mesmo valor usado por nomeAutor() em firestore.rules.
    private static final String NOME_PADRAO = "Qualidade";

    private final FirebaseFirestore banco = FirebaseFirestore.getInstance();
    private final CollectionReference lotes = banco.collection("lotes");

    public static boolean codigoValido(String id) {
        return id.matches("[A-Za-z0-9_-]{1,64}") && !id.matches("__.*__");
    }

    public DocumentReference lote(String id) {
        return lotes.document(id);
    }

    public Query eventos(String id) {
        return lotes.document(id).collection("historico").orderBy("registradoEm");
    }

    public Task<Etapa> carregarProximaEtapa(String loteId) {
        if (!codigoValido(loteId)) {
            return Tasks.forException(new IllegalArgumentException("Código de lote inválido."));
        }
        return lote(loteId).get(Source.SERVER).continueWith(tarefa -> {
            if (!tarefa.isSuccessful()) {
                throw tarefa.getException() == null
                        ? new IllegalStateException("Não foi possível consultar o lote.")
                        : tarefa.getException();
            }
            DocumentSnapshot documento = tarefa.getResult();
            if (documento == null || !documento.exists()) {
                throw new IllegalArgumentException("Lote não encontrado.");
            }
            return proximaEtapa(documento);
        });
    }

    public Task<Void> registrarProximaEtapa(String loteId, Etapa etapaEsperada, String uid) {
        if (!codigoValido(loteId)) {
            return Tasks.forException(new IllegalArgumentException("Código de lote inválido."));
        }
        return banco.runTransaction(transacao -> {
            DocumentReference referenciaLote = lote(loteId);
            DocumentSnapshot documento = transacao.get(referenciaLote);
            DocumentSnapshot perfil = transacao.get(banco.collection("usuarios").document(uid));
            Etapa etapaAtual = proximaEtapa(documento);
            if (etapaAtual.ordem != etapaEsperada.ordem
                    || !etapaAtual.nome.equals(etapaEsperada.nome)) {
                throw new IllegalStateException("A etapa do lote foi alterada. Leia o QR novamente.");
            }

            Object nomeSalvo = perfil.get("nome");
            String nome = nomeSalvo instanceof String ? (String) nomeSalvo : "";
            if (nome.trim().isEmpty() || nome.length() > 100) {
                nome = "Operador";
            }

            transacao.update(referenciaLote, "etapaAtual", etapaEsperada.ordem);
            transacao.set(referenciaLote.collection("historico")
                    .document(String.valueOf(etapaEsperada.ordem)), Map.of(
                    "tipo", Evento.ETAPA,
                    "etapa", etapaEsperada.nome,
                    "ordem", etapaEsperada.ordem,
                    "autorUid", uid,
                    "autorNome", nome,
                    "registradoEm", FieldValue.serverTimestamp()
            ));
            return null;
        });
    }

    private Etapa proximaEtapa(DocumentSnapshot documento) {
        if (documento == null || !documento.exists()
                || !"ativo".equals(documento.getString("status"))) {
            throw new IllegalArgumentException("Lote inexistente ou inativo.");
        }
        Long ordemAtual = documento.getLong("etapaAtual");
        Object etapasSalvas = documento.get("etapas");
        if (ordemAtual == null || ordemAtual < 0 || !(etapasSalvas instanceof List)) {
            throw new IllegalArgumentException("Sequência de etapas inválida.");
        }
        List<?> sequencia = (List<?>) etapasSalvas;
        if (ordemAtual >= sequencia.size()) {
            throw new IllegalArgumentException("O lote não possui próxima etapa.");
        }
        Object etapaSalva = sequencia.get(ordemAtual.intValue());
        if (!(etapaSalva instanceof Map)) {
            throw new IllegalArgumentException("Sequência de etapas inválida.");
        }
        Map<?, ?> dadosEtapa = (Map<?, ?>) etapaSalva;
        Object nome = dadosEtapa.get("nome");
        Object ordem = dadosEtapa.get("ordem");
        if (!(nome instanceof String) || ((String) nome).trim().isEmpty()
                || !(ordem instanceof Long) || ((Long) ordem) != ordemAtual + 1) {
            throw new IllegalArgumentException("Sequência de etapas inválida.");
        }
        return new Etapa((String) nome, ((Long) ordem).intValue());
    }

    public Task<Void> reprovar(String loteId, String motivo, String uid) {
        return banco.collection("usuarios").document(uid).get().continueWithTask(perfil -> {
            if (!perfil.isSuccessful()) {
                return Tasks.forException(perfil.getException());
            }
            Object salvo = perfil.getResult().get("nome");
            String nome = salvo instanceof String ? (String) salvo : "";
            if (nome.trim().isEmpty() || nome.length() > 100) {
                nome = NOME_PADRAO;
            }
            DocumentReference lote = lotes.document(loteId);
            WriteBatch reprovacao = banco.batch();
            reprovacao.update(lote, "status", REPROVADO);
            reprovacao.set(lote.collection("historico").document(Evento.REPROVACAO), Map.of(
                    "tipo", Evento.REPROVACAO,
                    "motivo", motivo,
                    "autorUid", uid,
                    "autorNome", nome,
                    "registradoEm", FieldValue.serverTimestamp()
            ));
            return reprovacao.commit();
        });
    }

    public static Evento converter(DocumentSnapshot documento) {
        String tipo = documento.getString("tipo");
        String autorUid = documento.getString("autorUid");
        String autorNome = documento.getString("autorNome");
        Timestamp data = documento.getTimestamp("registradoEm",
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
        if (autorNome == null || data == null) {
            return null;
        }
        long horario = data.toDate().getTime();
        if (Evento.REPROVACAO.equals(tipo)) {
            String motivo = documento.getString("motivo");
            return motivo == null ? null : Evento.reprovacao(motivo, autorUid, autorNome, horario);
        }
        String etapa = documento.getString("etapa");
        Long ordem = documento.getLong("ordem");
        if (!Evento.ETAPA.equals(tipo) || etapa == null || ordem == null) {
            return null;
        }
        return Evento.etapa(etapa, ordem.intValue(), autorUid, autorNome, horario);
    }
}
