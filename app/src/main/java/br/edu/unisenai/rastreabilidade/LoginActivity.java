package br.edu.unisenai.rastreabilidade;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

public class LoginActivity extends Activity {
    static final String EXTRA_PERFIL = "perfil";
    static final String EXTRA_UID = "uid";
    static final String OPERADOR = "operador";
    static final String QUALIDADE = "qualidade";

    private FirebaseAuth auth;
    private EditText email;
    private EditText senha;
    private TextView mensagem;
    private View entrar;
    private View cadastrar;
    private boolean ocupado;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        setContentView(R.layout.activity_login);
        findViewById(R.id.tela).setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets barras = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });
        auth = FirebaseAuth.getInstance();
        email = findViewById(R.id.email);
        senha = findViewById(R.id.senha);
        mensagem = findViewById(R.id.mensagem_login);
        entrar = findViewById(R.id.entrar);
        cadastrar = findViewById(R.id.cadastrar);
        entrar.setOnClickListener(view -> autenticar(false));
        cadastrar.setOnClickListener(view -> autenticar(true));
        FirebaseUser atual = auth.getCurrentUser();
        if (atual != null) {
            carregarPerfil(atual.getUid());
        }
    }

    private void autenticar(boolean novaConta) {
        if (ocupado) {
            return;
        }
        FirebaseUser atual = auth.getCurrentUser();
        if (!novaConta && atual != null) {
            carregarPerfil(atual.getUid());
            return;
        }
        String endereco = email.getText().toString().trim();
        String segredo = senha.getText().toString();
        if (!Patterns.EMAIL_ADDRESS.matcher(endereco).matches()) {
            email.setError(getString(R.string.erro_email));
            email.requestFocus();
            return;
        }
        if (segredo.isEmpty() || (novaConta && segredo.length() < 6)) {
            senha.setError(getString(novaConta ? R.string.erro_senha_curta : R.string.erro_senha));
            senha.requestFocus();
            return;
        }
        definirEstado(true, R.string.conectando);
        var operacao = novaConta ? auth.createUserWithEmailAndPassword(endereco, segredo)
                : auth.signInWithEmailAndPassword(endereco, segredo);
        operacao.addOnCompleteListener(tarefa -> {
            if (!tarefa.isSuccessful()) {
                if (!isDestroyed()) {
                    definirEstado(false, novaConta ? R.string.erro_cadastro : R.string.erro_login);
                }
                return;
            }
            if (novaConta) {
                auth.signOut();
                if (!isDestroyed()) {
                    senha.setText("");
                    definirEstado(false, R.string.cadastro_pendente);
                }
                return;
            }
            FirebaseUser usuario = auth.getCurrentUser();
            if (!isDestroyed() && usuario != null) {
                carregarPerfil(usuario.getUid());
            }
        });
    }

    private void carregarPerfil(String uid) {
        definirEstado(true, R.string.conectando);
        FirebaseFirestore.getInstance().collection("usuarios").document(uid).get(Source.SERVER)
                .addOnCompleteListener(tarefa -> {
                    if (isDestroyed() || isFinishing()) {
                        return;
                    }
                    if (!tarefa.isSuccessful()) {
                        auth.signOut();
                        definirEstado(false, R.string.erro_perfil);
                        return;
                    }
                    Object perfil = tarefa.getResult().get("perfil");
                    FirebaseUser atual = auth.getCurrentUser();
                    if (atual == null || !uid.equals(atual.getUid())
                            || (!OPERADOR.equals(perfil) && !QUALIDADE.equals(perfil))) {
                        auth.signOut();
                        definirEstado(false, R.string.sem_perfil);
                        return;
                    }
                    startActivity(new Intent(this, MainActivity.class)
                            .putExtra(EXTRA_UID, uid).putExtra(EXTRA_PERFIL, (String) perfil)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                });
    }

    private void definirEstado(boolean carregando, int texto) {
        ocupado = carregando;
        entrar.setEnabled(!carregando);
        cadastrar.setEnabled(!carregando);
        mensagem.setText(texto);
        mensagem.setVisibility(View.VISIBLE);
    }
}
