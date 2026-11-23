
package com.carlosfsn.meubolso;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class BancoDadosHelper extends SQLiteOpenHelper {

    private static final String NOME_BANCO = "meubolso.db";
    private static final int VERSAO_BANCO = 4;

    public static final String TABELA_MOVIMENTACOES = "movimentacoes";
    private static final String TABELA_USUARIO = "usuario_local";

    private final Context contexto;

    public BancoDadosHelper(Context context) {
        super(context, NOME_BANCO, null, VERSAO_BANCO);
        contexto = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase banco) {

        banco.execSQL(
                "CREATE TABLE " + TABELA_USUARIO + " ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "nome TEXT NOT NULL, "
                        + "email TEXT NOT NULL COLLATE NOCASE UNIQUE, "
                        + "senha_hash TEXT NOT NULL, "
                        + "salt TEXT NOT NULL, "
                        + "google_uid TEXT UNIQUE"
                        + ")"
        );

        banco.execSQL(
                "CREATE TABLE " + TABELA_MOVIMENTACOES + " ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "tipo TEXT NOT NULL, "
                        + "valor_centavos INTEGER NOT NULL, "
                        + "categoria TEXT NOT NULL, "
                        + "data TEXT NOT NULL, "
                        + "descricao TEXT NOT NULL, "
                        + "usuario_id INTEGER NOT NULL"
                        + ")"
        );

        banco.execSQL(
                "CREATE INDEX idx_movimentacoes_usuario "
                        + "ON " + TABELA_MOVIMENTACOES + "(usuario_id)"
        );
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase banco,
            int versaoAntiga,
            int versaoNova) {

        if (versaoAntiga < 4) {

            banco.execSQL(
                    "CREATE TABLE usuario_local_novo ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "nome TEXT NOT NULL, "
                            + "email TEXT NOT NULL COLLATE NOCASE UNIQUE, "
                            + "senha_hash TEXT NOT NULL, "
                            + "salt TEXT NOT NULL, "
                            + "google_uid TEXT UNIQUE"
                            + ")"
            );

            if (tabelaExiste(banco, TABELA_USUARIO)) {

                boolean possuiGoogle = colunaExiste(
                        banco,
                        TABELA_USUARIO,
                        "google_uid"
                );

                String colunaGoogle = possuiGoogle
                        ? "google_uid"
                        : "NULL";

                banco.execSQL(
                        "INSERT INTO usuario_local_novo "
                                + "(id, nome, email, senha_hash, salt, google_uid) "
                                + "SELECT id, nome, email, senha_hash, salt, "
                                + colunaGoogle
                                + " FROM " + TABELA_USUARIO
                );

                banco.execSQL(
                        "DROP TABLE " + TABELA_USUARIO
                );
            }

            banco.execSQL(
                    "ALTER TABLE usuario_local_novo "
                            + "RENAME TO " + TABELA_USUARIO
            );

            // Os registros antigos pertencem ao usuário original
            if (!colunaExiste(
                    banco,
                    TABELA_MOVIMENTACOES,
                    "usuario_id")) {

                banco.execSQL(
                        "ALTER TABLE " + TABELA_MOVIMENTACOES
                                + " ADD COLUMN usuario_id "
                                + "INTEGER NOT NULL DEFAULT 1"
                );
            }

            banco.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_movimentacoes_usuario "
                            + "ON " + TABELA_MOVIMENTACOES + "(usuario_id)"
            );
        }
    }

    private boolean tabelaExiste(
            SQLiteDatabase banco,
            String tabela) {

        try (Cursor cursor = banco.rawQuery(
                "SELECT name FROM sqlite_master "
                        + "WHERE type = 'table' AND name = ?",
                new String[]{tabela}
        )) {
            return cursor.moveToFirst();
        }
    }

    private boolean colunaExiste(
            SQLiteDatabase banco,
            String tabela,
            String coluna) {

        try (Cursor cursor = banco.rawQuery(
                "PRAGMA table_info(" + tabela + ")",
                null
        )) {

            while (cursor.moveToNext()) {

                String nome = cursor.getString(
                        cursor.getColumnIndexOrThrow("name")
                );

                if (coluna.equalsIgnoreCase(nome)) {
                    return true;
                }
            }
        }

        return false;
    }

    // Recuperar o usuário da sessão atual
    private long obterUsuarioSessao() {

        SharedPreferences preferencias =
                contexto.getSharedPreferences(
                        "meubolso_sessao",
                        Context.MODE_PRIVATE
                );

        if (!preferencias.getBoolean("autenticado", false)) {
            return -1L;
        }

        // Compatibilidade com a sessão da conta original
        long id = preferencias.getLong("usuario_id", 1L);

        return id > 0 ? id : -1L;
    }

    // Definir o proprietário da sessão após a autenticação
    public void definirUsuarioSessao(long usuarioId) {

        if (usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "Usuário inválido."
            );
        }

        contexto.getSharedPreferences(
                        "meubolso_sessao",
                        Context.MODE_PRIVATE
                ).edit()
                .putLong("usuario_id", usuarioId)
                .apply();
    }

    // Salvar movimentação para o usuário conectado
    public long salvarMovimentacao(
            String tipo,
            long valorCentavos,
            String categoria,
            String data,
            String descricao) {

        long usuarioId = obterUsuarioSessao();

        if (usuarioId <= 0) {
            return -1L;
        }

        SQLiteDatabase banco = getWritableDatabase();

        ContentValues valores = new ContentValues();

        valores.put("tipo", tipo);
        valores.put("valor_centavos", valorCentavos);
        valores.put("categoria", categoria);
        valores.put("data", data);
        valores.put("descricao", descricao);
        valores.put("usuario_id", usuarioId);

        return banco.insert(
                TABELA_MOVIMENTACOES,
                null,
                valores
        );
    }

    // Listar somente movimentações do usuário conectado
    public Cursor listarMovimentacoes() {

        SQLiteDatabase banco = getReadableDatabase();

        long usuarioId = obterUsuarioSessao();

        return banco.query(
                TABELA_MOVIMENTACOES,
                new String[]{
                        "id",
                        "tipo",
                        "valor_centavos",
                        "categoria",
                        "data",
                        "descricao"
                },
                "usuario_id = ?",
                new String[]{String.valueOf(usuarioId)},
                null,
                null,
                "id DESC"
        );
    }

    // Editar somente uma movimentação do usuário conectado
    public int atualizarMovimentacao(
            long id,
            String tipo,
            long valorCentavos,
            String categoria,
            String data,
            String descricao) {

        long usuarioId = obterUsuarioSessao();

        if (usuarioId <= 0) {
            return 0;
        }

        SQLiteDatabase banco = getWritableDatabase();

        ContentValues valores = new ContentValues();

        valores.put("tipo", tipo);
        valores.put("valor_centavos", valorCentavos);
        valores.put("categoria", categoria);
        valores.put("data", data);
        valores.put("descricao", descricao);

        return banco.update(
                TABELA_MOVIMENTACOES,
                valores,
                "id = ? AND usuario_id = ?",
                new String[]{
                        String.valueOf(id),
                        String.valueOf(usuarioId)
                }
        );
    }

    // Excluir somente uma movimentação do usuário conectado
    public int excluirMovimentacao(long id) {

        long usuarioId = obterUsuarioSessao();

        if (usuarioId <= 0) {
            return 0;
        }

        SQLiteDatabase banco = getWritableDatabase();

        return banco.delete(
                TABELA_MOVIMENTACOES,
                "id = ? AND usuario_id = ?",
                new String[]{
                        String.valueOf(id),
                        String.valueOf(usuarioId)
                }
        );
    }

    // Proteger a senha com PBKDF2 e salt aleatório
    private String gerarHashSenha(
            String senha,
            byte[] salt) {

        PBEKeySpec especificacao = new PBEKeySpec(
                senha.toCharArray(),
                salt,
                210000,
                256
        );

        try {

            byte[] hash = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA1")
                    .generateSecret(especificacao)
                    .getEncoded();

            return Base64.encodeToString(
                    hash,
                    Base64.NO_WRAP
            );

        } catch (GeneralSecurityException erro) {

            throw new IllegalStateException(
                    "Não foi possível proteger a senha.",
                    erro
            );

        } finally {
            especificacao.clearPassword();
        }
    }

    // Cadastrar usuário com e-mail e senha
    public boolean cadastrarUsuario(
            String nome,
            String email,
            String senha) {

        return inserirUsuario(
                nome,
                email,
                senha,
                null
        ) > 0;
    }

    // Cadastrar novo usuário autenticado pelo Google
    public long cadastrarUsuarioGoogle(
            String nome,
            String email,
            String senha,
            String googleUid) {

        if (googleUid == null || googleUid.trim().isEmpty()) {
            return -1L;
        }

        return inserirUsuario(
                nome,
                email,
                senha,
                googleUid
        );
    }

    private long inserirUsuario(
            String nome,
            String email,
            String senha,
            String googleUid) {

        if (nome == null || nome.trim().isEmpty()
                || email == null || email.trim().isEmpty()
                || senha == null || senha.length() < 8) {
            return -1L;
        }

        SQLiteDatabase banco = getWritableDatabase();

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        ContentValues valores = new ContentValues();

        valores.put("nome", nome.trim());

        valores.put(
                "email",
                email.trim().toLowerCase(Locale.ROOT)
        );

        valores.put(
                "senha_hash",
                gerarHashSenha(senha, salt)
        );

        valores.put(
                "salt",
                Base64.encodeToString(
                        salt,
                        Base64.NO_WRAP
                )
        );

        if (googleUid != null) {
            valores.put("google_uid", googleUid);
        }

        return banco.insertWithOnConflict(
                TABELA_USUARIO,
                null,
                valores,
                SQLiteDatabase.CONFLICT_IGNORE
        );
    }

    // Validar o login de qualquer usuário cadastrado
    public boolean validarLogin(
            String email,
            String senha) {

        if (email == null || senha == null) {
            return false;
        }

        SQLiteDatabase banco = getReadableDatabase();

        try (Cursor cursor = banco.query(
                TABELA_USUARIO,
                new String[]{"id", "senha_hash", "salt"},
                "email = ?",
                new String[]{
                        email.trim().toLowerCase(Locale.ROOT)
                },
                null,
                null,
                null
        )) {

            if (!cursor.moveToFirst()) {
                return false;
            }

            long usuarioId = cursor.getLong(
                    cursor.getColumnIndexOrThrow("id")
            );

            String hashSalvo = cursor.getString(
                    cursor.getColumnIndexOrThrow("senha_hash")
            );

            String saltSalvo = cursor.getString(
                    cursor.getColumnIndexOrThrow("salt")
            );

            byte[] salt = Base64.decode(
                    saltSalvo,
                    Base64.NO_WRAP
            );

            String hashDigitado = gerarHashSenha(
                    senha,
                    salt
            );

            boolean valido = MessageDigest.isEqual(
                    Base64.decode(hashSalvo, Base64.NO_WRAP),
                    Base64.decode(hashDigitado, Base64.NO_WRAP)
            );

            if (valido) {
                definirUsuarioSessao(usuarioId);
            }

            return valido;
        }
    }

    // Localizar o usuário pela conta Google
    public long obterIdUsuarioGoogle(String googleUid) {

        if (googleUid == null || googleUid.trim().isEmpty()) {
            return -1L;
        }

        SQLiteDatabase banco = getReadableDatabase();

        try (Cursor cursor = banco.query(
                TABELA_USUARIO,
                new String[]{"id"},
                "google_uid = ?",
                new String[]{googleUid},
                null,
                null,
                null
        )) {

            if (cursor.moveToFirst()) {
                return cursor.getLong(
                        cursor.getColumnIndexOrThrow("id")
                );
            }
        }

        return -1L;
    }

    public boolean contaGoogleVinculada(String googleUid) {
        return obterIdUsuarioGoogle(googleUid) > 0;
    }

    // Vincular Google à conta local original mediante senha
    public boolean vincularContaGoogle(
            String googleUid,
            String senhaLocal) {

        if (googleUid == null || googleUid.trim().isEmpty()
                || senhaLocal == null || senhaLocal.isEmpty()) {
            return false;
        }

        SQLiteDatabase banco = getWritableDatabase();

        String emailOriginal;

        try (Cursor cursor = banco.query(
                TABELA_USUARIO,
                new String[]{"email"},
                "id = ? AND google_uid IS NULL",
                new String[]{"1"},
                null,
                null,
                null
        )) {

            if (!cursor.moveToFirst()) {
                return false;
            }

            emailOriginal = cursor.getString(
                    cursor.getColumnIndexOrThrow("email")
            );
        }

        if (!validarLogin(emailOriginal, senhaLocal)) {
            return false;
        }

        ContentValues valores = new ContentValues();
        valores.put("google_uid", googleUid);

        int resultado = banco.update(
                TABELA_USUARIO,
                valores,
                "id = ? AND google_uid IS NULL",
                new String[]{"1"}
        );

        return resultado > 0;
    }

    // Recuperar o nome do usuário conectado
    public String obterNomeUsuario() {

        long usuarioId = obterUsuarioSessao();

        SQLiteDatabase banco = getReadableDatabase();

        try (Cursor cursor = banco.query(
                TABELA_USUARIO,
                new String[]{"nome"},
                "id = ?",
                new String[]{String.valueOf(usuarioId)},
                null,
                null,
                null
        )) {

            if (cursor.moveToFirst()) {

                String nome = cursor.getString(
                        cursor.getColumnIndexOrThrow("nome")
                );

                if (nome != null && !nome.trim().isEmpty()) {
                    return nome.trim();
                }
            }
        }

        return "Usuário";
    }

    // Recuperar o e-mail do usuário conectado
    public String obterEmailUsuario() {

        long usuarioId = obterUsuarioSessao();

        SQLiteDatabase banco = getReadableDatabase();

        try (Cursor cursor = banco.query(
                TABELA_USUARIO,
                new String[]{"email"},
                "id = ?",
                new String[]{String.valueOf(usuarioId)},
                null,
                null,
                null
        )) {

            if (cursor.moveToFirst()) {
                return cursor.getString(
                        cursor.getColumnIndexOrThrow("email")
                );
            }
        }

        return "";
    }

    // Editar os dados do usuário conectado
    public boolean atualizarDadosUsuario(
            String nome,
            String email) {

        long usuarioId = obterUsuarioSessao();

        if (usuarioId <= 0
                || nome == null || nome.trim().isEmpty()
                || email == null || email.trim().isEmpty()) {
            return false;
        }

        ContentValues valores = new ContentValues();

        valores.put("nome", nome.trim());

        valores.put(
                "email",
                email.trim().toLowerCase(Locale.ROOT)
        );

        int resultado = getWritableDatabase().update(
                TABELA_USUARIO,
                valores,
                "id = ?",
                new String[]{String.valueOf(usuarioId)}
        );

        return resultado > 0;
    }

    // Alterar a senha do usuário conectado
    public boolean alterarSenhaUsuario(
            String senhaAtual,
            String novaSenha) {

        long usuarioId = obterUsuarioSessao();

        if (usuarioId <= 0
                || senhaAtual == null
                || novaSenha == null
                || novaSenha.length() < 8) {
            return false;
        }

        SQLiteDatabase banco = getWritableDatabase();

        String hashSalvo;
        String saltSalvo;

        try (Cursor cursor = banco.query(
                TABELA_USUARIO,
                new String[]{"senha_hash", "salt"},
                "id = ?",
                new String[]{String.valueOf(usuarioId)},
                null,
                null,
                null
        )) {

            if (!cursor.moveToFirst()) {
                return false;
            }

            hashSalvo = cursor.getString(
                    cursor.getColumnIndexOrThrow("senha_hash")
            );

            saltSalvo = cursor.getString(
                    cursor.getColumnIndexOrThrow("salt")
            );
        }

        byte[] saltAtual = Base64.decode(
                saltSalvo,
                Base64.NO_WRAP
        );

        String hashDigitado = gerarHashSenha(
                senhaAtual,
                saltAtual
        );

        boolean senhaCorreta = MessageDigest.isEqual(
                Base64.decode(hashSalvo, Base64.NO_WRAP),
                Base64.decode(hashDigitado, Base64.NO_WRAP)
        );

        if (!senhaCorreta) {
            return false;
        }

        byte[] novoSalt = new byte[16];
        new SecureRandom().nextBytes(novoSalt);

        ContentValues valores = new ContentValues();

        valores.put(
                "senha_hash",
                gerarHashSenha(novaSenha, novoSalt)
        );

        valores.put(
                "salt",
                Base64.encodeToString(
                        novoSalt,
                        Base64.NO_WRAP
                )
        );

        int resultado = banco.update(
                TABELA_USUARIO,
                valores,
                "id = ?",
                new String[]{String.valueOf(usuarioId)}
        );

        return resultado > 0;
    }
}