package org.nick.wwwjdic.hkr;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class KanjiDrawView extends View implements OnTouchListener {

    private static final float STROKE_WIDTH = 4f;
    private static final float OUTLINE_WIDTH = 2f;

    public static interface OnStrokesChangedListener {
        void strokesUpdated(int numStrokes);
    }

    private Paint strokePaint;
    private Paint strokeAnnotationPaint;
    private Paint outlinePaint;

    private List<Stroke> strokes = new ArrayList<Stroke>();
    private Stroke currentStroke = null;

    private OnStrokesChangedListener onStrokesChangedListener;

    public KanjiDrawView(Context context) {
        super(context);
        init();
    }

    public KanjiDrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setFocusable(true);
        setFocusableInTouchMode(true);
        setOnTouchListener(this);

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

        drawStrokes(canvas);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            currentStroke = new Stroke();
            currentStroke.addPoint(new PointF(x, y));
            break;
        case MotionEvent.ACTION_MOVE:
            currentStroke.addPoint(new PointF(x, y));
            break;
        case MotionEvent.ACTION_UP:
            currentStroke.addPoint(new PointF(x, y));
            strokes.add(currentStroke);
            if (onStrokesChangedListener != null) {
                onStrokesChangedListener.strokesUpdated(strokes.size());
            }
            break;
        }
        invalidate();

        return true;
    }

    private void drawStrokes(Canvas canvas) {
        int strokeNum = 1;
        for (Stroke stroke : strokes) {
            stroke.draw(canvas, strokePaint);
            stroke.annotate(canvas, strokeAnnotationPaint, strokeNum);
            strokeNum++;
        }
    }

    public void clear() {
        strokes.clear();
        invalidate();
    }

    public List<Stroke> getStrokes() {
        return strokes;
    }

    public OnStrokesChangedListener getOnStrokesChangedListener() {
        return onStrokesChangedListener;
    }

    public void setOnStrokesChangedListener(
            OnStrokesChangedListener onStrokesChangedListener) {
        this.onStrokesChangedListener = onStrokesChangedListener;
    }
}
