package com.carlosfsn.meubolso;

import android.widget.TextView;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CriarContaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_criar_conta);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Localiza o texto Entrar
        TextView txtEntrarCadastro =
                findViewById(R.id.txtEntrarCadastro);

        // Retorna para a tela de Login
        txtEntrarCadastro.setOnClickListener(v -> {
            finish();
        });

        // Cadastrar um usuário no banco de dados local
        findViewById(R.id.btnCadastrar).setOnClickListener(botao -> {

            android.widget.EditText campoNome =
                    findViewById(R.id.edtNomeCadastro);

            android.widget.EditText campoEmail =
                    findViewById(R.id.edtEmailCadastro);

            android.widget.EditText campoSenha =
                    findViewById(R.id.edtSenhaCadastro);

            android.widget.EditText campoConfirmar =
                    findViewById(R.id.edtConfirmarSenha);

            String nome = campoNome.getText().toString().trim();
            String email = campoEmail.getText().toString().trim();
            String senha = campoSenha.getText().toString();
            String confirmacao = campoConfirmar.getText().toString();

            // Validar o nome
            if (nome.isEmpty()) {
                campoNome.setError("Informe seu nome.");
                campoNome.requestFocus();
                return;
            }

            // Validar o e-mail
            if (!android.util.Patterns.EMAIL_ADDRESS
                    .matcher(email).matches()) {

                campoEmail.setError("Informe um e-mail válido.");
                campoEmail.requestFocus();
                return;
            }

            // Validar a senha
            if (senha.length() < 8) {
                campoSenha.setError(
                        "A senha deve ter pelo menos 8 caracteres."
                );
                campoSenha.requestFocus();
                return;
            }

            // Confirmar se as senhas são iguais
            if (!senha.equals(confirmacao)) {
                campoConfirmar.setError(
                        "As senhas não coincidem."
                );
                campoConfirmar.requestFocus();
                return;
            }

            // Salvar o usuário no SQLite
            BancoDadosHelper banco =
                    new BancoDadosHelper(CriarContaActivity.this);

            try {

                boolean cadastrado = banco.cadastrarUsuario(
                        nome,
                        email,
                        senha
                );

                if (cadastrado) {

                    android.widget.Toast.makeText(
                            CriarContaActivity.this,
                            "Conta criada com sucesso!",
                            android.widget.Toast.LENGTH_LONG
                    ).show();

                    // Retornar para a tela de Login
                    finish();

                } else {

                    android.widget.Toast.makeText(
                            CriarContaActivity.this,
                            "Já existe uma conta cadastrada neste aparelho.",
                            android.widget.Toast.LENGTH_LONG
                    ).show();
                }

            } catch (Exception erro) {

                android.widget.Toast.makeText(
                        CriarContaActivity.this,
                        "Não foi possível criar a conta.",
                        android.widget.Toast.LENGTH_LONG
                ).show();

            } finally {
                banco.close();
            }
        });

    }
}