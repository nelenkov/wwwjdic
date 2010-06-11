package org.nick.wwwjdic.sod;

import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

public class StrokeOrderView extends View {

    private static final float STROKE_WIDTH = 4f;
    private static final float OUTLINE_WIDTH = 2f;

    private Paint strokePaint;
    private Paint strokeAnnotationPaint;
    private Paint outlinePaint;

    private boolean annotateStrokes = true;

    private List<StrokePath> strokePaths;

    public StrokeOrderView(Context context) {
        super(context);
        init();
    }

    public StrokeOrderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        strokePaint = new Paint();
        strokePaint.setColor(Color.WHITE);
        strokePaint.setStyle(Style.FILL);
        strokePaint.setAntiAlias(true);
        strokePaint.setStrokeWidth(STROKE_WIDTH);

        strokeAnnotationPaint = new Paint();
        strokeAnnotationPaint.setColor(Color.GREEN);
        strokeAnnotationPaint.setStyle(Style.FILL);
        strokeAnnotationPaint.setAntiAlias(true);
        strokeAnnotationPaint.setStrokeWidth(STROKE_WIDTH);

        outlinePaint = new Paint();
        outlinePaint.setColor(Color.GRAY);
        outlinePaint.setStyle(Style.STROKE);
        outlinePaint.setAntiAlias(true);
        outlinePaint.setStrokeWidth(OUTLINE_WIDTH);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Rect r = new Rect();
        getDrawingRect(r);

        canvas.drawRect(r, outlinePaint);

        if (strokePaths != null) {
            drawStrokePaths(canvas, annotateStrokes);
        }
    }

    private void drawStrokePaths(Canvas canvas, boolean annotate) {
        int strokeNum = 1;
        for (StrokePath sp : strokePaths) {
            sp.draw(canvas, strokePaint, strokeNum, annotate);
            strokeNum++;
        }
    }

    public void clear() {
        strokePaths.clear();
        invalidate();
    }

    public boolean isAnnotateStrokes() {
        return annotateStrokes;
    }

    public void setAnnotateStrokes(boolean annotateStrokes) {
        this.annotateStrokes = annotateStrokes;
    }

    public List<StrokePath> getStrokePaths() {
        return strokePaths;
    }

    public void setStrokePaths(List<StrokePath> strokePaths) {
        this.strokePaths = strokePaths;
    }

}
