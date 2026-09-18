package com.carlosfsn.meubolso;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InicioActivity extends AppCompatActivity {

    private final Locale localeBR = new Locale("pt", "BR");
    private GraficoMensalView graficoMensal;

    private static class Movimentacao {
        long id;
        long valorCentavos;
        long dataOrdenacao;
        int mes;
        int ano;
        String tipo;
        String categoria;
        String data;
        boolean temDataValida;

        boolean receita() {
            return "Receita".equalsIgnoreCase(tipo);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inicio);

        // Mantem o conteudo respeitando as barras do Android.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (tela, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            tela.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        // Preenche a area do grafico com um anel que aceita valores reais.
        FrameLayout areaGrafico = findViewById(R.id.graficoResumo);
        graficoMensal = new GraficoMensalView(this);
        areaGrafico.addView(graficoMensal, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Navegacao anterior preservada.
        findViewById(R.id.navNova).setOnClickListener(botao ->
                startActivity(new Intent(InicioActivity.this, NovaMovimentacaoActivity.class))
        );
        findViewById(R.id.navHistorico).setOnClickListener(botao ->
                startActivity(new Intent(InicioActivity.this, HistoricoActivity.class))
        );
        // ETAPA 89: acesso a tela Categorias.
        findViewById(R.id.navCategorias).setOnClickListener(botao ->
                startActivity(new Intent(InicioActivity.this, CategoriasActivity.class))
        );
        // ETAPA 91: abrir Perfil na barra inferior.
        findViewById(R.id.navPerfil).setOnClickListener(botao ->
                startActivity(new Intent(InicioActivity.this, PerfilActivity.class))
        );
        findViewById(R.id.txtVerTodas).setOnClickListener(botao ->
                startActivity(new Intent(InicioActivity.this, HistoricoActivity.class))
        );
    }

    // Atualiza sempre que o Dashboard reaparece (por exemplo, depois de Salvar).
    @Override
    protected void onResume() {
        super.onResume();
        carregarDadosDashboard();
    }

    private void carregarDadosDashboard() {
        List<Movimentacao> movimentacoes = new ArrayList<>();
        Calendar hoje = Calendar.getInstance();
        int mesAtual = hoje.get(Calendar.MONTH);
        int anoAtual = hoje.get(Calendar.YEAR);

        long receitasTotais = 0;
        long despesasTotais = 0;
        long receitasMes = 0;
        long despesasMes = 0;

        BancoDadosHelper helper = new BancoDadosHelper(this);
        try (Cursor cursor = helper.listarMovimentacoes()) {
            SimpleDateFormat formatoData = new SimpleDateFormat("dd/MM/yyyy", localeBR);
            formatoData.setLenient(false);

            while (cursor.moveToNext()) {
                Movimentacao item = new Movimentacao();
                item.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                item.tipo = cursor.getString(cursor.getColumnIndexOrThrow("tipo"));
                item.valorCentavos = cursor.getLong(cursor.getColumnIndexOrThrow("valor_centavos"));
                item.categoria = cursor.getString(cursor.getColumnIndexOrThrow("categoria"));
                item.data = cursor.getString(cursor.getColumnIndexOrThrow("data"));

                // Valores armazenados em centavos inteiros, sem float/double.
                if (item.receita()) {
                    receitasTotais += item.valorCentavos;
                } else {
                    despesasTotais += item.valorCentavos;
                }

                // As datas sao gravadas como DD/MM/AAAA no app.
                try {
                    Date dataConvertida = formatoData.parse(item.data);
                    if (dataConvertida != null) {
                        Calendar calendario = Calendar.getInstance();
                        calendario.setTime(dataConvertida);
                        item.mes = calendario.get(Calendar.MONTH);
                        item.ano = calendario.get(Calendar.YEAR);
                        item.dataOrdenacao = dataConvertida.getTime();
                        item.temDataValida = true;
                    }
                } catch (ParseException | NullPointerException ignorado) {
                    // Registro permanece na lista, mesmo com data invalida.
                }

                if (item.temDataValida && item.mes == mesAtual && item.ano == anoAtual) {
                    if (item.receita()) {
                        receitasMes += item.valorCentavos;
                    } else {
                        despesasMes += item.valorCentavos;
                    }
                }

                movimentacoes.add(item);
            }
        } catch (Exception erro) {
            Toast.makeText(this, "Não foi possível atualizar o Dashboard.",
                    Toast.LENGTH_LONG).show();
            return;
        } finally {
            helper.close();
        }

        // Saldo acumulado considera todas as movimentacoes cadastradas.
        long saldoTotal = receitasTotais - despesasTotais;
        // Receitas e despesas sao exibidas para o mes atual.
        long saldoMes = receitasMes - despesasMes;

        ((TextView) findViewById(R.id.txtValorSaldo)).setText(formatarMoeda(saldoTotal));
        ((TextView) findViewById(R.id.txtValorReceitas)).setText(formatarMoeda(receitasMes));
        ((TextView) findViewById(R.id.txtValorDespesas)).setText(formatarMoeda(despesasMes));
        ((TextView) findViewById(R.id.txtResumoDespesas)).setText(formatarMoeda(despesasMes));
        ((TextView) findViewById(R.id.txtResumoSaldo)).setText(formatarMoeda(saldoMes));

        graficoMensal.definirValores(receitasMes, despesasMes);
        mostrarUltimasMovimentacoes(movimentacoes);
    }

    private void mostrarUltimasMovimentacoes(List<Movimentacao> registros) {
        LinearLayout lista = findViewById(R.id.listaUltimasMovimentacoes);
        lista.removeAllViews();

        // Ordenacao decrescente por data; em dias iguais, mais recente cadastro primeiro.
        Collections.sort(registros, (primeiro, segundo) -> {
            int porData = Long.compare(segundo.dataOrdenacao, primeiro.dataOrdenacao);
            return porData != 0 ? porData : Long.compare(segundo.id, primeiro.id);
        });

        if (registros.isEmpty()) {
            TextView mensagem = new TextView(this);
            mensagem.setText("Você ainda não cadastrou movimentações.");
            mensagem.setTextColor(getColor(R.color.mb_muted));
            mensagem.setTextSize(14);
            mensagem.setGravity(Gravity.CENTER);
            mensagem.setPadding(dp(4), dp(18), dp(4), dp(10));
            lista.addView(mensagem);
            return;
        }

        int quantidade = Math.min(5, registros.size());
        for (int indice = 0; indice < quantidade; indice++) {
            if (indice > 0) {
                View divisoria = new View(this);
                divisoria.setBackgroundColor(Color.rgb(241, 237, 244));
                LinearLayout.LayoutParams parametros = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
                parametros.topMargin = dp(15);
                parametros.bottomMargin = dp(15);
                lista.addView(divisoria, parametros);
            }
            lista.addView(criarLinhaMovimentacao(registros.get(indice)));
        }
    }

    private View criarLinhaMovimentacao(Movimentacao item) {
        boolean receita = item.receita();

        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        linha.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView icone = new TextView(this);
        icone.setText(receita ? "↑" : "↓");
        icone.setTextSize(24);
        icone.setTypeface(null, android.graphics.Typeface.BOLD);
        icone.setGravity(Gravity.CENTER);
        icone.setTextColor(getColor(receita ? R.color.mb_green : R.color.mb_red));
        icone.setBackgroundResource(receita ? R.drawable.bg_card_receitas : R.drawable.bg_card_despesas);
        linha.addView(icone, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout informacoes = new LinearLayout(this);
        informacoes.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams parametrosTexto = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        parametrosTexto.leftMargin = dp(12);
        linha.addView(informacoes, parametrosTexto);

        TextView categoria = new TextView(this);
        categoria.setText(item.categoria == null || item.categoria.trim().isEmpty()
                ? "Sem categoria" : item.categoria);
        categoria.setTextSize(16);
        categoria.setTextColor(getColor(R.color.mb_ink));
        categoria.setTypeface(null, android.graphics.Typeface.BOLD);
        categoria.setSingleLine(true);
        categoria.setEllipsize(TextUtils.TruncateAt.END);
        informacoes.addView(categoria);

        TextView data = new TextView(this);
        data.setText(item.data == null ? "" : item.data);
        data.setTextSize(12);
        data.setTextColor(getColor(R.color.mb_muted));
        LinearLayout.LayoutParams parametrosData = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        parametrosData.topMargin = dp(4);
        informacoes.addView(data, parametrosData);

        TextView valor = new TextView(this);
        valor.setText((receita ? "+ " : "- ") + formatarMoeda(item.valorCentavos));
        valor.setTextSize(14);
        valor.setTypeface(null, android.graphics.Typeface.BOLD);
        valor.setTextColor(getColor(receita ? R.color.mb_green : R.color.mb_red));
        valor.setSingleLine(true);
        linha.addView(valor);

        // Tocar na linha abre o Historico, onde ficam os detalhes completos.
        linha.setOnClickListener(v ->
                startActivity(new Intent(InicioActivity.this, HistoricoActivity.class)));
        return linha;
    }

    private String formatarMoeda(long centavos) {
        NumberFormat formato = NumberFormat.getNumberInstance(localeBR);
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        BigDecimal valorAbsoluto = BigDecimal.valueOf(centavos, 2).abs();
        return (centavos < 0 ? "- R$ " : "R$ ") + formato.format(valorAbsoluto);
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    // Grafico circular dinamico. Permanece nesta classe para nao criar outro arquivo.
    private static class GraficoMensalView extends View {
        private long receitas;
        private long despesas;
        private final Paint tinta = new Paint(Paint.ANTI_ALIAS_FLAG);

        GraficoMensalView(android.content.Context contexto) {
            super(contexto);
        }

        void definirValores(long novasReceitas, long novasDespesas) {
            receitas = novasReceitas;
            despesas = novasDespesas;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float densidade = getResources().getDisplayMetrics().density;
            float escalaTexto = getResources().getDisplayMetrics().scaledDensity;
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float raio = Math.max(0f, Math.min(getWidth(), getHeight()) / 2f - 17f * densidade);
            float espessura = 12f * densidade;
            RectF circulo = new RectF(cx - raio, cy - raio, cx + raio, cy + raio);

            tinta.setStyle(Paint.Style.STROKE);
            tinta.setStrokeWidth(espessura);
            tinta.setStrokeCap(Paint.Cap.ROUND);
            tinta.setColor(Color.rgb(112, 96, 215));
            canvas.drawArc(circulo, 0, 360, false, tinta);

            double proporcao = receitas > 0 ? (double) despesas / receitas
                    : (despesas > 0 ? 1d : 0d);
            float graus = (float) (360d * Math.min(1d, Math.max(0d, proporcao)));
            if (graus > 0) {
                tinta.setColor(Color.rgb(233, 73, 96));
                canvas.drawArc(circulo, -90f, graus, false, tinta);
            }

            tinta.setStyle(Paint.Style.FILL);
            tinta.setColor(Color.rgb(75, 60, 169));
            tinta.setTextAlign(Paint.Align.CENTER);
            tinta.setTypeface(android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
            tinta.setTextSize(21f * escalaTexto);
            String percentual = receitas > 0
                    ? Math.round(100d * despesas / receitas) + "%"
                    : (despesas > 0 ? "—" : "0%");
            canvas.drawText(percentual, cx, cy - 1f * densidade, tinta);

            tinta.setTypeface(android.graphics.Typeface.DEFAULT);
            tinta.setTextSize(10f * escalaTexto);
            tinta.setColor(Color.rgb(126, 109, 141));
            canvas.drawText(receitas == 0 && despesas > 0 ? "sem receitas" : "despesas",
                    cx, cy + 15f * densidade, tinta);
        }
    }
}