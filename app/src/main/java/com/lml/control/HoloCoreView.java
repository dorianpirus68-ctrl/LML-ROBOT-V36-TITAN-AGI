package com.lml.control;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

public final class HoloCoreView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);

    public HoloCoreView(Context context) {
        super(context);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(2f);
        line.setColor(Color.rgb(66, 239, 255));
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(12f);
        label.setLetterSpacing(.10f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float cx = width / 2f;
        float cy = height / 2f;
        float r = Math.min(width, height) * 0.24f;
        paint.setShader(new RadialGradient(cx, cy, r * 1.55f,
                new int[]{Color.argb(210, 60, 236, 255), Color.argb(44, 40, 131, 255), Color.TRANSPARENT},
                new float[]{0f, .47f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r * 1.55f, paint);
        paint.setShader(null);
        for (int i = 1; i <= 4; i++) {
            canvas.drawCircle(cx, cy, r * i / 2.4f, line);
        }
        RectF segment = new RectF(cx - r * 1.28f, cy - r * 1.28f, cx + r * 1.28f, cy + r * 1.28f);
        line.setStrokeWidth(r * .13f);
        line.setColor(Color.rgb(91, 245, 255)); canvas.drawArc(segment, -82f, 68f, false, line);
        line.setColor(Color.rgb(255, 205, 90)); canvas.drawArc(segment, 8f, 68f, false, line);
        line.setColor(Color.rgb(255, 126, 118)); canvas.drawArc(segment, 98f, 68f, false, line);
        line.setColor(Color.rgb(91, 245, 255)); canvas.drawArc(segment, 188f, 68f, false, line);
        line.setStrokeWidth(2f);
        label.setColor(Color.rgb(169, 211, 222));
        canvas.drawText("PROPOSER", cx, cy - r * 1.65f, label);
        canvas.drawText("VÉRIFIER", cx + r * 1.8f, cy + 4f, label);
        canvas.drawText("AUTORISER", cx, cy + r * 1.83f, label);
        canvas.drawText("EXÉCUTER", cx - r * 1.8f, cy + 4f, label);
        paint.setColor(Color.rgb(208, 255, 255));
        canvas.drawCircle(cx, cy, r * .28f, paint);
        paint.setColor(Color.rgb(8, 18, 34));
        canvas.drawCircle(cx, cy, r * .14f, paint);
    }
}
