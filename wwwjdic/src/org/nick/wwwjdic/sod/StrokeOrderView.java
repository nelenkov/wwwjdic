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

    private static final float OUTLINE_WIDTH = 2f;

    private static final float KANJIVG_SIZE = 109f;

    private Paint outlinePaint;

    private boolean annotateStrokes = true;

    private List<StrokePath> strokePaths;

    private int animationDelayMillis;

    public StrokeOrderView(Context context) {
        super(context);
        init();
    }

    public StrokeOrderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
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

        int width = getWidth();
        int height = getHeight();
        int dimension = Math.min(width, height);

        float scale = dimension / KANJIVG_SIZE;
        float kanjiSize = scale * KANJIVG_SIZE;

        float dx = (width - kanjiSize) / 2;
        float dy = (height - kanjiSize) / 2;
        canvas.translate(dx, dy);

        for (StrokePath sp : strokePaths) {
            sp.draw(canvas, scale, strokeNum, annotate);
            strokeNum++;
        }
    }

    public void clear() {
        if (strokePaths != null) {
            strokePaths.clear();
        }
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

    public int getAnimationDelayMillis() {
        return animationDelayMillis;
    }

    public void setAnimationDelayMillis(int animationDelayMillis) {
        this.animationDelayMillis = animationDelayMillis;
    }

}
