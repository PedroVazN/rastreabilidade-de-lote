package br.edu.unisenai.rastreabilidade;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import java.util.ArrayList;
import java.util.List;

import br.edu.unisenai.rastreabilidade.data.HistoricoFirestore;
import br.edu.unisenai.rastreabilidade.model.Evento;

public class HistoricoActivity extends Activity {
    public static final String EXTRA_LOTE = "loteId";
    private static final String ESTADO_MOTIVO = "motivo";
    private static final String ESTADO_MOTIVO_LOTE = "motivoLote";

    private final HistoricoFirestore historico = new HistoricoFirestore();
    private EditText codigo;
    private TextView situacao;
    private TextView vazio;
    private Button reprovar;
    private Button lerQr;
    private AlertDialog dialogoMotivo;
    private boolean qualidade;
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
        reprovar = findViewById(R.id.reprovar);
        reprovar.setOnClickListener(view -> pedirMotivo(""));
        qualidade = LoginActivity.QUALIDADE.equals(getIntent().getStringExtra(LoginActivity.EXTRA_PERFIL));
        adapter = new HistoricoAdapter();
        RecyclerView lista = findViewById(R.id.eventos);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adapter);
        findViewById(R.id.consultar).setOnClickListener(view -> consultar());
        lerQr = findViewById(R.id.ler_qr);
        lerQr.setOnClickListener(view -> lerQr());
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
        if (estado != null && loteAtual != null && estado.getString(ESTADO_MOTIVO) != null
                && loteAtual.equals(estado.getString(ESTADO_MOTIVO_LOTE))) {
            pedirMotivo(estado.getString(ESTADO_MOTIVO));
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
        if (dialogoMotivo != null && dialogoMotivo.isShowing()) {
            EditText motivo = dialogoMotivo.findViewById(R.id.motivo);
            estado.putString(ESTADO_MOTIVO, motivo.getText().toString());
            estado.putString(ESTADO_MOTIVO_LOTE, loteAtual);
        }
    }

    @Override
    protected void onDestroy() {
        if (dialogoMotivo != null) {
            dialogoMotivo.dismiss();
        }
        super.onDestroy();
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

    private void lerQr() {
        GmsBarcodeScannerOptions opcoes = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        lerQr.setEnabled(false);
        GmsBarcodeScanning.getClient(this, opcoes).startScan()
                .addOnSuccessListener(lido -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    lerQr.setEnabled(true);
                    String id = lido.getRawValue() == null ? "" : lido.getRawValue().trim();
                    if (!HistoricoFirestore.codigoValido(id)) {
                        Toast.makeText(this, R.string.erro_qr_consulta, Toast.LENGTH_LONG).show();
                        return;
                    }
                    codigo.setText(id);
                    consultar();
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
        reprovar.setVisibility(View.GONE);
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

    private void pedirMotivo(String inicial) {
        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle(R.string.reprovar_lote)
                .setMessage(R.string.reprovar_mensagem)
                .setView(R.layout.campo_motivo)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.reprovar, null)
                .create();
        dialogo.setOnShowListener(aberto -> {
            EditText motivo = dialogo.findViewById(R.id.motivo);
            TextView erro = dialogo.findViewById(R.id.erro_motivo);
            if (motivo.length() == 0) {
                motivo.setText(inicial);
                motivo.setSelection(motivo.length());
            }
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String texto = motivo.getText().toString().trim();
                if (texto.isEmpty()) {
                    erro.setText(R.string.erro_motivo);
                    erro.setVisibility(View.VISIBLE);
                    motivo.requestFocus();
                    return;
                }
                FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
                if (loteAtual == null || usuario == null) {
                    erro.setText(R.string.erro_reprovar);
                    erro.setVisibility(View.VISIBLE);
                    return;
                }
                // O listener do lote mostra a reprovação; se as regras recusarem, o status volta a ativo.
                historico.reprovar(loteAtual, texto, usuario.getUid())
                        .addOnFailureListener(this, falha ->
                                Toast.makeText(this, R.string.erro_reprovar, Toast.LENGTH_LONG).show());
                reprovar.setVisibility(View.GONE);
                dialogo.dismiss();
            });
        });
        dialogo.setOnDismissListener(fechado -> dialogoMotivo = null);
        dialogoMotivo = dialogo;
        dialogo.show();
    }

    private void mostrar() {
        boolean carregado = reprovado != null && eventos != null;
        if (carregado) {
            situacao.setText(reprovado ? R.string.situacao_reprovado : R.string.situacao_ativo);
        }
        situacao.setVisibility(carregado ? View.VISIBLE : View.GONE);
        reprovar.setVisibility(carregado && !loteLocal && qualidade && !reprovado
                ? View.VISIBLE : View.GONE);
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
