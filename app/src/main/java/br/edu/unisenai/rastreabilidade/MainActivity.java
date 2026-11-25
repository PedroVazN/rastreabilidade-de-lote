package br.edu.unisenai.rastreabilidade;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.edu.unisenai.rastreabilidade.data.EtapasFirestore;
import br.edu.unisenai.rastreabilidade.data.LotesFirestore;
import br.edu.unisenai.rastreabilidade.model.Etapa;
import br.edu.unisenai.rastreabilidade.model.Lote;
import br.edu.unisenai.rastreabilidade.model.SequenciaEtapas;

public class MainActivity extends Activity {
    private EditText produto;
    private EditText quantidade;
    private LotesFirestore lotes;
    private EtapasFirestore etapasFirestore;
    private EditText sequenciaEtapas;
    private TextView sequenciaAtual;
    private Button salvarEtapas;
    private Button criar;
    private boolean operador;
    private Task<Void> gravacao;
    private ListenerRegistration etiquetaListener;
    private ListenerRegistration etapasListener;
    private boolean rolarAteEtiqueta;
    private boolean alterandoSequencia;
    private boolean edicaoSequencia;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        String perfil = getIntent().getStringExtra(LoginActivity.EXTRA_PERFIL);
        if (usuario == null || !usuario.getUid().equals(getIntent().getStringExtra(LoginActivity.EXTRA_UID))
                || (!LoginActivity.OPERADOR.equals(perfil) && !LoginActivity.QUALIDADE.equals(perfil))) {
            voltarAoLogin();
            return;
        }
        setContentView(R.layout.activity_main);
        findViewById(R.id.tela).setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets barras = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        produto = findViewById(R.id.produto);
        quantidade = findViewById(R.id.quantidade);
        lotes = new LotesFirestore(this, usuario.getUid());
        etapasFirestore = new EtapasFirestore();
        operador = LoginActivity.OPERADOR.equals(perfil);
        ((TextView) findViewById(R.id.perfil_atual)).setText(operador
                ? R.string.perfil_operador : R.string.perfil_qualidade);
        findViewById(R.id.form_lote).setVisibility(operador ? View.VISIBLE : View.GONE);
        findViewById(R.id.form_etapas).setVisibility(operador ? View.GONE : View.VISIBLE);
        sequenciaEtapas = findViewById(R.id.sequencia_etapas);
        sequenciaEtapas.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence texto, int inicio, int quantidade, int depois) {
            }

            @Override
            public void onTextChanged(CharSequence texto, int inicio, int antes, int quantidade) {
                if (!alterandoSequencia) {
                    edicaoSequencia = true;
                }
            }

            @Override
            public void afterTextChanged(Editable texto) {
            }
        });
        sequenciaAtual = findViewById(R.id.sequencia_atual);
        salvarEtapas = findViewById(R.id.salvar_etapas);
        salvarEtapas.setOnClickListener(view -> salvarEtapas());
        findViewById(R.id.sair).setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            voltarAoLogin();
        });
        criar = findViewById(R.id.criar);
        criar.setOnClickListener(view -> criarLote());
        findViewById(R.id.consultar_historico).setOnClickListener(view ->
                startActivity(new Intent(this, HistoricoActivity.class)
                        .putExtra(LoginActivity.EXTRA_PERFIL, perfil)));

        Object pendente = getLastNonConfigurationInstance();
        if (pendente instanceof GravacaoAnterior) {
            gravacao = ((GravacaoAnterior) pendente).tarefa;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (operador) {
            observarEtiqueta();
            if (gravacao != null) {
                acompanharGravacao();
            }
        } else {
            observarEtapas();
        }
    }

    @Override
    protected void onStop() {
        pararObservacao();
        pararObservacaoEtapas();
        super.onStop();
    }

    private void observarEtapas() {
        pararObservacaoEtapas();
        etapasListener = etapasFirestore.documento().addSnapshotListener(this, (snapshot, erro) -> {
            if (erro != null) {
                sequenciaAtual.setText(R.string.erro_carregar_etapas);
                return;
            }
            if (snapshot == null) {
                sequenciaAtual.setText(R.string.erro_carregar_etapas);
                return;
            }
            if (!snapshot.exists()) {
                sequenciaAtual.setText(R.string.etapas_nao_cadastradas);
                return;
            }
            try {
                List<Etapa> etapas = SequenciaEtapas.deFirestore(snapshot.get("etapas"));
                if (!edicaoSequencia) {
                    alterandoSequencia = true;
                    sequenciaEtapas.setText(SequenciaEtapas.paraTexto(etapas));
                    alterandoSequencia = false;
                }
                sequenciaAtual.setText(getString(R.string.etapas_atuais,
                        SequenciaEtapas.paraTexto(etapas)));
            } catch (IllegalArgumentException e) {
                alterandoSequencia = false;
                sequenciaAtual.setText(R.string.erro_carregar_etapas);
            }
        });
    }

    private void pararObservacaoEtapas() {
        if (etapasListener != null) {
            etapasListener.remove();
            etapasListener = null;
        }
    }

    private void salvarEtapas() {
        List<Etapa> etapas;
        try {
            etapas = SequenciaEtapas.analisar(sequenciaEtapas.getText().toString());
        } catch (IllegalArgumentException e) {
            sequenciaEtapas.setError(getString(R.string.erro_sequencia_etapas));
            sequenciaEtapas.requestFocus();
            return;
        }
        salvarEtapas.setEnabled(false);
        sequenciaEtapas.setEnabled(false);
        etapasFirestore.salvar(etapas).addOnCompleteListener(this, tarefa -> {
            salvarEtapas.setEnabled(true);
            sequenciaEtapas.setEnabled(true);
            if (tarefa.isSuccessful()) {
                edicaoSequencia = false;
                Toast.makeText(this, R.string.etapas_salvas, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.erro_salvar_etapas, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return gravacao == null ? null : new GravacaoAnterior(gravacao);
    }

    private void voltarAoLogin() {
        startActivity(new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    private void criarLote() {
        if (!operador || gravacao != null) {
            return;
        }
        String nome = produto.getText().toString().trim();
        if (nome.isEmpty()) {
            produto.setError(getString(R.string.erro_produto));
            produto.requestFocus();
            return;
        }

        int total;
        try {
            total = Integer.parseInt(quantidade.getText().toString().trim());
        } catch (NumberFormatException e) {
            total = 0;
        }
        if (total <= 0) {
            quantidade.setError(getString(R.string.erro_quantidade));
            quantidade.requestFocus();
            return;
        }

        gravacao = lotes.criar(nome, total);
        acompanharGravacao();
    }

    private void acompanharGravacao() {
        criar.setEnabled(false);
        produto.setEnabled(false);
        quantidade.setEnabled(false);
        criar.setText(R.string.salvando_lote);
        gravacao.addOnCompleteListener(this, tarefa -> {
            gravacao = null;
            criar.setEnabled(true);
            produto.setEnabled(true);
            quantidade.setEnabled(true);
            criar.setText(R.string.criar);
            if (!tarefa.isSuccessful()) {
                Toast.makeText(this, R.string.erro_gravar, Toast.LENGTH_LONG).show();
                return;
            }
            produto.setText("");
            quantidade.setText("");
            getSystemService(InputMethodManager.class).hideSoftInputFromWindow(produto.getWindowToken(), 0);
            rolarAteEtiqueta = true;
            observarEtiqueta();
        });
    }

    private void observarEtiqueta() {
        pararObservacao();
        DocumentReference documento = lotes.ultimaEtiqueta();
        if (documento == null) {
            return;
        }
        etiquetaListener = documento.addSnapshotListener(this, (snapshot, erro) -> {
            if (erro != null || snapshot == null || !snapshot.exists()) {
                findViewById(R.id.etiqueta).setVisibility(View.GONE);
                return;
            }
            Lote lote = LotesFirestore.converter(snapshot);
            if (lote != null) {
                mostrarEtiqueta(lote, gerarQr(lote.id));
                if (rolarAteEtiqueta) {
                    rolarAteEtiqueta = false;
                    android.widget.ScrollView tela = findViewById(R.id.tela);
                    tela.post(() -> tela.fullScroll(View.FOCUS_DOWN));
                }
            }
        });
    }

    private void pararObservacao() {
        if (etiquetaListener != null) {
            etiquetaListener.remove();
            etiquetaListener = null;
        }
    }

    private static final class GravacaoAnterior {
        final Task<Void> tarefa;

        GravacaoAnterior(Task<Void> tarefa) {
            this.tarefa = tarefa;
        }
    }

    private Bitmap gerarQr(String id) {
        try {
            int lado = 264;
            BitMatrix matriz = new QRCodeWriter().encode(id, BarcodeFormat.QR_CODE, lado, lado);
            int[] pixels = new int[lado * lado];
            for (int y = 0; y < lado; y++) {
                for (int x = 0; x < lado; x++) {
                    pixels[y * lado + x] = matriz.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }
            return Bitmap.createBitmap(pixels, lado, lado, Bitmap.Config.ARGB_8888);
        } catch (WriterException e) {
            throw new IllegalStateException("QR de ID de lote inválido", e);
        }
    }

    private void mostrarEtiqueta(Lote lote, Bitmap qr) {
        String data = DateFormat.getDateInstance(DateFormat.SHORT, Locale.forLanguageTag("pt-BR"))
                .format(new Date(lote.criadoEm));
        String resumo = getResources().getQuantityString(R.plurals.resumo, lote.quantidade,
                lote.produto, lote.quantidade, data);
        ((TextView) findViewById(R.id.resumo)).setText(resumo);
        ((TextView) findViewById(R.id.codigo)).setText(lote.id);
        ((ImageView) findViewById(R.id.qr)).setImageBitmap(qr);
        findViewById(R.id.ver_historico).setOnClickListener(view -> startActivity(
                new Intent(this, HistoricoActivity.class).putExtra(HistoricoActivity.EXTRA_LOTE, lote.id)));
        findViewById(R.id.etiqueta).setVisibility(View.VISIBLE);
    }
}
