package br.edu.unisenai.rastreabilidade.data;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import br.edu.unisenai.rastreabilidade.model.Evento;

public final class HistoricoFirestore {
    public static final String REPROVADO = "reprovado";

    private final CollectionReference lotes = FirebaseFirestore.getInstance().collection("lotes");

    public static boolean codigoValido(String id) {
        return id.matches("[A-Za-z0-9_-]{1,64}") && !id.matches("__.*__");
    }

    public DocumentReference lote(String id) {
        return lotes.document(id);
    }

    public Query eventos(String id) {
        return lotes.document(id).collection("historico").orderBy("registradoEm");
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
