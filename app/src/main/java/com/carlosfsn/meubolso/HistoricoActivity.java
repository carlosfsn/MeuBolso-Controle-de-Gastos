package com.carlosfsn.meubolso;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoricoActivity extends AppCompatActivity {

    // Os meses do Calendar comecam em zero (janeiro = 0).
    private int mesEscolhido;
    private int anoEscolhido;
    private int filtroAtual = 0; // 0 = Todos; 1 = Entradas; 2 = Saidas
    private boolean maisRecentesPrimeiro = true;

    private LinearLayout listaMovimentacoes;
    private final List<Movimentacao> movimentacoes = new ArrayList<>();
    private long receitasPeriodoCentavos = 0;
    private long despesasPeriodoCentavos = 0;

    private final Locale localeBR = new Locale("pt", "BR");
    private final String[] meses = {
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };

    private static class Movimentacao {
        long id;
        String tipo;
        long valorCentavos;
        String categoria;
        String data;
        String descricao;
        long dataOrdenacao;
        int mes;
        int ano;

        boolean receita() {
            return "Receita".equalsIgnoreCase(tipo);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_historico);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (tela, insets) -> {
                    Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    tela.setPadding(barras.left, barras.top, barras.right, barras.bottom);
                    return insets;
                }
        );

        // Abrir diretamente no mes atual do aparelho.
        java.util.Calendar hoje = java.util.Calendar.getInstance();
        mesEscolhido = hoje.get(java.util.Calendar.MONTH);
        anoEscolhido = hoje.get(java.util.Calendar.YEAR);
        atualizarTextoMes();

        // Os sete exemplos que estavam no XML deixam de aparecer.
        int[] idsExemplos = {
                R.id.itemHistoricoSalario,
                R.id.itemHistoricoAlimentacao,
                R.id.itemHistoricoTransporte,
                R.id.itemHistoricoMercado,
                R.id.itemHistoricoLazer,
                R.id.itemHistoricoContaLuz,
                R.id.itemHistoricoAlimentacao2
        };
        for (int id : idsExemplos) {
            findViewById(id).setVisibility(View.GONE);
        }

        // A lista real ocupa exatamente o lugar dos cartoes antigos.
        LinearLayout conteudo = findViewById(R.id.conteudoHistorico);
        int posicaoLista = conteudo.indexOfChild(findViewById(R.id.itemHistoricoSalario));
        listaMovimentacoes = new LinearLayout(this);
        listaMovimentacoes.setOrientation(LinearLayout.VERTICAL);
        conteudo.addView(
                listaMovimentacoes,
                posicaoLista,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        findViewById(R.id.navInicio).setOnClickListener(botao -> finish());
        // ETAPA 89: acesso as categorias pelo Historico.
        findViewById(R.id.navCategorias).setOnClickListener(botao ->
                startActivity(new android.content.Intent(HistoricoActivity.this, CategoriasActivity.class))
        );
        findViewById(R.id.navNova).setOnClickListener(botao ->
                startActivity(new android.content.Intent(HistoricoActivity.this, NovaMovimentacaoActivity.class))
        );
        // ETAPA 91: abrir Perfil na barra inferior.
        findViewById(R.id.navPerfil).setOnClickListener(botao ->
                startActivity(new android.content.Intent(HistoricoActivity.this, PerfilActivity.class))
        );
        findViewById(R.id.filtroTodos).setOnClickListener(botao -> aplicarFiltro(0));
        findViewById(R.id.filtroEntradas).setOnClickListener(botao -> aplicarFiltro(1));
        findViewById(R.id.filtroSaidas).setOnClickListener(botao -> aplicarFiltro(2));

        findViewById(R.id.seletorMes).setOnClickListener(botao -> {
            new DatePickerDialog(
                    HistoricoActivity.this,
                    (seletor, ano, mes, dia) -> {
                        mesEscolhido = mes;
                        anoEscolhido = ano;
                        atualizarTextoMes();
                        atualizarHistorico();
                    },
                    anoEscolhido, mesEscolhido, 1
            ).show();
        });

        findViewById(R.id.ordenarHistorico).setOnClickListener(botao -> {
            maisRecentesPrimeiro = !maisRecentesPrimeiro;
            atualizarHistorico();
        });

        findViewById(R.id.txtVerGrafico).setOnClickListener(
                botao -> mostrarGraficoPeriodo()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarMovimentacoesSQLite();
    }

    // Le os dados salvos anteriormente pela NovaMovimentacaoActivity.
    private void carregarMovimentacoesSQLite() {
        movimentacoes.clear();
        BancoDadosHelper helper = new BancoDadosHelper(this);
        try (Cursor cursor = helper.listarMovimentacoes()) {
            while (cursor.moveToNext()) {
                Movimentacao item = new Movimentacao();
                item.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                item.tipo = cursor.getString(cursor.getColumnIndexOrThrow("tipo"));
                item.valorCentavos = cursor.getLong(
                        cursor.getColumnIndexOrThrow("valor_centavos")
                );
                item.categoria = cursor.getString(cursor.getColumnIndexOrThrow("categoria"));
                item.data = cursor.getString(cursor.getColumnIndexOrThrow("data"));
                item.descricao = cursor.getString(cursor.getColumnIndexOrThrow("descricao"));

                try {
                    SimpleDateFormat parser = new SimpleDateFormat("dd/MM/yyyy", localeBR);
                    parser.setLenient(false);
                    Date data = parser.parse(item.data);
                    if (data == null) continue;
                    java.util.Calendar calendario = java.util.Calendar.getInstance();
                    calendario.setTime(data);
                    item.mes = calendario.get(java.util.Calendar.MONTH);
                    item.ano = calendario.get(java.util.Calendar.YEAR);
                    item.dataOrdenacao = data.getTime();
                    movimentacoes.add(item);
                } catch (java.text.ParseException erroData) {
                    // Ignora somente registros com data fora do formato DD/MM/AAAA.
                }
            }
        } catch (Exception erro) {
            Toast.makeText(this, "Não foi possível carregar as movimentações.",
                    Toast.LENGTH_LONG).show();
        } finally {
            helper.close();
        }
        atualizarHistorico();
    }

    private void atualizarTextoMes() {
        TextView texto = findViewById(R.id.txtMesHistorico);
        texto.setText(meses[mesEscolhido] + " " + anoEscolhido);
    }

    private void aplicarFiltro(int filtro) {
        filtroAtual = filtro;
        atualizarHistorico();
    }

    private void atualizarHistorico() {
        if (listaMovimentacoes == null) return;
        listaMovimentacoes.removeAllViews();

        // Resumo sempre considera todas as receitas/despesas do mes,
        // independentemente de qual botao de filtro esta selecionado.
        receitasPeriodoCentavos = 0;
        despesasPeriodoCentavos = 0;
        List<Movimentacao> exibidas = new ArrayList<>();
        int quantidadePeriodo = 0;

        for (Movimentacao item : movimentacoes) {
            if (item.mes != mesEscolhido || item.ano != anoEscolhido) continue;
            quantidadePeriodo++;
            if (item.receita()) {
                receitasPeriodoCentavos += item.valorCentavos;
            } else {
                despesasPeriodoCentavos += item.valorCentavos;
            }

            if (filtroAtual == 1 && !item.receita()) continue;
            if (filtroAtual == 2 && item.receita()) continue;
            exibidas.add(item);
        }

        // Ordenacao real pela data salva. O ID desempata datas iguais.
        Collections.sort(exibidas, (a, b) -> {
            int comparacao = Long.compare(a.dataOrdenacao, b.dataOrdenacao);
            if (comparacao == 0) comparacao = Long.compare(a.id, b.id);
            return maisRecentesPrimeiro ? -comparacao : comparacao;
        });

        if (exibidas.isEmpty()) {
            TextView mensagem = new TextView(this);
            mensagem.setText("Nenhuma movimentação encontrada para "
                    + meses[mesEscolhido] + " de " + anoEscolhido + ".");
            mensagem.setTextSize(15);
            mensagem.setTextColor(getColor(R.color.mb_muted));
            mensagem.setGravity(Gravity.CENTER);
            mensagem.setBackgroundResource(R.drawable.bg_card_resumo);
            mensagem.setPadding(dp(18), dp(32), dp(18), dp(32));
            listaMovimentacoes.addView(mensagem);
        } else {
            for (Movimentacao item : exibidas) {
                listaMovimentacoes.addView(criarCartao(item));
            }
        }

        // Nunca mostrar os totais ficticios do XML.
        View resumo = findViewById(R.id.cardResumoPeriodo);
        resumo.setVisibility(quantidadePeriodo > 0 ? View.VISIBLE : View.GONE);
        if (quantidadePeriodo > 0) atualizarValoresResumo();

        TextView ordenar = findViewById(R.id.ordenarHistorico);
        ordenar.setText(maisRecentesPrimeiro ? "Mais recentes ⌄" : "Mais antigas ⌄");

        TextView[] filtros = {
                findViewById(R.id.filtroTodos),
                findViewById(R.id.filtroEntradas),
                findViewById(R.id.filtroSaidas)
        };
        for (int i = 0; i < filtros.length; i++) {
            if (i == filtroAtual) {
                filtros[i].setBackgroundResource(R.drawable.bg_card_saldo);
                filtros[i].setTextColor(getColor(R.color.mb_white));
            } else {
                filtros[i].setBackground(null);
                filtros[i].setTextColor(getColor(R.color.mb_purple_dark));
            }
        }
    }

    // Usa a mesma estrutura do resumo original: receitas, despesas, saldo.
    private void atualizarValoresResumo() {
        LinearLayout resumo = findViewById(R.id.cardResumoPeriodo);
        if (resumo.getChildCount() < 2 || !(resumo.getChildAt(1) instanceof LinearLayout)) {
            resumo.setVisibility(View.GONE);
            return;
        }
        LinearLayout colunas = (LinearLayout) resumo.getChildAt(1);
        long[] valores = {
                receitasPeriodoCentavos,
                despesasPeriodoCentavos,
                receitasPeriodoCentavos - despesasPeriodoCentavos
        };
        for (int i = 0; i < 3; i++) {
            int indiceColuna = i * 2; // 0, 2, 4; linhas divisorias nos indices 1 e 3.
            if (colunas.getChildCount() <= indiceColuna
                    || !(colunas.getChildAt(indiceColuna) instanceof LinearLayout)) {
                resumo.setVisibility(View.GONE);
                return;
            }
            LinearLayout coluna = (LinearLayout) colunas.getChildAt(indiceColuna);
            if (coluna.getChildCount() < 3 || !(coluna.getChildAt(2) instanceof TextView)) {
                resumo.setVisibility(View.GONE);
                return;
            }
            TextView valor = (TextView) coluna.getChildAt(2);
            valor.setText(formatarMoeda(valores[i]));
        }
    }

    // Cartao de movimentacao com seta verde ou vermelha, como no design.
    private View criarCartao(Movimentacao item) {
        boolean receita = item.receita();
        LinearLayout cartao = new LinearLayout(this);
        cartao.setOrientation(LinearLayout.HORIZONTAL);
        cartao.setGravity(Gravity.CENTER_VERTICAL);
        cartao.setPadding(dp(12), dp(12), dp(12), dp(12));
        cartao.setBackgroundResource(R.drawable.bg_card_resumo);
        LinearLayout.LayoutParams parametrosCartao = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        parametrosCartao.bottomMargin = dp(7);
        cartao.setLayoutParams(parametrosCartao);

        TextView icone = new TextView(this);
        icone.setWidth(dp(44));
        icone.setHeight(dp(44));
        icone.setGravity(Gravity.CENTER);
        icone.setText(receita ? "↑" : "↓");
        icone.setTextSize(28);
        icone.setTypeface(null, Typeface.BOLD);
        icone.setTextColor(getColor(receita ? R.color.mb_green : R.color.mb_red));
        icone.setBackgroundResource(R.drawable.bg_card_resumo);
        icone.setBackgroundTintList(ColorStateList.valueOf(
                receita ? 0xFFD9F6EA : 0xFFFFDFE7
        ));
        cartao.addView(icone, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams parametrosTextos =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        parametrosTextos.leftMargin = dp(10);
        cartao.addView(textos, parametrosTextos);
        TextView titulo = new TextView(this);
        titulo.setText(item.categoria);
        titulo.setTextColor(getColor(R.color.mb_ink));
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setTextSize(14);
        textos.addView(titulo);
        TextView descricao = new TextView(this);
        descricao.setText(item.descricao);
        descricao.setTextColor(getColor(R.color.mb_muted));
        descricao.setTextSize(11);
        descricao.setSingleLine(true);
        descricao.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textos.addView(descricao);
        TextView data = new TextView(this);
        data.setText(item.data);
        data.setTextColor(getColor(R.color.mb_muted));
        data.setTextSize(11);
        textos.addView(data);

        TextView valor = new TextView(this);
        valor.setText((receita ? "+ " : "- ") + formatarMoeda(item.valorCentavos));
        valor.setTextColor(getColor(receita ? R.color.mb_green : R.color.mb_red));
        valor.setTextSize(13);
        valor.setTypeface(null, Typeface.BOLD);
        valor.setSingleLine(true);
        cartao.addView(valor);

        TextView setaDetalhes = new TextView(this);
        setaDetalhes.setText("›");
        setaDetalhes.setTextSize(22);
        setaDetalhes.setTextColor(getColor(R.color.mb_muted));
        LinearLayout.LayoutParams parametrosSeta = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        parametrosSeta.leftMargin = dp(6);
        cartao.addView(setaDetalhes, parametrosSeta);

        cartao.setOnClickListener(botao -> mostrarDetalhes(item));
        return cartao;
    }

    private void mostrarDetalhes(Movimentacao item) {
        String valor = (item.receita() ? "+ " : "- ") + formatarMoeda(item.valorCentavos);
        new AlertDialog.Builder(this)
                .setTitle("Detalhes da movimentação")
                .setMessage("Categoria: " + item.categoria + "\n\n"
                        + "Tipo: " + item.tipo + "\n\n"
                        + "Descrição: " + item.descricao + "\n\n"
                        + "Data: " + item.data + "\n\n"
                        + "Valor: " + valor)
                .setPositiveButton("Fechar", (dialogo, opcao) -> dialogo.dismiss())
                .show();
    }

    private String formatarMoeda(long centavos) {
        NumberFormat formato = NumberFormat.getNumberInstance(localeBR);
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        BigDecimal decimal = BigDecimal.valueOf(centavos, 2);
        return "R$ " + formato.format(decimal);
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    // Mesmo grafico roxo-escuro com letras brancas, agora com valores reais.
    private void mostrarGraficoPeriodo() {
        long receitas = receitasPeriodoCentavos;
        long despesas = despesasPeriodoCentavos;
        int percentual = receitas > 0
                ? (int) Math.round(100d * despesas / receitas) : 0;
        int proporcaoBarra = Math.max(0, Math.min(100, percentual));

        int margem = dp(24);
        int separacao = dp(14);

        TextView titulo = new TextView(this);
        titulo.setText("Gráfico do período");
        titulo.setTextSize(21);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setTextColor(Color.WHITE);
        titulo.setPadding(margem, margem, margem, 0);

        LinearLayout conteudo = new LinearLayout(this);
        conteudo.setOrientation(LinearLayout.VERTICAL);
        conteudo.setPadding(margem, separacao, margem, separacao);

        TextView explicacao = new TextView(this);
        String mensagem = receitas > 0
                ? percentual + "% das receitas foram utilizadas em despesas."
                : "Não há receitas registradas neste período.";
        explicacao.setText(meses[mesEscolhido] + " de " + anoEscolhido + "\n" + mensagem);
        explicacao.setTextSize(15);
        explicacao.setTextColor(Color.WHITE);
        conteudo.addView(explicacao);

        ProgressBar grafico = new ProgressBar(
                this, null, android.R.attr.progressBarStyleHorizontal
        );
        grafico.setMax(100);
        grafico.setProgress(proporcaoBarra);
        grafico.setProgressTintList(ColorStateList.valueOf(0xFFFF688B));
        grafico.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF9687D8));

        LinearLayout.LayoutParams parametros = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(18)
        );
        parametros.topMargin = separacao;
        parametros.bottomMargin = separacao;
        conteudo.addView(grafico, parametros);

        TextView valores = new TextView(this);
        valores.setText("Receitas: " + formatarMoeda(receitas)
                + "\nDespesas: " + formatarMoeda(despesas)
                + "\nSaldo restante: " + formatarMoeda(receitas - despesas));
        valores.setTextSize(15);
        valores.setLineSpacing(dp(5), 1f);
        valores.setTextColor(Color.WHITE);
        conteudo.addView(valores);

        GradientDrawable fundo = new GradientDrawable();
        fundo.setColor(0xFF493995);
        fundo.setCornerRadius(dp(22));

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setCustomTitle(titulo)
                .setView(conteudo)
                .setPositiveButton("Fechar", (janela, opcao) -> janela.dismiss())
                .create();
        dialogo.show();
        if (dialogo.getWindow() != null) {
            dialogo.getWindow().setBackgroundDrawable(fundo);
        }
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
    }
}