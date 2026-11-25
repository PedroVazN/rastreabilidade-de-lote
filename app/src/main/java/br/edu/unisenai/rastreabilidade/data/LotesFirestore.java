package br.edu.unisenai.rastreabilidade.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

import br.edu.unisenai.rastreabilidade.model.Etapa;
import br.edu.unisenai.rastreabilidade.model.Lote;
import br.edu.unisenai.rastreabilidade.model.SequenciaEtapas;

public final class LotesFirestore {
    private final CollectionReference lotes = FirebaseFirestore.getInstance().collection("lotes");
    private final SharedPreferences etiquetas;
    private final String uid;

    public LotesFirestore(Context context, String uid) {
        etiquetas = context.getSharedPreferences("etiquetas_firestore", Context.MODE_PRIVATE);
        this.uid = uid;
    }

    public Task<Void> criar(String produto, int quantidade) {
        DocumentReference documento = lotes.document();
        DocumentReference configuracaoEtapas = FirebaseFirestore.getInstance()
                .collection("configuracao").document("etapas");
        return FirebaseFirestore.getInstance().<Void>runTransaction(transacao -> {
            DocumentSnapshot configuracao = transacao.get(configuracaoEtapas);
            if (!configuracao.exists()) {
                throw new IllegalStateException("A Qualidade ainda não cadastrou as etapas.");
            }
            List<Etapa> etapas = SequenciaEtapas.deFirestore(configuracao.get("etapas"));
            transacao.set(documento, Map.of(
                    "produto", produto,
                    "quantidade", quantidade,
                    "criadoEm", FieldValue.serverTimestamp(),
                    "criadoPor", uid,
                    "status", "ativo",
                    "etapaAtual", 0,
                    "etapas", SequenciaEtapas.paraFirestore(etapas)
            ));
            return null;
        }).continueWithTask(tarefa -> {
            if (tarefa.isSuccessful()) {
                etiquetas.edit().putString(uid, documento.getId()).apply();
            }
            return tarefa;
        });
    }

    public DocumentReference ultimaEtiqueta() {
        String id = etiquetas.getString(uid, null);
        return id == null ? null : lotes.document(id);
    }

    public static Lote converter(DocumentSnapshot documento) {
        String produto = documento.getString("produto");
        Long quantidade = documento.getLong("quantidade");
        Timestamp data = documento.getTimestamp("criadoEm");
        if (produto == null || quantidade == null || quantidade < 1
                || quantidade > Integer.MAX_VALUE || data == null) {
            return null;
        }
        return new Lote(documento.getId(), produto, quantidade.intValue(), data.toDate().getTime());
    }
}
