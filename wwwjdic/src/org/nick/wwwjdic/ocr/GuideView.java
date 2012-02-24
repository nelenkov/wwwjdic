package org.nick.wwwjdic.ocr;

import org.nick.wwwjdic.utils.UIUtils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class GuideView extends View {

    private static int GUIDE_HEIGHT = 50;
    private int firstGuideY;

    private Paint paint;

    public GuideView(Context context) {
        super(context);
        firstGuideY = UIUtils.isHoneycombTablet(context) ? 58 : 0;
        paint = new Paint();
    }

    public GuideView(Context context, AttributeSet attrs) {
        super(context, attrs);
        firstGuideY = UIUtils.isHoneycombTablet(context) ? 58 : 0;
        paint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float density = getResources().getDisplayMetrics().density;

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        int usedHeight = height - (int) (firstGuideY * density);
        int numGuides = usedHeight / GUIDE_HEIGHT;

        int top = 0;
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.LTGRAY);
        for (int i = 0; i < numGuides; i++) {
            if (i == 0) {
                top = (int) (firstGuideY * density);
            } else {
                top = top + GUIDE_HEIGHT;
            }

            int bottom = top + GUIDE_HEIGHT;
            if (i == numGuides - 1) {
                bottom = height - 1;
            }

            Rect rect = new Rect();
            rect.set(0, top, width, bottom);
            canvas.drawRect(rect, paint);
        }
    }
}
