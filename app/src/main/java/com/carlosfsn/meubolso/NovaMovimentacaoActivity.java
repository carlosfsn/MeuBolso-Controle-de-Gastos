package com.carlosfsn.meubolso;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class NovaMovimentacaoActivity extends AppCompatActivity {

    private boolean receitaSelecionada = false;
    private String categoriaSelecionada = null;

    private static class CategoriaItem {
        final String nome;
        final int icone;
        final boolean moradia;

        CategoriaItem(String nome, int icone) {
            this(nome, icone, false);
        }

        CategoriaItem(String nome, int icone, boolean moradia) {
            this.nome = nome;
            this.icone = icone;
            this.moradia = moradia;
        }
    }

    private final CategoriaItem[] categoriasDespesa = {
            new CategoriaItem("Alimentação", R.drawable.ic_cat_alimentacao),
            new CategoriaItem("Moradia", 0, true),
            new CategoriaItem("Transporte", R.drawable.ic_cat_transporte),
            new CategoriaItem("Compras", R.drawable.ic_cat_compras),
            new CategoriaItem("Lazer", R.drawable.ic_cat_lazer),
            new CategoriaItem("Saúde", R.drawable.ic_cat_saude),
            new CategoriaItem("Educação", R.drawable.ic_cat_educacao),
            new CategoriaItem("Outros", R.drawable.ic_cat_outros)
    };

    private final CategoriaItem[] categoriasReceita = {
            new CategoriaItem("Salário", R.drawable.ic_cat_salario),
            new CategoriaItem("Freelance", R.drawable.ic_cat_freelance),
            new CategoriaItem("Vendas", R.drawable.ic_cat_compras),
            new CategoriaItem("Investimentos", R.drawable.ic_cat_investimentos),
            new CategoriaItem("Presentes", R.drawable.ic_cat_presentes),
            new CategoriaItem("Outros", R.drawable.ic_cat_outros)
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nova_movimentacao);

// VALIDACAO DOS CAMPOS

        android.widget.EditText edtValor =
                findViewById(R.id.edtValorMovimentacao);

// FORMATACAO AUTOMATICA DO VALOR

        final java.text.NumberFormat formatoValor =
                java.text.NumberFormat.getNumberInstance(
                        new java.util.Locale("pt", "BR")
                );

        formatoValor.setMinimumFractionDigits(2);
        formatoValor.setMaximumFractionDigits(2);

        edtValor.addTextChangedListener(new android.text.TextWatcher() {

            private boolean formatando = false;

            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable texto) {

                if (formatando) {
                    return;
                }

                // Manter apenas os numeros digitados
                String numeros = texto.toString()
                        .replaceAll("[^0-9]", "");

                if (numeros.isEmpty()) {
                    return;
                }

                // Converter os dois ultimos digitos em centavos
                java.math.BigDecimal valor =
                        new java.math.BigDecimal(numeros)
                                .movePointLeft(2);

                // Formatar no padrao brasileiro
                String novoTexto = formatoValor.format(valor);

                if (!novoTexto.equals(texto.toString())) {

                    formatando = true;

                    try {
                        edtValor.setText(novoTexto);

                        // Manter cursor no final
                        edtValor.setSelection(
                                novoTexto.length()
                        );

                    } finally {
                        formatando = false;
                    }
                }
            }
        });

        android.widget.EditText edtDescricao =
                findViewById(R.id.edtDescricaoMovimentacao);


// ETAPA 84 - SALVAR MOVIMENTACAO NO SQLITE

        findViewById(R.id.btnSalvarMovimentacao)
                .setOnClickListener(botao -> {

                    // Obter os campos preenchidos
                    android.widget.EditText campoValor =
                            findViewById(R.id.edtValorMovimentacao);

                    android.widget.EditText campoDescricao =
                            findViewById(R.id.edtDescricaoMovimentacao);

                    android.widget.TextView campoCategoria =
                            findViewById(R.id.txtCategoriaSelecionada);

                    android.widget.TextView campoData =
                            findViewById(R.id.txtDataMovimentacao);

                    // Obter as informacoes
                    String valorDigitado =
                            campoValor.getText().toString().trim();

                    String descricao =
                            campoDescricao.getText().toString().trim();

                    String categoria =
                            categoriaSelecionada == null ? "" : categoriaSelecionada;

                    String data =
                            campoData.getText().toString().trim();

                    // Identificar se e receita ou despesa
                    String tipo = findViewById(R.id.btnReceita).isSelected()
                            ? "Receita"
                            : "Despesa";

                    // VALIDAR VALOR
                    if (valorDigitado.isEmpty()) {
                        campoValor.setError("Informe o valor.");
                        campoValor.requestFocus();
                        return;
                    }

                    // Exemplo: "1.234,56" transforma em "123456"
                    String numeros = valorDigitado.replaceAll("[^0-9]", "");

                    long valorCentavos;

                    try {
                        valorCentavos = Long.parseLong(numeros);

                    } catch (NumberFormatException erro) {
                        campoValor.setError("Valor inválido ou muito alto.");
                        campoValor.requestFocus();
                        return;
                    }

                    if (valorCentavos <= 0) {
                        campoValor.setError("Informe um valor maior que zero.");
                        campoValor.requestFocus();
                        return;
                    }

                    // VALIDAR DESCRICAO
                    if (descricao.isEmpty()) {
                        campoDescricao.setError("Informe uma descrição.");
                        campoDescricao.requestFocus();
                        return;
                    }

                    // VALIDAR CATEGORIA
                    if (categoria.isEmpty()) {
                        android.widget.Toast.makeText(
                                NovaMovimentacaoActivity.this,
                                "Selecione uma categoria.",
                                android.widget.Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    // VALIDAR DATA
                    if (data.isEmpty()) {
                        android.widget.Toast.makeText(
                                NovaMovimentacaoActivity.this,
                                "Selecione uma data.",
                                android.widget.Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    // SALVAR NO BANCO DE DADOS
                    BancoDadosHelper banco =
                            new BancoDadosHelper(NovaMovimentacaoActivity.this);

                    long idMovimentacao;

                    try {
                        idMovimentacao = banco.salvarMovimentacao(
                                tipo,
                                valorCentavos,
                                categoria,
                                data,
                                descricao
                        );

                    } catch (Exception erro) {

                        android.widget.Toast.makeText(
                                NovaMovimentacaoActivity.this,
                                "Erro ao salvar movimentação.",
                                android.widget.Toast.LENGTH_LONG
                        ).show();

                        return;

                    } finally {
                        banco.close();
                    }

                    // VERIFICAR SE SALVOU CORRETAMENTE
                    if (idMovimentacao != -1) {

                        android.widget.Toast.makeText(
                                NovaMovimentacaoActivity.this,
                                "Movimentação salva com sucesso!",
                                android.widget.Toast.LENGTH_LONG
                        ).show();

                        // Voltar ao Dashboard
                        finish();

                    } else {

                        android.widget.Toast.makeText(
                                NovaMovimentacaoActivity.this,
                                "Não foi possível salvar a movimentação.",
                                android.widget.Toast.LENGTH_LONG
                        ).show();
                    }

                });

// CALENDARIO DA NOVA MOVIMENTACAO

// Obter a data atual do celular
        final java.util.Calendar calendarioDataMovimentacao =
                java.util.Calendar.getInstance();

// Campo que mostra a data escolhida
        android.widget.TextView txtDataMovimentacao =
                findViewById(R.id.txtDataMovimentacao);

// Mostrar a data atual inicialmente
        txtDataMovimentacao.setText(
                String.format(
                        java.util.Locale.forLanguageTag("pt-BR"),
                        "%02d/%02d/%04d",
                        calendarioDataMovimentacao.get(
                                java.util.Calendar.DAY_OF_MONTH),
                        calendarioDataMovimentacao.get(
                                java.util.Calendar.MONTH) + 1,
                        calendarioDataMovimentacao.get(
                                java.util.Calendar.YEAR)
                )
        );

// Abrir calendario ao tocar no campo Data
        findViewById(R.id.btnSelecionarData)
                .setOnClickListener(botao -> {

                    new android.app.DatePickerDialog(
                            NovaMovimentacaoActivity.this,

                            (seletor, ano, mes, dia) -> {

                                calendarioDataMovimentacao.set(
                                        ano, mes, dia
                                );

                                txtDataMovimentacao.setText(
                                        String.format(
                                                java.util.Locale.forLanguageTag("pt-BR"),
                                                "%02d/%02d/%04d",
                                                dia, mes + 1, ano
                                        )
                                );
                            },

                            calendarioDataMovimentacao.get(
                                    java.util.Calendar.YEAR),

                            calendarioDataMovimentacao.get(
                                    java.util.Calendar.MONTH),

                            calendarioDataMovimentacao.get(
                                    java.util.Calendar.DAY_OF_MONTH)

                    ).show();
                });

// CATEGORIAS ESPECIFICAS PARA DESPESA E RECEITA
        // Tocar no campo superior tambem abre a lista.
        findViewById(R.id.campoCategoria).setOnClickListener(botao -> {
            CategoriaItem[] opcoes = obterCategoriasAtuais();
            String[] nomes = new String[opcoes.length];
            for (int i = 0; i < opcoes.length; i++) {
                nomes[i] = opcoes[i].nome;
            }
            new androidx.appcompat.app.AlertDialog.Builder(
                    NovaMovimentacaoActivity.this)
                    .setTitle("Selecione uma categoria")
                    .setItems(nomes, (dialogo, indice) ->
                            selecionarCategoria(opcoes[indice]))
                    .show();
        });

        // SELETOR DE TIPO
        findViewById(R.id.btnDespesa).setOnClickListener(botao ->
                alterarTipo(false));
        findViewById(R.id.btnReceita).setOnClickListener(botao ->
                alterarTipo(true));

        // Comeca em Despesa, aguardando a escolha de uma categoria.
        alterarTipo(false);

        // BOTAO VOLTAR
        findViewById(R.id.btnVoltarMovimentacao)
                .setOnClickListener(botao -> {
                    finish();
                });

        // BOTAO CANCELAR
        findViewById(R.id.btnCancelar)
                .setOnClickListener(botao -> {
                    finish();
                });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }


    private CategoriaItem[] obterCategoriasAtuais() {
        return receitaSelecionada ? categoriasReceita : categoriasDespesa;
    }

    private int dp(int valor) {
        return (int) (valor * getResources().getDisplayMetrics().density + 0.5f);
    }

    // Ao alternar Despesa/Receita, limpa a categoria anterior.
    private void alterarTipo(boolean receita) {
        receitaSelecionada = receita;
        categoriaSelecionada = null;

        android.widget.TextView botaoDespesa = findViewById(R.id.btnDespesa);
        android.widget.TextView botaoReceita = findViewById(R.id.btnReceita);
        android.widget.TextView selecionado = receita ? botaoReceita : botaoDespesa;
        android.widget.TextView naoSelecionado = receita ? botaoDespesa : botaoReceita;

        selecionado.setSelected(true);
        naoSelecionado.setSelected(false);
        selecionado.setBackgroundResource(R.drawable.bg_card_saldo);
        naoSelecionado.setBackground(null);
        selecionado.setTextColor(getColor(R.color.mb_white));
        naoSelecionado.setTextColor(getColor(R.color.mb_purple_dark));
        selecionado.setTypeface(null, android.graphics.Typeface.BOLD);
        naoSelecionado.setTypeface(null, android.graphics.Typeface.NORMAL);

        ((android.widget.TextView) findViewById(R.id.txtCategoriaSelecionada))
                .setText("Selecione uma categoria");
        findViewById(R.id.imgCategoriaSelecionada).setVisibility(android.view.View.GONE);
        findViewById(R.id.txtIconeMoradia).setVisibility(android.view.View.GONE);
        montarGradeCategorias();
    }

    // Atualiza o campo superior e destaca somente a categoria escolhida.
    private void selecionarCategoria(CategoriaItem item) {
        categoriaSelecionada = item.nome;
        ((android.widget.TextView) findViewById(R.id.txtCategoriaSelecionada))
                .setText(item.nome);

        android.widget.ImageView imagem = findViewById(R.id.imgCategoriaSelecionada);
        android.widget.TextView simboloMoradia = findViewById(R.id.txtIconeMoradia);
        if (item.moradia) {
            imagem.setVisibility(android.view.View.GONE);
            simboloMoradia.setVisibility(android.view.View.VISIBLE);
        } else {
            simboloMoradia.setVisibility(android.view.View.GONE);
            imagem.setImageResource(item.icone);
            imagem.setVisibility(android.view.View.VISIBLE);
        }
        montarGradeCategorias();
    }

    // Grade de 4 colunas; os icones sao vetoriais, exceto Moradia.
    private void montarGradeCategorias() {
        android.widget.LinearLayout grade = findViewById(R.id.gradeCategorias);
        grade.removeAllViews();
        CategoriaItem[] opcoes = obterCategoriasAtuais();
        android.widget.LinearLayout linha = null;

        for (int i = 0; i < opcoes.length; i++) {
            if (i % 4 == 0) {
                linha = new android.widget.LinearLayout(this);
                linha.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                linha.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
                grade.addView(linha);
            }

            CategoriaItem item = opcoes[i];
            android.widget.LinearLayout cartao = new android.widget.LinearLayout(this);
            cartao.setOrientation(android.widget.LinearLayout.VERTICAL);
            cartao.setGravity(android.view.Gravity.CENTER);
            cartao.setPadding(dp(2), dp(8), dp(2), dp(6));
            cartao.setBackgroundResource(R.drawable.bg_card_despesas);

            boolean ativo = item.nome.equals(categoriaSelecionada);
            int cor = android.graphics.Color.parseColor(
                    ativo ? "#FAD0E2" : "#F8EAF3");
            cartao.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(cor));

            android.widget.LinearLayout.LayoutParams parametros =
                    new android.widget.LinearLayout.LayoutParams(0, dp(91), 1f);
            parametros.setMargins(dp(3), dp(3), dp(3), dp(3));
            linha.addView(cartao, parametros);

            if (item.moradia) {
                // Preserva exatamente o simbolo original de Moradia.
                android.widget.TextView casa = new android.widget.TextView(this);
                casa.setText("⌂");
                casa.setTextSize(25);
                casa.setGravity(android.view.Gravity.CENTER);
                casa.setTextColor(getColor(R.color.mb_purple_dark));
                cartao.addView(casa, new android.widget.LinearLayout.LayoutParams(
                        dp(30), dp(34)));
            } else {
                android.widget.ImageView imagem = new android.widget.ImageView(this);
                imagem.setImageResource(item.icone);
                imagem.setColorFilter(getColor(R.color.mb_purple_dark));
                imagem.setContentDescription("Ícone " + item.nome);
                cartao.addView(imagem, new android.widget.LinearLayout.LayoutParams(
                        dp(28), dp(34)));
            }

            android.widget.TextView legenda = new android.widget.TextView(this);
            legenda.setText(item.nome);
            legenda.setTextSize(10.5f);
            legenda.setTextColor(getColor(R.color.mb_purple_dark));
            legenda.setGravity(android.view.Gravity.CENTER);
            legenda.setMaxLines(2);
            legenda.setEllipsize(android.text.TextUtils.TruncateAt.END);
            android.widget.LinearLayout.LayoutParams textoParams =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            textoParams.topMargin = dp(3);
            cartao.addView(legenda, textoParams);

            cartao.setOnClickListener(botao -> selecionarCategoria(item));
        }

        // Completa a ultima linha, mantendo as colunas igualmente largas.
        int faltantes = (4 - opcoes.length % 4) % 4;
        if (linha != null) {
            for (int i = 0; i < faltantes; i++) {
                android.view.View espaco = new android.view.View(this);
                linha.addView(espaco, new android.widget.LinearLayout.LayoutParams(
                        0, dp(1), 1f));
            }
        }
    }

}