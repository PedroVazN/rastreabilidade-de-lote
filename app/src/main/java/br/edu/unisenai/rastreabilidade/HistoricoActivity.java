package br.edu.unisenai.rastreabilidade;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import br.edu.unisenai.rastreabilidade.data.HistoricoExemplo;
import br.edu.unisenai.rastreabilidade.data.LotesLocais;
import br.edu.unisenai.rastreabilidade.model.Evento;

public class HistoricoActivity extends Activity {
    public static final String EXTRA_LOTE = "loteId";

    private final HistoricoExemplo.Ouvinte ouvinte = this::mostrar;
    private EditText codigo;
    private TextView situacao;
    private TextView vazio;
    private HistoricoAdapter adapter;
    private String loteAtual;

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
    protected void onSaveInstanceState(Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putString(EXTRA_LOTE, loteAtual);
    }

    @Override
    protected void onDestroy() {
        HistoricoExemplo.parar(ouvinte);
        super.onDestroy();
    }

    private void consultar() {
        String id = codigo.getText().toString().trim();
        if (id.isEmpty()) {
            limpar();
            codigo.setError(getString(R.string.erro_codigo));
            codigo.requestFocus();
            return;
        }
        if (!HistoricoExemplo.existe(id)) {
            if (new LotesLocais(this).buscar(id) == null) {
                limpar();
                codigo.setError(getString(R.string.erro_lote_nao_encontrado));
                codigo.requestFocus();
                return;
            }
            HistoricoExemplo.incluir(id);
        }
        loteAtual = id;
        codigo.setError(null);
        getSystemService(InputMethodManager.class)
                .hideSoftInputFromWindow(codigo.getWindowToken(), 0);
        HistoricoExemplo.ouvir(id, ouvinte);
    }

    private void limpar() {
        HistoricoExemplo.parar(ouvinte);
        loteAtual = null;
        situacao.setVisibility(View.GONE);
        vazio.setVisibility(View.GONE);
        adapter.mostrar(List.of());
    }

    private void mostrar(String status, List<Evento> eventos) {
        situacao.setText(HistoricoExemplo.REPROVADO.equals(status)
                ? R.string.situacao_reprovado : R.string.situacao_ativo);
        situacao.setVisibility(View.VISIBLE);
        vazio.setVisibility(eventos.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.mostrar(eventos);
    }
}
