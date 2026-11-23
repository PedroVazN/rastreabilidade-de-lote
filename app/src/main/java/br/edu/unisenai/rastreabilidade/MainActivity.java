package br.edu.unisenai.rastreabilidade;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import br.edu.unisenai.rastreabilidade.data.LotesLocais;
import br.edu.unisenai.rastreabilidade.model.Lote;

public class MainActivity extends Activity {
    private EditText produto;
    private EditText quantidade;
    private LotesLocais lotes;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        setContentView(R.layout.activity_main);
        findViewById(R.id.tela).setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets barras = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        produto = findViewById(R.id.produto);
        quantidade = findViewById(R.id.quantidade);
        lotes = new LotesLocais(this);
        findViewById(R.id.criar).setOnClickListener(view -> criarLote());

        Lote ultimo = lotes.ultimo();
        if (ultimo != null) {
            mostrarEtiqueta(ultimo, gerarQr(ultimo.id));
        }
    }

    private void criarLote() {
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

        Lote lote = new Lote(UUID.randomUUID().toString(), nome, total, System.currentTimeMillis());
        Bitmap qr = gerarQr(lote.id);
        if (!lotes.salvar(lote)) {
            Toast.makeText(this, R.string.erro_gravar, Toast.LENGTH_LONG).show();
            return;
        }
        produto.setText("");
        quantidade.setText("");
        getSystemService(android.view.inputmethod.InputMethodManager.class)
                .hideSoftInputFromWindow(produto.getWindowToken(), 0);
        mostrarEtiqueta(lote, qr);
        View tela = findViewById(R.id.tela);
        tela.post(() -> ((android.widget.ScrollView) tela).fullScroll(View.FOCUS_DOWN));
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
        findViewById(R.id.etiqueta).setVisibility(View.VISIBLE);
    }
}
