
package com.carlosfsn.meubolso;

import android.content.ContentValues;
import android.content.Context;
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
    private static final int VERSAO_BANCO = 2;

    public static final String TABELA_MOVIMENTACOES = "movimentacoes";
    private static final String TABELA_USUARIO = "usuario_local";

    public BancoDadosHelper(Context context) {
        super(context, NOME_BANCO, null, VERSAO_BANCO);
    }

    @Override
    public void onCreate(SQLiteDatabase banco) {

        String sql = "CREATE TABLE " + TABELA_MOVIMENTACOES
                + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "tipo TEXT NOT NULL, "
                + "valor_centavos INTEGER NOT NULL, "
                + "categoria TEXT NOT NULL, "
                + "data TEXT NOT NULL, "
                + "descricao TEXT NOT NULL"
                + ")";

        banco.execSQL(sql);

        criarTabelaUsuario(banco);
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase banco,
            int versaoAntiga,
            int versaoNova) {

        // Preservar as movimentações durante a atualização
        if (versaoAntiga < 2) {
            criarTabelaUsuario(banco);
        }
    }

    // Criar a tabela do usuário local
    private void criarTabelaUsuario(SQLiteDatabase banco) {

        String sql = "CREATE TABLE IF NOT EXISTS "
                + TABELA_USUARIO
                + " ("
                + "id INTEGER PRIMARY KEY CHECK (id = 1), "
                + "nome TEXT NOT NULL, "
                + "email TEXT NOT NULL COLLATE NOCASE UNIQUE, "
                + "senha_hash TEXT NOT NULL, "
                + "salt TEXT NOT NULL"
                + ")";

        banco.execSQL(sql);
    }

    // Salvar uma receita ou despesa
    public long salvarMovimentacao(
            String tipo,
            long valorCentavos,
            String categoria,
            String data,
            String descricao) {

        SQLiteDatabase banco = getWritableDatabase();

        ContentValues valores = new ContentValues();

        valores.put("tipo", tipo);
        valores.put("valor_centavos", valorCentavos);
        valores.put("categoria", categoria);
        valores.put("data", data);
        valores.put("descricao", descricao);

        return banco.insert(
                TABELA_MOVIMENTACOES,
                null,
                valores
        );
    }

    // Listar todas as movimentações salvas
    public Cursor listarMovimentacoes() {

        SQLiteDatabase banco = getReadableDatabase();

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
                null,
                null,
                null,
                null,
                "id DESC"
        );
    }

    // Atualizar uma movimentação existente
    public int atualizarMovimentacao(
            long id,
            String tipo,
            long valorCentavos,
            String categoria,
            String data,
            String descricao) {

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
                "id = ?",
                new String[]{String.valueOf(id)}
        );
    }

    // Excluir uma movimentação pelo ID
    public int excluirMovimentacao(long id) {

        SQLiteDatabase banco = getWritableDatabase();

        return banco.delete(
                TABELA_MOVIMENTACOES,
                "id = ?",
                new String[]{String.valueOf(id)}
        );
    }

    // Gerar hash para proteger a senha
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

    // Cadastrar um usuário no SQLite
    public boolean cadastrarUsuario(
            String nome,
            String email,
            String senha) {

        SQLiteDatabase banco = getWritableDatabase();

        // Gerar salt aleatório para a senha
        byte[] salt = new byte[16];

        new SecureRandom().nextBytes(salt);

        String senhaHash = gerarHashSenha(
                senha,
                salt
        );

        String saltTexto = Base64.encodeToString(
                salt,
                Base64.NO_WRAP
        );

        ContentValues valores = new ContentValues();

        valores.put("id", 1);
        valores.put("nome", nome.trim());

        valores.put(
                "email",
                email.trim().toLowerCase(Locale.ROOT)
        );

        valores.put("senha_hash", senhaHash);
        valores.put("salt", saltTexto);

        // Impedir a substituição do usuário existente
        long resultado = banco.insertWithOnConflict(
                TABELA_USUARIO,
                null,
                valores,
                SQLiteDatabase.CONFLICT_IGNORE
        );

        return resultado != -1;
    }

    // Validar e-mail e senha do usuário cadastrado
    public boolean validarLogin(
            String email,
            String senha) {

        SQLiteDatabase banco = getReadableDatabase();

        String emailNormalizado = email.trim()
                .toLowerCase(Locale.ROOT);

        try (Cursor cursor = banco.query(
                TABELA_USUARIO,
                new String[]{"senha_hash", "salt"},
                "id = ? AND email = ?",
                new String[]{"1", emailNormalizado},
                null,
                null,
                null
        )) {

            // Verificar se existe um usuário com esse e-mail
            if (!cursor.moveToFirst()) {
                return false;
            }

            String hashSalvo = cursor.getString(
                    cursor.getColumnIndexOrThrow("senha_hash")
            );

            String saltSalvo = cursor.getString(
                    cursor.getColumnIndexOrThrow("salt")
            );

            // Recuperar o salt utilizado no cadastro
            byte[] salt = Base64.decode(
                    saltSalvo,
                    Base64.NO_WRAP
            );

            // Gerar o hash da senha informada
            String hashDigitado = gerarHashSenha(
                    senha,
                    salt
            );

            byte[] hashOriginal = Base64.decode(
                    hashSalvo,
                    Base64.NO_WRAP
            );

            byte[] hashInformado = Base64.decode(
                    hashDigitado,
                    Base64.NO_WRAP
            );

            // Comparar os hashes
            return MessageDigest.isEqual(
                    hashOriginal,
                    hashInformado
            );
        }
    }

    // Recuperar o nome do usuário cadastrado
    public String obterNomeUsuario() {

        SQLiteDatabase banco = getReadableDatabase();

        try (Cursor cursor = banco.query(
                TABELA_USUARIO,
                new String[]{"nome"},
                "id = ?",
                new String[]{"1"},
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

    // Recuperar o e-mail do usuário cadastrado
    public String obterEmailUsuario() {

        SQLiteDatabase banco = getReadableDatabase();

        try (Cursor cursor = banco.query(
                TABELA_USUARIO,
                new String[]{"email"},
                "id = ?",
                new String[]{"1"},
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

    // Atualizar nome e e-mail do usuário cadastrado
    public boolean atualizarDadosUsuario(
            String nome,
            String email) {

        if (nome == null || nome.trim().isEmpty()
                || email == null || email.trim().isEmpty()) {
            return false;
        }

        SQLiteDatabase banco = getWritableDatabase();

        ContentValues valores = new ContentValues();

        valores.put("nome", nome.trim());

        valores.put(
                "email",
                email.trim().toLowerCase(java.util.Locale.ROOT)
        );

        int linhasAtualizadas = banco.update(
                TABELA_USUARIO,
                valores,
                "id = ?",
                new String[]{"1"}
        );

        return linhasAtualizadas > 0;
    }

    // Alterar a senha do usuário cadastrado
    public boolean alterarSenhaUsuario(
            String senhaAtual,
            String novaSenha) {

        if (senhaAtual == null || novaSenha == null
                || novaSenha.length() < 8) {
            return false;
        }

        SQLiteDatabase banco = getWritableDatabase();

        String hashSalvo;
        String saltSalvo;

        // Recuperar a senha protegida do usuário
        try (Cursor cursor = banco.query(
                TABELA_USUARIO,
                new String[]{"senha_hash", "salt"},
                "id = ?",
                new String[]{"1"},
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

        // Verificar se a senha atual está correta
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

        // Criar um novo salt para a nova senha
        byte[] novoSalt = new byte[16];
        new SecureRandom().nextBytes(novoSalt);

        String novoHash = gerarHashSenha(
                novaSenha,
                novoSalt
        );

        ContentValues valores = new ContentValues();

        valores.put("senha_hash", novoHash);

        valores.put(
                "salt",
                Base64.encodeToString(
                        novoSalt,
                        Base64.NO_WRAP
                )
        );

        // Atualizar somente a senha do usuário
        int resultado = banco.update(
                TABELA_USUARIO,
                valores,
                "id = ?",
                new String[]{"1"}
        );

        return resultado > 0;
    }
}
