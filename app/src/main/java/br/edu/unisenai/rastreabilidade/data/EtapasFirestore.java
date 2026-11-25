package br.edu.unisenai.rastreabilidade.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

import br.edu.unisenai.rastreabilidade.model.Etapa;
import br.edu.unisenai.rastreabilidade.model.SequenciaEtapas;

public final class EtapasFirestore {
    private final DocumentReference documento = FirebaseFirestore.getInstance()
            .collection("configuracao").document("etapas");

    public DocumentReference documento() {
        return documento;
    }

    public Task<Void> salvar(List<Etapa> etapas) {
        return documento.set(Map.of("etapas", SequenciaEtapas.paraFirestore(etapas)));
    }
}
