
package com.carlosfsn.meubolso;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CriarContaActivity extends AppCompatActivity {

    private EditText campoNome;
    private EditText campoEmail;
    private EditText campoSenha;
    private EditText campoConfirmar;

    private boolean cadastroGoogle = false;
    private String googleUid = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_criar_conta);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets barras = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            barras.left,
                            barras.top,
                            barras.right,
                            barras.bottom
                    );

                    return insets;
                }
        );

        campoNome = findViewById(R.id.edtNomeCadastro);
        campoEmail = findViewById(R.id.edtEmailCadastro);
        campoSenha = findViewById(R.id.edtSenhaCadastro);
        campoConfirmar = findViewById(R.id.edtConfirmarSenha);

        cadastroGoogle = getIntent().getBooleanExtra(
                "cadastro_google",
                false
        );

        if (cadastroGoogle) {
            prepararCadastroGoogle();
        }

        TextView txtEntrarCadastro =
                findViewById(R.id.txtEntrarCadastro);

        txtEntrarCadastro.setOnClickListener(v -> {
            encerrarCadastro();
        });

        findViewById(R.id.btnCadastrar).setOnClickListener(v -> {
            cadastrarConta();
        });

        // Abrir a seleção de contas Google a partir do cadastro
        findViewById(R.id.imgGoogle).setOnClickListener(v -> {

            // Encerrar uma seleção Google anterior, se existir
            com.google.firebase.auth.FirebaseAuth
                    .getInstance()
                    .signOut();

            android.content.Intent intent =
                    new android.content.Intent(
                            CriarContaActivity.this,
                            MainActivity.class
                    );

            // Solicitar a abertura automática do Google
            intent.putExtra(
                    "abrir_google_automaticamente",
                    true
            );

            startActivity(intent);
            finish();
        });
    }

    // Preencher os dados confirmados pelo Firebase
    private void prepararCadastroGoogle() {

        FirebaseUser usuarioGoogle =
                FirebaseAuth.getInstance().getCurrentUser();

        if (usuarioGoogle == null
                || usuarioGoogle.getUid() == null
                || usuarioGoogle.getEmail() == null
                || usuarioGoogle.getEmail().trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Não foi possível confirmar a conta Google.",
                    Toast.LENGTH_LONG
            ).show();

            encerrarCadastro();
            return;
        }

        googleUid = usuarioGoogle.getUid();

        String nome = usuarioGoogle.getDisplayName();
        String email = usuarioGoogle.getEmail().trim();

        if (nome == null || nome.trim().isEmpty()) {
            nome = email.substring(0, email.indexOf("@"));
        }

        campoNome.setText(nome);
        campoEmail.setText(email);

        // Nome e e-mail vieram da conta autenticada
        campoNome.setEnabled(false);
        campoEmail.setEnabled(false);

        TextView titulo = findViewById(R.id.txtTituloCadastro);
        titulo.setText("Concluir cadastro");

        android.widget.Button botao =
                findViewById(R.id.btnCadastrar);

        botao.setText("Criar minha conta");

        campoSenha.requestFocus();
    }

    // Validar os campos e salvar o novo cadastro
    private void cadastrarConta() {

        String nome = campoNome.getText().toString().trim();
        String email = campoEmail.getText().toString().trim();
        String senha = campoSenha.getText().toString();
        String confirmacao = campoConfirmar.getText().toString();

        if (nome.isEmpty()) {

            campoNome.setError("Informe seu nome.");
            campoNome.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

            campoEmail.setError("Informe um e-mail válido.");
            campoEmail.requestFocus();
            return;
        }

        if (senha.length() < 8) {

            campoSenha.setError(
                    "A senha deve ter pelo menos 8 caracteres."
            );

            campoSenha.requestFocus();
            return;
        }

        if (!senha.equals(confirmacao)) {

            campoConfirmar.setError(
                    "As senhas não coincidem."
            );

            campoConfirmar.requestFocus();
            return;
        }

        BancoDadosHelper banco =
                new BancoDadosHelper(this);

        try {

            if (cadastroGoogle) {

                FirebaseUser usuarioGoogle =
                        FirebaseAuth.getInstance().getCurrentUser();

                // Conferir novamente a identidade autenticada
                if (usuarioGoogle == null
                        || googleUid == null
                        || !googleUid.equals(usuarioGoogle.getUid())
                        || usuarioGoogle.getEmail() == null
                        || !email.equalsIgnoreCase(
                        usuarioGoogle.getEmail())) {

                    Toast.makeText(
                            this,
                            "Sessão Google inválida. Faça login novamente.",
                            Toast.LENGTH_LONG
                    ).show();

                    encerrarCadastro();
                    return;
                }

                long usuarioId = banco.cadastrarUsuarioGoogle(
                        nome,
                        email,
                        senha,
                        googleUid
                );

                if (usuarioId > 0) {

                    // Identificar o novo usuário na sessão local
                    banco.definirUsuarioSessao(usuarioId);

                    boolean sessaoSalva = getSharedPreferences(
                            "meubolso_sessao",
                            MODE_PRIVATE
                    ).edit()
                            .putLong("usuario_id", usuarioId)
                            .putBoolean("autenticado", true)
                            .commit();

                    FirebaseAuth.getInstance().signOut();

                    if (sessaoSalva) {

                        Toast.makeText(
                                this,
                                "Conta criada com sucesso!",
                                Toast.LENGTH_LONG
                        ).show();

                        abrirDashboard();

                    } else {

                        Toast.makeText(
                                this,
                                "Conta criada. Entre novamente para continuar.",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                    }

                } else {

                    Toast.makeText(
                            this,
                            "Este e-mail ou conta Google já possui cadastro.",
                            Toast.LENGTH_LONG
                    ).show();
                }

            } else {

                boolean cadastrado = banco.cadastrarUsuario(
                        nome,
                        email,
                        senha
                );

                if (cadastrado) {

                    Toast.makeText(
                            this,
                            "Conta criada com sucesso!",
                            Toast.LENGTH_LONG
                    ).show();

                    // Cadastro tradicional continua voltando ao Login
                    finish();

                } else {

                    Toast.makeText(
                            this,
                            "Este e-mail já possui cadastro.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

        } catch (Exception erro) {

            Toast.makeText(
                    this,
                    "Não foi possível criar a conta.",
                    Toast.LENGTH_LONG
            ).show();

        } finally {
            banco.close();
        }
    }

    // Abrir somente os dados do usuário que acabou de entrar
    private void abrirDashboard() {

        Intent intent = new Intent(
                this,
                InicioActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    // Voltar ao Login sem manter a autenticação Google pendente
    private void encerrarCadastro() {

        if (cadastroGoogle) {
            FirebaseAuth.getInstance().signOut();
        }

        finish();
    }
}