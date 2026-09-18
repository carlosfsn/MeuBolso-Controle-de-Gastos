
package com.carlosfsn.meubolso;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class GraficoResumoView extends View {

    public GraficoResumoView(
            Context context,
            AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float densidade =
                getResources().getDisplayMetrics().density;

        float escalaTexto =
                getResources().getDisplayMetrics().scaledDensity;

        float centroX = getWidth() / 2f;
        float centroY = getHeight() / 2f;

        float espessura = 14 * densidade;

        float raio = Math.min(
                getWidth(), getHeight()
        ) / 2f - espessura;

        if (raio <= 0) return;

        RectF area = new RectF(
                centroX - raio,
                centroY - raio,
                centroX + raio,
                centroY + raio
        );

        // Parte roxa: circulo completo
        Paint pinturaSaldo = new Paint(
                Paint.ANTI_ALIAS_FLAG
        );

        pinturaSaldo.setColor(
                Color.parseColor("#7160D7")
        );
        pinturaSaldo.setStyle(Paint.Style.STROKE);
        pinturaSaldo.setStrokeWidth(espessura);

        canvas.drawArc(
                area, -90, 360, false, pinturaSaldo
        );

        // Parte vermelha: percentual de despesas
        Paint pinturaDespesas = new Paint(
                Paint.ANTI_ALIAS_FLAG
        );

        pinturaDespesas.setColor(
                Color.parseColor("#E94960")
        );
        pinturaDespesas.setStyle(Paint.Style.STROKE);
        pinturaDespesas.setStrokeWidth(espessura);

        float percentual = 1350f / 3200f;

        canvas.drawArc(
                area,
                -90,
                360 * percentual,
                false,
                pinturaDespesas
        );

        // Texto central: 42%
        Paint textoPercentual = new Paint(
                Paint.ANTI_ALIAS_FLAG
        );

        textoPercentual.setColor(
                Color.parseColor("#4B3CA9")
        );
        textoPercentual.setTextSize(25 * escalaTexto);
        textoPercentual.setFakeBoldText(true);
        textoPercentual.setTextAlign(Paint.Align.CENTER);

        canvas.drawText(
                "42%",
                centroX,
                centroY,
                textoPercentual
        );

        // Texto abaixo do percentual
        Paint textoDescricao = new Paint(
                Paint.ANTI_ALIAS_FLAG
        );

        textoDescricao.setColor(
                Color.parseColor("#756C8D")
        );
        textoDescricao.setTextSize(11 * escalaTexto);
        textoDescricao.setTextAlign(Paint.Align.CENTER);

        canvas.drawText(
                "despesas",
                centroX,
                centroY + 20 * densidade,
                textoDescricao
        );
    }
}
