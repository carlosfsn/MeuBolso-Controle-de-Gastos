package com.carlosfsn.meubolso;

import android.content.Intent;
import android.widget.Button;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verificar se o usuário já possui uma sessão ativa
        boolean autenticado = getSharedPreferences(
                "meubolso_sessao",
                MODE_PRIVATE
        ).getBoolean("autenticado", false);

        if (autenticado) {

            // Abrir o Dashboard sem solicitar login novamente
            Intent intent = new Intent(
                    MainActivity.this,
                    InicioActivity.class
            );

            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        findViewById(R.id.imgGoogle).setOnClickListener(
                v -> selecionarContaGoogle()
        );

        // Abrir o Google automaticamente quando vier da tela Criar Conta
        if (getIntent().getBooleanExtra(
                "abrir_google_automaticamente",
                false
        )) {

            findViewById(R.id.imgGoogle).post(() -> {
                selecionarContaGoogle();
            });
        }

        findViewById(R.id.txtEsqueciSenha).setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(
                    MainActivity.this
            )
                    .setTitle("Esqueci minha senha")
                    .setMessage(
                            "O MeuBolso utiliza uma conta armazenada "
                                    + "localmente neste dispositivo.\n\n"
                                    + "No momento, não existe recuperação "
                                    + "de senha por e-mail.\n\n"
                                    + "Se você ainda souber sua senha atual, "
                                    + "entre no aplicativo e acesse "
                                    + "Perfil → Segurança para alterá-la.\n\n"
                                    + "Atenção: não desinstale o aplicativo "
                                    + "nem limpe seus dados, pois isso pode "
                                    + "apagar suas movimentações financeiras."
                    )
                    .setPositiveButton("Entendi", null)
                    .show();
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Botao para abrir a tela Criar Conta
        Button btnCriarConta = findViewById(R.id.btnCriarConta);

        btnCriarConta.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    CriarContaActivity.class
            );

            startActivity(intent);

        });

        // Botao Entrar - Abrir a tela Inicio
        Button btnEntrar = findViewById(R.id.btnEntrar);


        // Validar o login antes de abrir o Dashboard
        btnEntrar.setOnClickListener(v -> {

            android.widget.EditText campoEmail =
                    findViewById(R.id.edtEmail);

            android.widget.EditText campoSenha =
                    findViewById(R.id.edtSenha);

            String email = campoEmail.getText().toString().trim();
            String senha = campoSenha.getText().toString();

            // Verificar se o e-mail foi informado
            if (email.isEmpty()) {
                campoEmail.setError("Informe seu e-mail.");
                campoEmail.requestFocus();
                return;
            }

            // Verificar se a senha foi informada
            if (senha.isEmpty()) {
                campoSenha.setError("Informe sua senha.");
                campoSenha.requestFocus();
                return;
            }

            BancoDadosHelper banco =
                    new BancoDadosHelper(MainActivity.this);

            try {

                boolean loginValido = banco.validarLogin(
                        email,
                        senha
                );

                if (loginValido) {

                    // Registrar que o usuário está conectado
                    getSharedPreferences(
                            "meubolso_sessao",
                            MODE_PRIVATE
                    ).edit()
                            .putBoolean("autenticado", true)
                            .apply();

                    android.widget.Toast.makeText(
                            MainActivity.this,
                            "Login realizado com sucesso!",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();

                    Intent intent = new Intent(
                            MainActivity.this,
                            InicioActivity.class
                    );

                    startActivity(intent);
                    finish();

                } else {

                    campoSenha.setText("");

                    android.widget.Toast.makeText(
                            MainActivity.this,
                            "E-mail ou senha incorretos.",
                            android.widget.Toast.LENGTH_LONG
                    ).show();
                }

            } catch (Exception erro) {

                android.widget.Toast.makeText(
                        MainActivity.this,
                        "Não foi possível realizar o login.",
                        android.widget.Toast.LENGTH_LONG
                ).show();

            } finally {
                banco.close();
            }
        });

    }

    // Abrir a seleção de contas Google no Android
    private void selecionarContaGoogle() {

        try {

            com.google.android.libraries.identity.googleid.GetGoogleIdOption opcaoGoogle =
                    new com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                            .setServerClientId(
                                    getString(R.string.default_web_client_id)
                            )
                            .setFilterByAuthorizedAccounts(false)
                            .setAutoSelectEnabled(false)
                            .build();

            androidx.credentials.GetCredentialRequest solicitacao =
                    new androidx.credentials.GetCredentialRequest.Builder()
                            .addCredentialOption(opcaoGoogle)
                            .build();

            androidx.credentials.CredentialManager gerenciador =
                    androidx.credentials.CredentialManager.create(this);

            // Abrir a interface de seleção de contas
            gerenciador.getCredentialAsync(
                    this,
                    solicitacao,
                    new android.os.CancellationSignal(),
                    androidx.core.content.ContextCompat.getMainExecutor(this),

                    new androidx.credentials.CredentialManagerCallback<
                            androidx.credentials.GetCredentialResponse,
                            androidx.credentials.exceptions.GetCredentialException>() {

                        @Override
                        public void onResult(
                                androidx.credentials.GetCredentialResponse resposta) {

                            androidx.credentials.Credential credencial =
                                    resposta.getCredential();

                            if (!(credencial instanceof
                                    androidx.credentials.CustomCredential)) {

                                mostrarErroGoogle();
                                return;
                            }

                            androidx.credentials.CustomCredential personalizada =
                                    (androidx.credentials.CustomCredential) credencial;

                            if (!com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
                                    .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                    .equals(personalizada.getType())) {

                                mostrarErroGoogle();
                                return;
                            }

                            try {

                                // Recuperar a credencial selecionada
                                com.google.android.libraries.identity.googleid.GoogleIdTokenCredential contaGoogle =
                                        com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
                                                .createFrom(personalizada.getData());

                                // Transformar o token Google em uma credencial Firebase
                                com.google.firebase.auth.AuthCredential credencialFirebase =
                                        com.google.firebase.auth.GoogleAuthProvider.getCredential(
                                                contaGoogle.getIdToken(),
                                                null
                                        );

                                com.google.firebase.auth.FirebaseAuth autenticacao =
                                        com.google.firebase.auth.FirebaseAuth.getInstance();

                                // Validar a conta Google com o Firebase
                                autenticacao.signInWithCredential(credencialFirebase)
                                        .addOnCompleteListener(
                                                MainActivity.this,
                                                tarefa -> {


                                                    if (tarefa.isSuccessful()) {

                                                        com.google.firebase.auth.FirebaseUser usuario =
                                                                autenticacao.getCurrentUser();

                                                        if (usuario == null) {

                                                            autenticacao.signOut();
                                                            mostrarErroGoogle();
                                                            return;
                                                        }

                                                        // Verificar se a conta Google já está vinculada ao cadastro local
                                                        processarLoginGoogle(
                                                                usuario.getUid(),
                                                                usuario.getEmail(),
                                                                autenticacao
                                                        );

                                                    } else {

                                                        String detalhes =
                                                                tarefa.getException() != null
                                                                        ? tarefa.getException().getMessage()
                                                                        : "Erro desconhecido.";

                                                        new androidx.appcompat.app.AlertDialog.Builder(
                                                                MainActivity.this
                                                        )
                                                                .setTitle("Erro no Firebase")
                                                                .setMessage(detalhes)
                                                                .setPositiveButton("Entendi", null)
                                                                .show();
                                                    }
                                                }
                                        );

                            } catch (Exception erro) {

                                mostrarErroGoogle();
                            }
                        }

                        @Override
                        public void onError(
                                androidx.credentials.exceptions.GetCredentialException erro) {

                            // Ignorar quando o usuário cancelar a seleção
                            if (erro instanceof
                                    androidx.credentials.exceptions.GetCredentialCancellationException) {
                                return;
                            }

                            // Mostrar o motivo real da falha
                            new androidx.appcompat.app.AlertDialog.Builder(
                                    MainActivity.this
                            )
                                    .setTitle("Erro na autenticação Google")
                                    .setMessage(
                                            "Tipo: " + erro.getClass().getSimpleName()
                                                    + "\n\nDetalhes: " + erro.getMessage()
                                    )
                                    .setPositiveButton("Entendi", null)
                                    .show();
                        }
                    }
            );

        } catch (Exception erro) {

            mostrarErroGoogle();
        }
    }

    // Informar quando não for possível selecionar uma conta
    private void mostrarErroGoogle() {

        android.widget.Toast.makeText(
                this,
                "Não foi possível selecionar a conta Google.",
                android.widget.Toast.LENGTH_LONG
        ).show();
    }


    // Identificar se a conta Google já está cadastrada
    private void processarLoginGoogle(
            String googleUid,
            String emailGoogle,
            com.google.firebase.auth.FirebaseAuth autenticacao) {

        BancoDadosHelper banco = new BancoDadosHelper(this);

        try {

            com.google.firebase.auth.FirebaseUser usuarioGoogle =
                    autenticacao.getCurrentUser();

            if (usuarioGoogle == null
                    || googleUid == null
                    || !googleUid.equals(usuarioGoogle.getUid())
                    || usuarioGoogle.getEmail() == null) {

                autenticacao.signOut();
                mostrarErroGoogle();
                return;
            }

            // Procurar o usuário pelo identificador do Firebase
            long usuarioId = banco.obterIdUsuarioGoogle(googleUid);

            if (usuarioId > 0) {

                // Conta existente: salvar o usuário correto na sessão
                boolean sessaoSalva = getSharedPreferences(
                        "meubolso_sessao",
                        MODE_PRIVATE
                ).edit()
                        .putLong("usuario_id", usuarioId)
                        .putBoolean("autenticado", true)
                        .commit();

                autenticacao.signOut();

                if (!sessaoSalva) {
                    mostrarErroGoogle();
                    return;
                }

                android.content.Intent intent =
                        new android.content.Intent(
                                MainActivity.this,
                                InicioActivity.class
                        );

                intent.addFlags(
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                );

                startActivity(intent);
                finish();

            } else {

                // Conta nova: deixar o Firebase autenticado para o cadastro
                boolean sessaoLimpa = getSharedPreferences(
                        "meubolso_sessao",
                        MODE_PRIVATE
                ).edit()
                        .putBoolean("autenticado", false)
                        .remove("usuario_id")
                        .commit();

                if (!sessaoLimpa) {
                    autenticacao.signOut();
                    mostrarErroGoogle();
                    return;
                }

                // Reutilizar a tela de cadastro existente
                android.content.Intent intent =
                        new android.content.Intent(
                                MainActivity.this,
                                CriarContaActivity.class
                        );

                intent.putExtra("cadastro_google", true);

                startActivity(intent);
            }

        } catch (Exception erro) {

            autenticacao.signOut();

            android.widget.Toast.makeText(
                    this,
                    "Não foi possível continuar com o Google.",
                    android.widget.Toast.LENGTH_LONG
            ).show();

        } finally {
            banco.close();
        }
    }

    // Solicitar a senha local antes de vincular o Google
    private void solicitarVinculoGoogle(
            String googleUid,
            String emailGoogle,
            com.google.firebase.auth.FirebaseAuth autenticacao) {

        android.widget.EditText campoSenha =
                new android.widget.EditText(this);

        campoSenha.setHint("Senha atual do MeuBolso");

        campoSenha.setSingleLine(true);

        campoSenha.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        android.widget.LinearLayout formulario =
                new android.widget.LinearLayout(this);

        formulario.setOrientation(
                android.widget.LinearLayout.VERTICAL
        );

        int margem = Math.round(
                20 * getResources().getDisplayMetrics().density
        );

        formulario.setPadding(margem, 0, margem, 0);
        formulario.addView(campoSenha);

        String emailExibido = emailGoogle == null
                ? "Conta Google selecionada"
                : emailGoogle;

        androidx.appcompat.app.AlertDialog dialogo =
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Vincular conta Google")
                        .setMessage(
                                "Conta Google: " + emailExibido
                                        + "\n\nPara proteger suas movimentações, "
                                        + "confirme a senha da conta "
                                        + "já cadastrada no MeuBolso."
                        )
                        .setView(formulario)
                        .setNegativeButton(
                                "Cancelar",
                                (janela, qual) -> autenticacao.signOut()
                        )
                        .setPositiveButton("Vincular", null)
                        .create();

        // Encerrar a sessão Firebase se a janela for cancelada
        dialogo.setOnCancelListener(
                janela -> autenticacao.signOut()
        );

        dialogo.setOnShowListener(ignorado -> {

            dialogo.getButton(
                    androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(botao -> {

                String senhaLocal =
                        campoSenha.getText().toString();

                if (senhaLocal.isEmpty()) {

                    campoSenha.setError(
                            "Informe a senha da conta MeuBolso."
                    );

                    campoSenha.requestFocus();
                    return;
                }

                BancoDadosHelper banco =
                        new BancoDadosHelper(this);

                try {

                    boolean vinculado =
                            banco.vincularContaGoogle(
                                    googleUid,
                                    senhaLocal
                            );

                    if (vinculado) {

                        dialogo.dismiss();

                        android.widget.Toast.makeText(
                                this,
                                "Conta Google vinculada com sucesso!",
                                android.widget.Toast.LENGTH_LONG
                        ).show();

                        finalizarLoginGoogle(autenticacao);

                    } else {

                        campoSenha.setError(
                                "Senha incorreta ou já existe "
                                        + "outra conta Google vinculada."
                        );

                        campoSenha.requestFocus();
                    }

                } catch (Exception erro) {

                    android.widget.Toast.makeText(
                            this,
                            "Não foi possível vincular a conta.",
                            android.widget.Toast.LENGTH_LONG
                    ).show();

                } finally {
                    banco.close();
                }
            });
        });

        dialogo.show();
    }

    // Abrir o Dashboard após validar a conta Google
    private void finalizarLoginGoogle(
            com.google.firebase.auth.FirebaseAuth autenticacao) {

        // Encerrar a sessão Firebase de autenticação
        autenticacao.signOut();

        // Manter a sessão local utilizada pelo MeuBolso
        getSharedPreferences(
                "meubolso_sessao",
                MODE_PRIVATE
        ).edit()
                .putBoolean("autenticado", true)
                .apply();

        android.content.Intent intent =
                new android.content.Intent(
                        MainActivity.this,
                        InicioActivity.class
                );

        intent.addFlags(
                android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
    }
}