package com.carlosfsn.meubolso;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CategoriasActivity extends AppCompatActivity {

    private boolean mostrandoReceitas = false;
    private final Locale localeBrasil = new Locale("pt", "BR");
    private final List<RegistroCategoria> registrosBanco = new ArrayList<>();
    private LinearLayout listaCategorias;
    private TextView tabDespesas;
    private TextView tabReceitas;

    private static class RegistroCategoria {
        String tipo;
        String categoria;
        long centavos;
    }

    private static class ResumoCategoria {
        final String nome;
        final String descricao;
        final int icone;
        final boolean moradia;
        long centavos;
        int quantidade;

        ResumoCategoria(String nome, String descricao, int icone, boolean moradia) {
            this.nome = nome;
            this.descricao = descricao;
            this.icone = icone;
            this.moradia = moradia;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_categorias);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (tela, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            tela.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        listaCategorias = findViewById(R.id.listaCategorias);
        tabDespesas = findViewById(R.id.tabCategoriasDespesas);
        tabReceitas = findViewById(R.id.tabCategoriasReceitas);

        tabDespesas.setOnClickListener(v -> escolherTipo(false));
        tabReceitas.setOnClickListener(v -> escolherTipo(true));

        findViewById(R.id.navInicio).setOnClickListener(v -> {
            Intent intent = new Intent(this, InicioActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.navHistorico).setOnClickListener(v ->
                startActivity(new Intent(this, HistoricoActivity.class))
        );
        findViewById(R.id.navNova).setOnClickListener(v ->
                startActivity(new Intent(this, NovaMovimentacaoActivity.class))
        );
        // ETAPA 91: abrir Perfil na barra inferior.
        findViewById(R.id.navPerfil).setOnClickListener(v ->
                startActivity(new Intent(this, PerfilActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarBanco();
        escolherTipo(mostrandoReceitas);
    }

    private void carregarBanco() {
        registrosBanco.clear();
        BancoDadosHelper helper = new BancoDadosHelper(this);
        try (Cursor cursor = helper.listarMovimentacoes()) {
            while (cursor.moveToNext()) {
                RegistroCategoria registro = new RegistroCategoria();
                registro.tipo = cursor.getString(cursor.getColumnIndexOrThrow("tipo"));
                registro.categoria = cursor.getString(cursor.getColumnIndexOrThrow("categoria"));
                registro.centavos = cursor.getLong(cursor.getColumnIndexOrThrow("valor_centavos"));
                registrosBanco.add(registro);
            }
        } catch (Exception erro) {
            Toast.makeText(this, "Não foi possível carregar as categorias.", Toast.LENGTH_LONG).show();
        } finally {
            helper.close();
        }
    }

    private void escolherTipo(boolean receita) {
        mostrandoReceitas = receita;
        TextView ativo = receita ? tabReceitas : tabDespesas;
        TextView inativo = receita ? tabDespesas : tabReceitas;
        ativo.setBackgroundResource(R.drawable.bg_card_saldo);
        ativo.setTextColor(getColor(R.color.mb_white));
        ativo.setTypeface(null, Typeface.BOLD);
        inativo.setBackground(null);
        inativo.setTextColor(getColor(R.color.mb_purple_dark));
        inativo.setTypeface(null, Typeface.NORMAL);
        preencherListaCategorias();
    }

    private LinkedHashMap<String, ResumoCategoria> categoriasPadrao() {
        LinkedHashMap<String, ResumoCategoria> mapa = new LinkedHashMap<>();
        if (mostrandoReceitas) {
            adicionar(mapa, "Salário", "Salário mensal e benefícios", R.drawable.ic_cat_salario, false);
            adicionar(mapa, "Freelance", "Serviços e trabalhos avulsos", R.drawable.ic_cat_freelance, false);
            adicionar(mapa, "Vendas", "Vendas de produtos e itens", R.drawable.ic_cat_compras, false);
            adicionar(mapa, "Investimentos", "Rendimentos e aplicações", R.drawable.ic_cat_investimentos, false);
            adicionar(mapa, "Presentes", "Valores recebidos de presente", R.drawable.ic_cat_presentes, false);
            adicionar(mapa, "Outros", "Outras receitas", R.drawable.ic_cat_outros, false);
        } else {
            adicionar(mapa, "Alimentação", "Restaurantes, mercado e delivery", R.drawable.ic_cat_alimentacao, false);
            adicionar(mapa, "Transporte", "Combustível, transporte e aplicativos", R.drawable.ic_cat_transporte, false);
            adicionar(mapa, "Moradia", "Aluguel, condomínio e contas da casa", 0, true);
            adicionar(mapa, "Saúde", "Medicamentos, consultas e exames", R.drawable.ic_cat_saude, false);
            adicionar(mapa, "Lazer", "Cinema, viagens e diversão", R.drawable.ic_cat_lazer, false);
            adicionar(mapa, "Compras", "Roupas, eletrônicos e outros", R.drawable.ic_cat_compras, false);
            adicionar(mapa, "Educação", "Cursos, livros e materiais", R.drawable.ic_cat_educacao, false);
            adicionar(mapa, "Outros", "Despesas diversas", R.drawable.ic_cat_outros, false);
        }
        return mapa;
    }

    private void adicionar(Map<String, ResumoCategoria> mapa, String nome,
                           String descricao, int icone, boolean moradia) {
        mapa.put(nome.toLowerCase(localeBrasil), new ResumoCategoria(nome, descricao, icone, moradia));
    }

    private void preencherListaCategorias() {
        listaCategorias.removeAllViews();
        LinkedHashMap<String, ResumoCategoria> mapa = categoriasPadrao();
        String tipoExibido = mostrandoReceitas ? "Receita" : "Despesa";

        for (RegistroCategoria registro : registrosBanco) {
            if (registro.tipo == null || !tipoExibido.equalsIgnoreCase(registro.tipo)) {
                continue;
            }
            if (registro.categoria == null || registro.categoria.trim().isEmpty()) {
                continue;
            }
            String chave = registro.categoria.trim().toLowerCase(localeBrasil);
            ResumoCategoria resumo = mapa.get(chave);
            if (resumo == null) {
                resumo = new ResumoCategoria(registro.categoria.trim(),
                        "Categoria já utilizada", R.drawable.ic_cat_outros, false);
                mapa.put(chave, resumo);
            }
            resumo.centavos += registro.centavos;
            resumo.quantidade++;
        }

        for (ResumoCategoria resumo : mapa.values()) {
            adicionarCartao(resumo);
        }
    }

    private void adicionarCartao(ResumoCategoria categoria) {
        LinearLayout cartao = new LinearLayout(this);
        cartao.setOrientation(LinearLayout.HORIZONTAL);
        cartao.setGravity(Gravity.CENTER_VERTICAL);
        cartao.setPadding(dp(11), dp(12), dp(12), dp(12));
        cartao.setBackground(fundoArredondado(Color.WHITE, 20));
        cartao.setElevation(dp(1));
        LinearLayout.LayoutParams lpCartao = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpCartao.topMargin = dp(10);
        listaCategorias.addView(cartao, lpCartao);

        LinearLayout caixaIcone = new LinearLayout(this);
        caixaIcone.setGravity(Gravity.CENTER);
        int corFundo = (categoria.moradia || categoria.icone == R.drawable.ic_cat_transporte ||
                categoria.icone == R.drawable.ic_cat_compras) ? Color.parseColor("#EEE8FF") :
                Color.parseColor("#FFE6F0");
        caixaIcone.setBackground(fundoArredondado(corFundo, 100));
        LinearLayout.LayoutParams lpIcone = new LinearLayout.LayoutParams(dp(48), dp(48));
        lpIcone.rightMargin = dp(12);
        cartao.addView(caixaIcone, lpIcone);

        if (categoria.moradia) {
            TextView simboloCasa = new TextView(this);
            simboloCasa.setText("⌂"); // Mesmo simbolo da categoria Moradia na Nova Movimentacao.
            simboloCasa.setTextColor(getColor(R.color.mb_purple_dark));
            simboloCasa.setTextSize(31);
            simboloCasa.setGravity(Gravity.CENTER);
            caixaIcone.addView(simboloCasa);
        } else {
            ImageView imagem = new ImageView(this);
            imagem.setImageResource(categoria.icone);
            // Nao aplicamos tint nos vetores; mantemos o roxo da Etapa 88.
            caixaIcone.addView(imagem, new LinearLayout.LayoutParams(dp(27), dp(27)));
        }

        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpTextos = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        cartao.addView(textos, lpTextos);

        TextView titulo = criarTexto(categoria.nome, 15, R.color.mb_purple_dark, true);
        textos.addView(titulo);
        TextView descricao = criarTexto(categoria.descricao, 12, R.color.mb_muted, false);
        descricao.setMaxLines(2);
        textos.addView(descricao);

        LinearLayout totais = new LinearLayout(this);
        totais.setOrientation(LinearLayout.VERTICAL);
        totais.setGravity(Gravity.END);
        LinearLayout.LayoutParams lpTotais = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTotais.leftMargin = dp(8);
        cartao.addView(totais, lpTotais);

        int corValor = mostrandoReceitas ? R.color.mb_green : R.color.mb_red;
        TextView valor = criarTexto(formatarMoeda(categoria.centavos), 14, corValor, true);
        valor.setGravity(Gravity.END);
        valor.setSingleLine(true);
        totais.addView(valor);
        String textoQuantidade = categoria.quantidade == 1 ? "1 transação" :
                categoria.quantidade + " transações";
        TextView quantidade = criarTexto(textoQuantidade, 11, R.color.mb_muted, false);
        quantidade.setGravity(Gravity.END);
        totais.addView(quantidade);

        TextView seta = criarTexto("›", 26, R.color.mb_muted, false);
        LinearLayout.LayoutParams lpSeta = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpSeta.leftMargin = dp(7);
        cartao.addView(seta, lpSeta);

        cartao.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(categoria.nome)
                .setMessage("Total acumulado: " + formatarMoeda(categoria.centavos) +
                        "\n" + textoQuantidade +
                        "\n\nValores de todos os períodos cadastrados.")
                .setPositiveButton("Fechar", null)
                .show());
    }

    private TextView criarTexto(String conteudo, int tamanhoSp, int cor, boolean negrito) {
        TextView texto = new TextView(this);
        texto.setText(conteudo);
        texto.setTextSize(tamanhoSp);
        texto.setTextColor(getColor(cor));
        if (negrito) texto.setTypeface(null, Typeface.BOLD);
        return texto;
    }

    private GradientDrawable fundoArredondado(int cor, int raioDp) {
        GradientDrawable fundo = new GradientDrawable();
        fundo.setColor(cor);
        fundo.setCornerRadius(dp(raioDp));
        return fundo;
    }

    private int dp(float valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    private String formatarMoeda(long centavos) {
        return NumberFormat.getCurrencyInstance(localeBrasil)
                .format(BigDecimal.valueOf(centavos, 2));
    }
}