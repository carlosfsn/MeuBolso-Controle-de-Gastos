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

        // Edicao REAL de nome e e-mail localmente neste dispositivo.
        findViewById(R.id.opcaoEditarPerfil).setOnClickListener(v -> abrirEdicaoPerfil());
        findViewById(R.id.btnEditarAvatar).setOnClickListener(v -> abrirEdicaoPerfil());

        // As seguintes telas serao feitas nas proximas etapas.
        findViewById(R.id.opcaoMetaGastos).setOnClickListener(v -> avisarProximaEtapa("Meta de gastos"));
        findViewById(R.id.opcaoNotificacoes).setOnClickListener(v -> avisarProximaEtapa("Notificações"));
        findViewById(R.id.opcaoSeguranca).setOnClickListener(v -> avisarProximaEtapa("Segurança"));
        findViewById(R.id.opcaoAjuda).setOnClickListener(v -> avisarProximaEtapa("Ajuda"));
        findViewById(R.id.btnSinoPerfil).setOnClickListener(v -> avisarProximaEtapa("Notificações"));
        findViewById(R.id.cardPlanoPerfil).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Versão padrão")
                .setMessage("O MeuBolso está na versão pessoal. Não há assinaturas ou planos pagos integrados ao aplicativo.")
                .setPositiveButton("Entendi", null).show());

        // Nao finge encerrar uma sessao real: explica antes de voltar ao Login.
        findViewById(R.id.btnSairPerfil).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Voltar para o login?")
                        .setMessage("O MeuBolso ainda não possui autenticação real. "
                                + "Você voltará à tela de login e suas movimentações continuarão salvas neste aparelho.")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Continuar", (dialogo, qual) -> {
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        })
                        .show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarDadosPerfil();
    }

    private void atualizarDadosPerfil() {
        SharedPreferences preferencias = getSharedPreferences(ARQUIVO_PREFS, MODE_PRIVATE);
        String nome = preferencias.getString(CHAVE_NOME, "").trim();
        String email = preferencias.getString(CHAVE_EMAIL, "").trim();
        txtNome.setText(nome.isEmpty() ? "Usuário MeuBolso" : nome);
        txtEmail.setText(email.isEmpty() ? "E-mail não configurado" : email);
    }

    private void abrirEdicaoPerfil() {
        SharedPreferences preferencias = getSharedPreferences(ARQUIVO_PREFS, MODE_PRIVATE);

        LinearLayout formulario = new LinearLayout(this);
        formulario.setOrientation(LinearLayout.VERTICAL);
        formulario.setPadding(dp(22), dp(8), dp(22), dp(4));

        TextView tituloNome = new TextView(this);
        tituloNome.setText("Nome");
        tituloNome.setTextColor(getColor(R.color.mb_purple_dark));
        tituloNome.setTextSize(14);
        formulario.addView(tituloNome);

        EditText entradaNome = new EditText(this);
        entradaNome.setSingleLine(true);
        entradaNome.setHint("Seu nome");
        entradaNome.setText(preferencias.getString(CHAVE_NOME, ""));
        entradaNome.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        formulario.addView(entradaNome, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView tituloEmail = new TextView(this);
        tituloEmail.setText("E-mail (opcional)");
        tituloEmail.setTextColor(getColor(R.color.mb_purple_dark));
        tituloEmail.setTextSize(14);
        LinearLayout.LayoutParams espacoEmail = new LinearLayout.LayoutParams(-1, -2);
        espacoEmail.topMargin = dp(12);
        formulario.addView(tituloEmail, espacoEmail);

        EditText entradaEmail = new EditText(this);
        entradaEmail.setSingleLine(true);
        entradaEmail.setHint("voce@exemplo.com");
        entradaEmail.setText(preferencias.getString(CHAVE_EMAIL, ""));
        entradaEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        formulario.addView(entradaEmail, new LinearLayout.LayoutParams(-1, dp(54)));

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Editar perfil")
                .setView(formulario)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null)
                .create();

        dialogo.setOnShowListener(ignorado -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(botao -> {
                    String nome = entradaNome.getText().toString().trim();
                    String email = entradaEmail.getText().toString().trim();

                    if (nome.isEmpty()) {
                        entradaNome.setError("Informe seu nome.");
                        entradaNome.requestFocus();
                        return;
                    }
                    if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        entradaEmail.setError("Informe um e-mail válido.");
                        entradaEmail.requestFocus();
                        return;
                    }
                    boolean salvo = preferencias.edit()
                            .putString(CHAVE_NOME, nome)
                            .putString(CHAVE_EMAIL, email)
                            .commit();
                    if (salvo) {
                        atualizarDadosPerfil();
                        Toast.makeText(this, "Perfil atualizado neste dispositivo!", Toast.LENGTH_SHORT).show();
                        dialogo.dismiss();
                    } else {
                        Toast.makeText(this, "Não foi possível salvar os dados.", Toast.LENGTH_SHORT).show();
                    }
                }));
        dialogo.show();
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