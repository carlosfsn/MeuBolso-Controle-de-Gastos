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
}