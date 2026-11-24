package br.edu.unisenai.rastreabilidade;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;

import java.util.ArrayList;
import java.util.List;

import br.edu.unisenai.rastreabilidade.data.HistoricoFirestore;
import br.edu.unisenai.rastreabilidade.model.Evento;

public class HistoricoActivity extends Activity {
    public static final String EXTRA_LOTE = "loteId";

    private final HistoricoFirestore historico = new HistoricoFirestore();
    private EditText codigo;
    private TextView situacao;
    private TextView vazio;
    private HistoricoAdapter adapter;
    private ListenerRegistration loteListener;
    private ListenerRegistration eventosListener;
    private String loteAtual;
    private Boolean reprovado;
    private List<Evento> eventos;
    private boolean loteLocal;
    private boolean eventosLocais;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        setContentView(R.layout.activity_historico);
        findViewById(R.id.tela).setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets barras = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        codigo = findViewById(R.id.codigo);
        situacao = findViewById(R.id.situacao);
        vazio = findViewById(R.id.vazio);
        adapter = new HistoricoAdapter();
        RecyclerView lista = findViewById(R.id.eventos);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adapter);
        findViewById(R.id.consultar).setOnClickListener(view -> consultar());
        codigo.setOnEditorActionListener((view, acao, evento) -> {
            if (acao == EditorInfo.IME_ACTION_SEARCH) {
                consultar();
                return true;
            }
            return false;
        });

        String inicial = estado != null ? estado.getString(EXTRA_LOTE)
                : getIntent().getStringExtra(EXTRA_LOTE);
        if (inicial != null) {
            codigo.setText(inicial);
            consultar();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (loteAtual != null && loteListener == null) {
            ouvir();
        }
    }

    @Override
    protected void onStop() {
        parar();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putString(EXTRA_LOTE, loteAtual);
    }

    private void consultar() {
        String id = codigo.getText().toString().trim();
        limpar();
        if (id.isEmpty()) {
            codigo.setError(getString(R.string.erro_codigo));
            codigo.requestFocus();
            return;
        }
        if (!HistoricoFirestore.codigoValido(id)) {
            naoEncontrado();
            return;
        }
        loteAtual = id;
        codigo.setError(null);
        getSystemService(InputMethodManager.class)
                .hideSoftInputFromWindow(codigo.getWindowToken(), 0);
        ouvir();
        mostrar();
    }

    private void ouvir() {
        parar();
        loteListener = historico.lote(loteAtual).addSnapshotListener(MetadataChanges.INCLUDE,
                (documento, erro) -> {
                    if (erro != null || documento == null) {
                        falhar();
                        return;
                    }
                    loteLocal = documento.getMetadata().isFromCache();
                    if (!documento.exists()) {
                        if (!loteLocal) {
                            naoEncontrado();
                            return;
                        }
                        reprovado = null;
                    } else {
                        reprovado = HistoricoFirestore.REPROVADO.equals(documento.getString("status"));
                    }
                    mostrar();
                });
        eventosListener = historico.eventos(loteAtual).addSnapshotListener(MetadataChanges.INCLUDE,
                (consulta, erro) -> {
                    if (erro != null || consulta == null) {
                        falhar();
                        return;
                    }
                    List<Evento> lidos = new ArrayList<>();
                    for (DocumentSnapshot documento : consulta.getDocuments()) {
                        Evento evento = HistoricoFirestore.converter(documento);
                        if (evento != null) {
                            lidos.add(evento);
                        }
                    }
                    eventos = lidos;
                    eventosLocais = consulta.getMetadata().isFromCache();
                    mostrar();
                });
    }

    private void parar() {
        if (loteListener != null) {
            loteListener.remove();
            loteListener = null;
        }
        if (eventosListener != null) {
            eventosListener.remove();
            eventosListener = null;
        }
    }

    private void limpar() {
        parar();
        loteAtual = null;
        reprovado = null;
        eventos = null;
        situacao.setVisibility(View.GONE);
        vazio.setVisibility(View.GONE);
        adapter.mostrar(List.of());
    }

    private void naoEncontrado() {
        limpar();
        codigo.setError(getString(R.string.erro_lote_nao_encontrado));
        codigo.requestFocus();
    }

    private void falhar() {
        limpar();
        Toast.makeText(this, R.string.erro_historico, Toast.LENGTH_LONG).show();
    }

    private void mostrar() {
        boolean carregado = reprovado != null && eventos != null;
        if (carregado) {
            situacao.setText(reprovado ? R.string.situacao_reprovado : R.string.situacao_ativo);
        }
        situacao.setVisibility(carregado ? View.VISIBLE : View.GONE);
        adapter.mostrar(carregado ? eventos : List.of());

        int aviso = 0;
        if (!carregado || (eventos.isEmpty() && (loteLocal || eventosLocais))) {
            aviso = R.string.aguardando_servidor;
        } else if (loteLocal || eventosLocais) {
            aviso = R.string.historico_local;
        } else if (eventos.isEmpty()) {
            aviso = R.string.historico_vazio;
        }
        if (aviso != 0) {
            vazio.setText(aviso);
        }
        vazio.setVisibility(aviso != 0 ? View.VISIBLE : View.GONE);
    }
}
