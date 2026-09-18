
package com.carlosfsn.meubolso;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BancoDadosHelper extends SQLiteOpenHelper {

    // Nome e versao do banco de dados
    private static final String NOME_BANCO = "meubolso.db";
    private static final int VERSAO_BANCO = 1;

    // Nome da tabela
    public static final String TABELA_MOVIMENTACOES =
            "movimentacoes";

    // Construtor
    public BancoDadosHelper(Context context) {
        super(context, NOME_BANCO, null, VERSAO_BANCO);
    }

    // Criar tabela ao iniciar o banco pela primeira vez
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
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase banco,
            int versaoAntiga,
            int versaoNova) {

        // Reservado para futuras atualizacoes.
    }

    // SALVAR MOVIMENTACAO
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

    // LISTAR MOVIMENTACOES SALVAS
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
}