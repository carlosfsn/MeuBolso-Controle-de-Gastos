package com.carlosfsn.meubolso;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PerfilActivity extends AppCompatActivity {

    // Este perfil e local. Ainda nao existe login/autenticacao real no app.
    private static final String ARQUIVO_PREFS = "meubolso_perfil_local";
    private static final String CHAVE_NOME = "nome";
    private static final String CHAVE_EMAIL = "email";
    private TextView txtNome;
    private TextView txtEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verificar se existe uma sessão ativa
        boolean autenticado = getSharedPreferences(
                "meubolso_sessao",
                MODE_PRIVATE
        ).getBoolean("autenticado", false);

        // Impedir acesso ao Perfil sem autenticação
        if (!autenticado) {

            Intent intent = new Intent(
                    PerfilActivity.this,
                    MainActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (tela, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            tela.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        txtNome = findViewById(R.id.txtNomePerfil);
        txtEmail = findViewById(R.id.txtEmailPerfil);

        // Navegacao inferior, preservando as telas existentes.
        findViewById(R.id.navInicio).setOnClickListener(v -> {
            Intent intent = new Intent(this, InicioActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.navHistorico).setOnClickListener(v ->
                startActivity(new Intent(this, HistoricoActivity.class)));
        findViewById(R.id.navNova).setOnClickListener(v ->
                startActivity(new Intent(this, NovaMovimentacaoActivity.class)));
        findViewById(R.id.navCategorias).setOnClickListener(v ->
                startActivity(new Intent(this, CategoriasActivity.class)));

        findViewById(R.id.opcaoEditarPerfil).setOnClickListener(v -> abrirEdicaoPerfil());
        findViewById(R.id.btnEditarAvatar).setOnClickListener(v -> abrirEdicaoPerfil());
        findViewById(R.id.opcaoMetaGastos).setOnClickListener(v -> abrirMetaGastos());
        findViewById(R.id.opcaoNotificacoes).setOnClickListener(v -> abrirConfiguracoesAvisos());
        findViewById(R.id.opcaoSeguranca).setOnClickListener(v -> abrirSeguranca());
        findViewById(R.id.opcaoAjuda).setOnClickListener(v -> abrirAjuda());
        findViewById(R.id.btnSinoPerfil).setOnClickListener(v -> abrirConfiguracoesAvisos());
        findViewById(R.id.cardPlanoPerfil).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Versão padrão")
                .setMessage("O MeuBolso está na versão pessoal. Não há assinaturas ou planos pagos integrados ao aplicativo.")
                .setPositiveButton("Entendi", null).show());

        // Nao finge encerrar uma sessao real: explica antes de voltar ao Login.
        findViewById(R.id.btnSairPerfil).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Sair da conta?")
                        .setMessage("Deseja sair da sua conta? "
                                + "Você voltará à tela de login e suas movimentações "
                                + "continuarão salvas neste aparelho.")
                        .setNegativeButton("Cancelar", null)

                        .setPositiveButton("Continuar", (dialogo, qual) -> {

                            // Encerrar a sessão local do usuário
                            getSharedPreferences("meubolso_sessao", MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("autenticado", false)
                                    .apply();

                            // Voltar ao Login e limpar as telas anteriores
                            Intent intent = new Intent(
                                    PerfilActivity.this,
                                    MainActivity.class
                            );

                            intent.addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                            );

                            startActivity(intent);
                        })
                        .show());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Verificar se a sessão continua ativa
        boolean autenticado = getSharedPreferences(
                "meubolso_sessao",
                MODE_PRIVATE
        ).getBoolean("autenticado", false);

        if (!autenticado) {

            Intent intent = new Intent(
                    PerfilActivity.this,
                    MainActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
            return;
        }

        // Atualizar os dados do perfil após validar a sessão
        atualizarDadosPerfil();
    }

    // Atualizar os dados do Perfil usando o SQLite
    private void atualizarDadosPerfil() {

        BancoDadosHelper banco =
                new BancoDadosHelper(this);

        try {

            // Recuperar os dados cadastrados
            String nome = banco.obterNomeUsuario();
            String email = banco.obterEmailUsuario();

            // Atualizar o nome exibido
            txtNome.setText(
                    nome == null || nome.trim().isEmpty()
                            ? "Usuário MeuBolso"
                            : nome
            );

            // Atualizar o e-mail exibido
            txtEmail.setText(
                    email == null || email.trim().isEmpty()
                            ? "E-mail não configurado"
                            : email
            );

        } catch (Exception erro) {

            android.widget.Toast.makeText(
                    this,
                    "Não foi possível carregar os dados do perfil.",
                    android.widget.Toast.LENGTH_SHORT
            ).show();

        } finally {

            banco.close();
        }
    }

    // Permitir editar o nome e o e-mail cadastrados no SQLite
    private void abrirEdicaoPerfil() {

        BancoDadosHelper banco =
                new BancoDadosHelper(this);

        String nomeAtual;
        String emailAtual;

        try {

            nomeAtual = banco.obterNomeUsuario();
            emailAtual = banco.obterEmailUsuario();

        } catch (Exception erro) {

            Toast.makeText(
                    this,
                    "Não foi possível carregar os dados da conta.",
                    Toast.LENGTH_LONG
            ).show();

            return;

        } finally {
            banco.close();
        }

        // Preparar o formulário de edição
        LinearLayout formulario = new LinearLayout(this);

        formulario.setOrientation(LinearLayout.VERTICAL);

        formulario.setPadding(
                dp(22), dp(8), dp(22), dp(4)
        );

        // Campo do nome
        TextView tituloNome = new TextView(this);
        tituloNome.setText("Nome");
        tituloNome.setTextColor(
                getColor(R.color.mb_purple_dark)
        );
        tituloNome.setTextSize(14);

        formulario.addView(tituloNome);

        EditText entradaNome = new EditText(this);

        entradaNome.setSingleLine(true);
        entradaNome.setHint("Seu nome");
        entradaNome.setText(nomeAtual);

        entradaNome.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );

        formulario.addView(
                entradaNome,
                new LinearLayout.LayoutParams(-1, dp(54))
        );

        // Campo do e-mail
        TextView tituloEmail = new TextView(this);
        tituloEmail.setText("E-mail");
        tituloEmail.setTextColor(
                getColor(R.color.mb_purple_dark)
        );
        tituloEmail.setTextSize(14);

        LinearLayout.LayoutParams espacoEmail =
                new LinearLayout.LayoutParams(-1, -2);

        espacoEmail.topMargin = dp(12);

        formulario.addView(tituloEmail, espacoEmail);

        EditText entradaEmail = new EditText(this);

        entradaEmail.setSingleLine(true);
        entradaEmail.setHint("voce@exemplo.com");
        entradaEmail.setText(emailAtual);

        entradaEmail.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        formulario.addView(
                entradaEmail,
                new LinearLayout.LayoutParams(-1, dp(54))
        );

        // Criar a janela de edição
        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Editar perfil")
                .setView(formulario)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null)
                .create();

        dialogo.setOnShowListener(ignorado -> {

            dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(botao -> {

                        String nome = entradaNome.getText()
                                .toString().trim();

                        String email = entradaEmail.getText()
                                .toString().trim();

                        // Validar o nome
                        if (nome.isEmpty()) {

                            entradaNome.setError(
                                    "Informe seu nome."
                            );

                            entradaNome.requestFocus();
                            return;
                        }

                        // Validar o e-mail
                        if (!Patterns.EMAIL_ADDRESS
                                .matcher(email).matches()) {

                            entradaEmail.setError(
                                    "Informe um e-mail válido."
                            );

                            entradaEmail.requestFocus();
                            return;
                        }

                        // Atualizar os dados reais da conta
                        BancoDadosHelper bancoEdicao =
                                new BancoDadosHelper(this);

                        try {

                            boolean atualizado =
                                    bancoEdicao.atualizarDadosUsuario(
                                            nome,
                                            email
                                    );

                            if (atualizado) {

                                atualizarDadosPerfil();

                                Toast.makeText(
                                        this,
                                        "Perfil atualizado com sucesso!",
                                        Toast.LENGTH_SHORT
                                ).show();

                                dialogo.dismiss();

                            } else {

                                Toast.makeText(
                                        this,
                                        "Não foi possível atualizar o perfil.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }

                        } catch (Exception erro) {

                            Toast.makeText(
                                    this,
                                    "Erro ao salvar os dados do perfil.",
                                    Toast.LENGTH_LONG
                            ).show();

                        } finally {

                            bancoEdicao.close();
                        }
                    });
        });

        dialogo.show();
    }

    // Permitir que o usuário configure sua meta mensal de gastos
    private void abrirMetaGastos() {

        android.content.SharedPreferences preferencias =
                getSharedPreferences(
                        "meubolso_meta_gastos",
                        MODE_PRIVATE
                );

        long metaAtual = preferencias.getLong(
                "meta_mensal_centavos",
                0L
        );

        EditText campoMeta = new EditText(this);

        campoMeta.setHint("Ex.: 1500,00");

        campoMeta.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        campoMeta.setSingleLine(true);

        // Exibir a meta anteriormente cadastrada
        if (metaAtual > 0) {

            java.text.NumberFormat formato =
                    java.text.NumberFormat.getNumberInstance(
                            new java.util.Locale("pt", "BR")
                    );

            formato.setMinimumFractionDigits(2);
            formato.setMaximumFractionDigits(2);

            campoMeta.setText(
                    formato.format(
                            java.math.BigDecimal.valueOf(metaAtual, 2)
                    )
            );
        }

        LinearLayout formulario = new LinearLayout(this);
        formulario.setOrientation(LinearLayout.VERTICAL);
        formulario.setPadding(
                dp(22), dp(8), dp(22), dp(4)
        );

        formulario.addView(campoMeta);

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Meta mensal de gastos")
                .setMessage(
                        "Defina o valor máximo que pretende gastar por mês."
                )
                .setView(formulario)
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Remover meta", null)
                .setPositiveButton("Salvar", null)
                .create();

        dialogo.setOnShowListener(ignorado -> {

            // Configurar o botão para remover a meta mensal
            android.widget.Button botaoRemover =
                    dialogo.getButton(AlertDialog.BUTTON_NEUTRAL);

            // Mostrar o botão somente quando existir uma meta
            botaoRemover.setVisibility(
                    metaAtual > 0
                            ? android.view.View.VISIBLE
                            : android.view.View.GONE
            );

            botaoRemover.setOnClickListener(botao -> {

                new AlertDialog.Builder(this)
                        .setTitle("Remover meta mensal?")
                        .setMessage(
                                "Deseja realmente remover sua meta mensal?\n\n"
                                        + "Suas movimentações financeiras "
                                        + "continuarão salvas normalmente."
                        )
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Remover", (confirmacao, qual) -> {

                            // Excluir apenas a configuração da meta
                            boolean removida = preferencias.edit()
                                    .remove("meta_mensal_centavos")
                                    .commit();

                            if (removida) {

                                Toast.makeText(
                                        this,
                                        "Meta mensal removida com sucesso!",
                                        Toast.LENGTH_SHORT
                                ).show();

                                dialogo.dismiss();

                            } else {

                                Toast.makeText(
                                        this,
                                        "Não foi possível remover a meta.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        })
                        .show();
            });

            dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(botao -> {

                        String texto = campoMeta.getText()
                                .toString()
                                .trim()
                                .replace(".", "")
                                .replace(",", ".");

                        try {

                            java.math.BigDecimal valor =
                                    new java.math.BigDecimal(texto);

                            if (valor.signum() <= 0) {
                                campoMeta.setError(
                                        "Informe um valor maior que zero."
                                );
                                return;
                            }

                            long centavos = valor
                                    .movePointRight(2)
                                    .longValueExact();

                            boolean salvo = preferencias.edit()
                                    .putLong(
                                            "meta_mensal_centavos",
                                            centavos
                                    )
                                    .commit();

                            if (salvo) {

                                Toast.makeText(
                                        this,
                                        "Meta mensal salva com sucesso!",
                                        Toast.LENGTH_SHORT
                                ).show();

                                dialogo.dismiss();

                            } else {

                                Toast.makeText(
                                        this,
                                        "Não foi possível salvar a meta.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }

                        } catch (NumberFormatException
                                 | ArithmeticException erro) {

                            campoMeta.setError(
                                    "Informe um valor válido, como 1500,00."
                            );
                        }
                    });
        });

        dialogo.show();
    }

    // Configurar os avisos da meta mensal
    private void abrirConfiguracoesAvisos() {

        android.content.SharedPreferences preferencias =
                getSharedPreferences(
                        "meubolso_meta_gastos",
                        MODE_PRIVATE
                );

        boolean avisosAtivos = preferencias.getBoolean(
                "avisos_ativos",
                true
        );

        String[] opcoes = {
                "Ativar avisos da meta mensal",
                "Desativar avisos da meta mensal"
        };

        final int[] opcaoSelecionada = {
                avisosAtivos ? 0 : 1
        };

        new AlertDialog.Builder(this)
                .setTitle("Configurações de avisos")
                .setSingleChoiceItems(
                        opcoes,
                        opcaoSelecionada[0],
                        (dialogo, posicao) -> {
                            opcaoSelecionada[0] = posicao;
                        }
                )
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", (dialogo, qual) -> {

                    boolean ativar =
                            opcaoSelecionada[0] == 0;

                    preferencias.edit()
                            .putBoolean("avisos_ativos", ativar)
                            .apply();

                    Toast.makeText(
                            this,
                            ativar
                                    ? "Avisos ativados!"
                                    : "Avisos desativados!",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .show();
    }

    // Permitir que o usuário altere sua senha
    private void abrirSeguranca() {

        LinearLayout formulario = new LinearLayout(this);
        formulario.setOrientation(LinearLayout.VERTICAL);
        formulario.setPadding(
                dp(22), dp(8), dp(22), dp(4)
        );

        // Campo da senha atual
        EditText campoSenhaAtual = new EditText(this);
        campoSenhaAtual.setHint("Senha atual");
        campoSenhaAtual.setSingleLine(true);
        campoSenhaAtual.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        formulario.addView(
                campoSenhaAtual,
                new LinearLayout.LayoutParams(-1, dp(56))
        );

        // Campo da nova senha
        EditText campoNovaSenha = new EditText(this);
        campoNovaSenha.setHint("Nova senha");
        campoNovaSenha.setSingleLine(true);
        campoNovaSenha.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        formulario.addView(
                campoNovaSenha,
                new LinearLayout.LayoutParams(-1, dp(56))
        );

        // Campo de confirmação da nova senha
        EditText campoConfirmacao = new EditText(this);
        campoConfirmacao.setHint("Confirmar nova senha");
        campoConfirmacao.setSingleLine(true);
        campoConfirmacao.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        formulario.addView(
                campoConfirmacao,
                new LinearLayout.LayoutParams(-1, dp(56))
        );

        // Criar a janela de alteração de senha
        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Alterar senha")
                .setMessage(
                        "Informe sua senha atual e escolha "
                                + "uma nova senha com pelo menos 8 caracteres."
                )
                .setView(formulario)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null)
                .create();

        dialogo.setOnShowListener(ignorado -> {

            dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(botao -> {

                        String senhaAtual = campoSenhaAtual
                                .getText().toString();

                        String novaSenha = campoNovaSenha
                                .getText().toString();

                        String confirmacao = campoConfirmacao
                                .getText().toString();

                        // Verificar o preenchimento da senha atual
                        if (senhaAtual.isEmpty()) {

                            campoSenhaAtual.setError(
                                    "Informe sua senha atual."
                            );

                            campoSenhaAtual.requestFocus();
                            return;
                        }

                        // Validar o tamanho da nova senha
                        if (novaSenha.length() < 8) {

                            campoNovaSenha.setError(
                                    "A senha deve ter pelo menos 8 caracteres."
                            );

                            campoNovaSenha.requestFocus();
                            return;
                        }

                        // Verificar se a nova senha é diferente
                        if (senhaAtual.equals(novaSenha)) {

                            campoNovaSenha.setError(
                                    "Escolha uma senha diferente da atual."
                            );

                            campoNovaSenha.requestFocus();
                            return;
                        }

                        // Confirmar se as novas senhas coincidem
                        if (!novaSenha.equals(confirmacao)) {

                            campoConfirmacao.setError(
                                    "As senhas não coincidem."
                            );

                            campoConfirmacao.requestFocus();
                            return;
                        }

                        // Atualizar a senha no SQLite
                        BancoDadosHelper banco =
                                new BancoDadosHelper(this);

                        try {

                            boolean atualizada =
                                    banco.alterarSenhaUsuario(
                                            senhaAtual,
                                            novaSenha
                                    );

                            if (atualizada) {

                                Toast.makeText(
                                        this,
                                        "Senha alterada com sucesso!",
                                        Toast.LENGTH_LONG
                                ).show();

                                dialogo.dismiss();

                            } else {

                                campoSenhaAtual.setError(
                                        "Senha atual incorreta."
                                );

                                campoSenhaAtual.requestFocus();
                            }

                        } catch (Exception erro) {

                            Toast.makeText(
                                    this,
                                    "Não foi possível alterar a senha.",
                                    Toast.LENGTH_LONG
                            ).show();

                        } finally {

                            banco.close();
                        }
                    });
        });

        dialogo.show();
    }

    // Exibir orientações sobre as funcionalidades do MeuBolso
    private void abrirAjuda() {

        String[] perguntas = {
                "Como cadastrar uma movimentação?",
                "Como editar ou excluir uma movimentação?",
                "Como funciona a meta mensal?",
                "Como alterar meus dados ou minha senha?",
                "Onde meus dados são armazenados?"
        };

        String[] respostas = {

                "Acesse a opção Nova na barra inferior. "
                        + "Escolha Receita ou Despesa, informe "
                        + "o valor, a categoria, a data e os demais "
                        + "dados. Depois, toque em Salvar.",

                "Acesse Histórico e selecione o mês desejado. "
                        + "Toque na movimentação para visualizar "
                        + "as opções de editar ou excluir. "
                        + "A exclusão exige confirmação.",

                "Acesse Perfil e selecione Meta de Gastos. "
                        + "Você poderá cadastrar, alterar ou remover "
                        + "um limite mensal. No Dashboard, o aplicativo "
                        + "mostra quanto da meta já foi utilizado "
                        + "e quanto ainda está disponível.",

                "Em Perfil, toque em Editar perfil para alterar "
                        + "seu nome ou e-mail. Para mudar a senha, "
                        + "acesse Segurança e informe sua senha atual "
                        + "e a nova senha.",

                "As movimentações e os dados da conta são "
                        + "armazenados localmente no banco SQLite "
                        + "do aparelho. A meta mensal e as preferências "
                        + "de avisos também são salvas no dispositivo. "
                        + "O aplicativo ainda não possui sincronização "
                        + "ou recuperação dos dados pela internet."
        };

        new AlertDialog.Builder(this)
                .setTitle("Central de Ajuda")
                .setItems(perguntas, (dialogo, posicao) -> {

                    new AlertDialog.Builder(this)
                            .setTitle(perguntas[posicao])
                            .setMessage(respostas[posicao])
                            .setNegativeButton("Fechar", null)
                            .setPositiveButton(
                                    "Voltar",
                                    (janela, qual) -> abrirAjuda()
                            )
                            .show();
                })
                .setNegativeButton("Fechar", null)
                .show();
    }

    private void avisarProximaEtapa(String funcionalidade) {
        Toast.makeText(this,
                funcionalidade + " estará disponível em uma próxima etapa.",
                Toast.LENGTH_LONG).show();
    }

    private int dp(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}