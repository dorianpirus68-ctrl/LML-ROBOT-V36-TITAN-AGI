package com.lml.control;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

public final class HoloCoreView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);

    public HoloCoreView(Context context) {
        super(context);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(2f);
        line.setColor(Color.rgb(66, 239, 255));
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
        canvas.drawLine(cx - r * 1.7f, cy, cx + r * 1.7f, cy, line);
        canvas.drawLine(cx, cy - r * 1.7f, cx, cy + r * 1.7f, line);
        paint.setColor(Color.rgb(208, 255, 255));
        canvas.drawCircle(cx, cy, r * .28f, paint);
        paint.setColor(Color.rgb(8, 18, 34));
        canvas.drawCircle(cx, cy, r * .14f, paint);
    }
}
