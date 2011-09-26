package org.nick.wwwjdic.ocr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

public class GuideView extends View {

    private static final boolean IS_HONEYCOMB = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

    private static int GUIDE_HEIGHT = 50;
    private static int FIRST_GUIDE_Y = IS_HONEYCOMB ? 58 : 0;

    public GuideView(Context context) {
        super(context);
    }

    public GuideView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float density = getResources().getDisplayMetrics().density;

        final Paint paint = new Paint();
        final Rect rect = new Rect();

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        int usedHeight = height - (int) (FIRST_GUIDE_Y * density);
        int numGuides = usedHeight / GUIDE_HEIGHT;

        int top = 0;
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.LTGRAY);
        for (int i = 0; i < numGuides; i++) {
            if (i == 0) {
                top = (int) (FIRST_GUIDE_Y * density);
            } else {
                top = top + GUIDE_HEIGHT;
            }

            int bottom = top + GUIDE_HEIGHT;
            if (i == numGuides - 1) {
                bottom = height - 1;
            }
            rect.set(0, top, width, bottom);
            canvas.drawRect(rect, paint);
        }
    }
}
