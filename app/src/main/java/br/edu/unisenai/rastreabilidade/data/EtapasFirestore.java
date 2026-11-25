package br.edu.unisenai.rastreabilidade.data;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.edu.unisenai.rastreabilidade.model.Etapa;

public final class EtapasFirestore {
    private final FirebaseFirestore banco = FirebaseFirestore.getInstance();
    private final CollectionReference etapas = banco.collection("etapas");

    public void carregar(OnSuccessListener<List<Etapa>> aoCarregar, OnFailureListener aoFalhar) {
        etapas.orderBy("ordem").get().addOnSuccessListener(resultado -> {
            List<Etapa> sequencia = new ArrayList<>();
            for (DocumentSnapshot documento : resultado.getDocuments()) {
                String nome = documento.getString("nome");
                Long ordem = documento.getLong("ordem");
                if (nome == null || nome.trim().isEmpty() || ordem == null
                        || ordem < 1 || ordem > Integer.MAX_VALUE) {
                    aoFalhar.onFailure(new IllegalStateException("A sequência de etapas salva é inválida."));
                    return;
                }
                sequencia.add(new Etapa(nome, ordem.intValue()));
            }
            for (int i = 0; i < sequencia.size(); i++) {
                if (sequencia.get(i).ordem != i + 1) {
                    aoFalhar.onFailure(new IllegalStateException("A sequência de etapas salva é inválida."));
                    return;
                }
            }
            aoCarregar.onSuccess(sequencia);
        }).addOnFailureListener(aoFalhar);
    }

    public void salvar(List<Etapa> novaSequencia, OnSuccessListener<Void> aoSalvar,
                       OnFailureListener aoFalhar) {
        String erro = validar(novaSequencia);
        if (erro != null) {
            aoFalhar.onFailure(new IllegalArgumentException(erro));
            return;
        }

        etapas.get().addOnSuccessListener(resultado -> {
            WriteBatch lote = banco.batch();
            for (Etapa etapa : novaSequencia) {
                // O ID igual à ordem atualiza a etapa naquela posição.
                String id = String.valueOf(etapa.ordem);
                lote.set(etapas.document(id), Map.of(
                        "nome", etapa.nome.trim(),
                        "ordem", etapa.ordem
                ));
            }
            for (DocumentSnapshot documento : resultado.getDocuments()) {
                boolean manter = false;
                for (Etapa etapa : novaSequencia) {
                    if (String.valueOf(etapa.ordem).equals(documento.getId())) {
                        manter = true;
                        break;
                    }
                }
                if (!manter) {
                    lote.delete(documento.getReference());
                }
            }
            lote.commit().addOnSuccessListener(aoSalvar).addOnFailureListener(aoFalhar);
        }).addOnFailureListener(aoFalhar);
    }

    private String validar(List<Etapa> sequencia) {
        if (sequencia == null || sequencia.isEmpty()) {
            return "Cadastre pelo menos uma etapa.";
        }

        for (int i = 0; i < sequencia.size(); i++) {
            Etapa etapa = sequencia.get(i);
            if (etapa == null || etapa.nome == null || etapa.nome.trim().isEmpty()) {
                return "Informe o nome de todas as etapas.";
            }
            if (etapa.ordem < 1) {
                return "A ordem das etapas deve começar em 1.";
            }
            for (int j = i + 1; j < sequencia.size(); j++) {
                Etapa outraEtapa = sequencia.get(j);
                if (outraEtapa != null && outraEtapa.ordem == etapa.ordem) {
                    return "Não repita a ordem de uma etapa.";
                }
            }
        }

        for (int ordem = 1; ordem <= sequencia.size(); ordem++) {
            boolean encontrada = false;
            for (Etapa etapa : sequencia) {
                if (etapa.ordem == ordem) {
                    encontrada = true;
                    break;
                }
            }
            if (!encontrada) {
                return "Use ordens consecutivas a partir de 1.";
            }
        }
        return null;
    }
}
