package br.edu.unisenai.rastreabilidade;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.edu.unisenai.rastreabilidade.data.EtapasFirestore;
import br.edu.unisenai.rastreabilidade.data.HistoricoFirestore;
import br.edu.unisenai.rastreabilidade.data.LotesFirestore;
import br.edu.unisenai.rastreabilidade.model.Etapa;
import br.edu.unisenai.rastreabilidade.model.Lote;

public class MainActivity extends Activity {
    private EditText produto;
    private EditText quantidade;
    private EditText etapasSequencia;
    private LotesFirestore lotes;
    private EtapasFirestore etapas;
    private final HistoricoFirestore historico = new HistoricoFirestore();
    private Button criar;
    private Button lerQr;
    private Button salvarEtapas;
    private boolean operador;
    private String uid;
    private boolean carregandoEtapas;
    private boolean editouEtapasDuranteCarregamento;
    private Task<Void> gravacao;
    private ListenerRegistration etiquetaListener;
    private boolean rolarAteEtiqueta;

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
        uid = usuario.getUid();
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
        etapas = new EtapasFirestore();
        etapasSequencia = findViewById(R.id.etapas_sequencia);
        etapasSequencia.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence texto, int inicio, int quantidade, int depois) {
            }

            @Override
            public void onTextChanged(CharSequence texto, int inicio, int antes, int quantidade) {
                if (carregandoEtapas) {
                    editouEtapasDuranteCarregamento = true;
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable texto) {
            }
        });
        operador = LoginActivity.OPERADOR.equals(perfil);
        ((TextView) findViewById(R.id.perfil_atual)).setText(operador
                ? R.string.perfil_operador : R.string.perfil_qualidade);
        findViewById(R.id.form_lote).setVisibility(operador ? View.VISIBLE : View.GONE);
        findViewById(R.id.form_etapas).setVisibility(operador ? View.GONE : View.VISIBLE);
        findViewById(R.id.sair).setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            voltarAoLogin();
        });
        criar = findViewById(R.id.criar);
        criar.setOnClickListener(view -> criarLote());
        lerQr = findViewById(R.id.ler_qr);
        lerQr.setOnClickListener(view -> abrirLeitorQr());
        findViewById(R.id.consultar_historico).setOnClickListener(view ->
                startActivity(new Intent(this, HistoricoActivity.class)
                        .putExtra(LoginActivity.EXTRA_PERFIL, perfil)));
        salvarEtapas = findViewById(R.id.btn_salvar_etapas);
        salvarEtapas.setOnClickListener(view -> salvarSequencia());
        if (!operador) {
            carregarSequencia();
        }

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
        }
    }

    @Override
    protected void onStop() {
        pararObservacao();
        super.onStop();
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

        int quantidadeLote = total;
        criar.setEnabled(false);
        criar.setText(R.string.salvando_lote);
        etapas.carregar(sequencia -> {
            if (sequencia.isEmpty()) {
                criar.setEnabled(true);
                criar.setText(R.string.criar);
                Toast.makeText(this, R.string.erro_etapas_nao_cadastradas, Toast.LENGTH_LONG).show();
                return;
            }
            gravacao = lotes.criar(nome, quantidadeLote, sequencia);
            acompanharGravacao();
        }, erro -> {
            criar.setEnabled(true);
            criar.setText(R.string.criar);
            Toast.makeText(this, R.string.erro_carregar_etapas, Toast.LENGTH_LONG).show();
        });
    }

    private void carregarSequencia() {
        carregandoEtapas = true;
        editouEtapasDuranteCarregamento = false;
        etapas.carregar(sequencia -> {
            carregandoEtapas = false;
            if (editouEtapasDuranteCarregamento) {
                return;
            }
            StringBuilder texto = new StringBuilder();
            for (Etapa etapa : sequencia) {
                if (texto.length() > 0) {
                    texto.append('\n');
                }
                texto.append(etapa.nome);
            }
            etapasSequencia.setText(texto.toString());
        }, erro -> {
            carregandoEtapas = false;
            Toast.makeText(this, R.string.erro_carregar_etapas, Toast.LENGTH_LONG).show();
        });
    }

    private void salvarSequencia() {
        if (operador) {
            return;
        }

        String texto = etapasSequencia.getText().toString();
        if (texto.isEmpty()) {
            etapasSequencia.setError(getString(R.string.erro_etapas_vazias));
            etapasSequencia.requestFocus();
            return;
        }

        String[] nomes = texto.split("\\R", -1);
        List<Etapa> sequencia = new ArrayList<>();
        for (int i = 0; i < nomes.length; i++) {
            String nome = nomes[i].trim();
            if (nome.isEmpty()) {
                etapasSequencia.setError(getString(R.string.erro_etapas_vazias));
                etapasSequencia.requestFocus();
                return;
            }
            sequencia.add(new Etapa(nome, i + 1));
        }

        etapasSequencia.setError(null);
        salvarEtapas.setEnabled(false);
        salvarEtapas.setText(R.string.etapas_salvando);
        etapas.salvar(sequencia, resultado -> {
            salvarEtapas.setEnabled(true);
            salvarEtapas.setText(R.string.etapas_salvar);
            Toast.makeText(this, R.string.etapas_salvas, Toast.LENGTH_SHORT).show();
        }, erro -> {
            salvarEtapas.setEnabled(true);
            salvarEtapas.setText(R.string.etapas_salvar);
            Toast.makeText(this, R.string.erro_salvar_etapas, Toast.LENGTH_LONG).show();
        });
    }

    private void abrirLeitorQr() {
        GmsBarcodeScannerOptions opcoes = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        lerQr.setEnabled(false);
        GmsBarcodeScanning.getClient(this, opcoes).startScan()
                .addOnSuccessListener(codigo -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    String loteId = codigo.getRawValue();
                    if (loteId == null || !HistoricoFirestore.codigoValido(loteId.trim())) {
                        Toast.makeText(this, R.string.erro_qr_lote, Toast.LENGTH_LONG).show();
                        lerQr.setEnabled(true);
                        return;
                    }
                    String id = loteId.trim();
                    historico.carregarProximaEtapa(id).addOnCompleteListener(tarefa -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (!tarefa.isSuccessful() || tarefa.getResult() == null) {
                            int mensagem = tarefa.getException() instanceof IllegalArgumentException
                                    ? R.string.erro_qr_lote : R.string.erro_carregar_qr_lote;
                            Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
                            lerQr.setEnabled(true);
                            return;
                        }
                        confirmarEtapa(id, tarefa.getResult());
                    });
                })
                .addOnCanceledListener(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        lerQr.setEnabled(true);
                    }
                })
                .addOnFailureListener(erro -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    Toast.makeText(this, R.string.erro_leitura_qr, Toast.LENGTH_LONG).show();
                    lerQr.setEnabled(true);
                });
    }

    private void confirmarEtapa(String loteId, Etapa etapa) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirmar_etapa)
                .setMessage(getString(R.string.confirmar_etapa_mensagem, etapa.nome))
                .setNegativeButton(R.string.cancelar, (dialogo, escolha) -> lerQr.setEnabled(true))
                .setPositiveButton(R.string.confirmar, (dialogo, escolha) ->
                        historico.registrarProximaEtapa(loteId, etapa, uid)
                                .addOnCompleteListener(this, tarefa -> {
                                    lerQr.setEnabled(true);
                                    if (tarefa.isSuccessful()) {
                                        Toast.makeText(this, R.string.etapa_salva, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(this, R.string.erro_registrar_etapa,
                                                Toast.LENGTH_LONG).show();
                                    }
                                }))
                .setOnCancelListener(dialogo -> lerQr.setEnabled(true))
                .show();
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
