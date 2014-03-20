package org.nick.wwwjdic.hkr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class KanjiDrawView extends View {

    private static final float STROKE_WIDTH = 8f;
    private static final float OUTLINE_WIDTH = 2f;
    private static final float ANNOTATION_TEXT_SIZE = 12f;

    public static interface OnStrokesChangedListener {
        void strokesUpdated(int numStrokes);
    }

    private Paint strokePaint;
    private Paint strokeAnnotationPaint;
    private float annotationTextSize = ANNOTATION_TEXT_SIZE;
    private Paint outlinePaint;
    private Rect outlineRect;

    private List<Stroke> strokes = new ArrayList<Stroke>();
    private Stroke currentStroke = null;

    private OnStrokesChangedListener onStrokesChangedListener;

    private boolean annotateStrokes = true;
    private boolean annotateStrokesMidway = false;

    private boolean currentStrokeDone = false;

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

        strokePaint = new Paint();
        strokePaint.setColor(Color.WHITE);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setAntiAlias(true);
        strokePaint.setDither(true);
        strokePaint.setStrokeWidth(STROKE_WIDTH);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);

        strokeAnnotationPaint = new Paint();
        strokeAnnotationPaint.setColor(Color.GREEN);
        strokeAnnotationPaint.setStyle(Style.FILL);
        strokeAnnotationPaint.setAntiAlias(true);
        strokeAnnotationPaint.setTextSize(annotationTextSize);

        outlinePaint = new Paint();
        outlinePaint.setColor(Color.GRAY);
        outlinePaint.setStyle(Style.STROKE);
        outlinePaint.setAntiAlias(true);
        outlinePaint.setStrokeWidth(OUTLINE_WIDTH);

        outlineRect = new Rect();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // TODO?
        // scale and translate?
        super.onSizeChanged(w, h, oldw, oldh);
    }

    public void setStrokePaintColor(int color) {
        strokePaint.setColor(color);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        getDrawingRect(outlineRect);

        canvas.drawRect(outlineRect, outlinePaint);

        drawStrokes(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            currentStroke = new Stroke();
            currentStroke.addPoint(new PointF(x, y));
            strokes.add(currentStroke);
            currentStrokeDone = false;
            break;
        case MotionEvent.ACTION_MOVE:
            currentStroke.addPoint(new PointF(x, y));
            break;
        case MotionEvent.ACTION_UP:
            currentStroke.addPoint(new PointF(x, y));
            if (onStrokesChangedListener != null) {
                onStrokesChangedListener.strokesUpdated(strokes.size());
            }
            currentStrokeDone = true;
            break;
        }
        invalidate();

        return true;
    }

    private void drawStrokes(Canvas canvas) {
        int strokeNum = 1;
        for (int i = 0; i < strokes.size(); i++) {
            Stroke stroke = strokes.get(i);
            stroke.draw(canvas, strokePaint);

            if (annotateStrokes) {
                if (i == strokes.size() - 1 && !currentStrokeDone) {
                    break;
                }

                if (annotateStrokesMidway) {
                    stroke.annotateMidway(canvas, strokeAnnotationPaint,
                            strokeNum);
                } else {
                    stroke.annotate(canvas, strokeAnnotationPaint, strokeNum);
                }
                strokeNum++;
            }
        }
    }

    public List<Stroke> getStrokes() {
        return strokes;
    }

    public void removeLastStroke() {
        if (strokes.isEmpty()) {
            return;
        }

        strokes.remove(strokes.size() - 1);
        invalidate();
    }

    public void clear() {
        strokes.clear();
        invalidate();
    }

    public OnStrokesChangedListener getOnStrokesChangedListener() {
        return onStrokesChangedListener;
    }

    public void setOnStrokesChangedListener(
            OnStrokesChangedListener onStrokesChangedListener) {
        this.onStrokesChangedListener = onStrokesChangedListener;
    }

    public boolean isAnnotateStrokes() {
        return annotateStrokes;
    }

    public void setAnnotateStrokes(boolean annotateStrokes) {
        this.annotateStrokes = annotateStrokes;
    }

    public boolean isAnnotateStrokesMidway() {
        return annotateStrokesMidway;
    }

    public void setAnnotateStrokesMidway(boolean annotateStrokesMidway) {
        this.annotateStrokesMidway = annotateStrokesMidway;
    }

    public float getAnnotationTextSize() {
        return annotationTextSize;
    }

    public void setAnnotationTextSize(float annotationTextSize) {
        this.annotationTextSize = annotationTextSize;
        strokeAnnotationPaint.setTextSize(annotationTextSize);
    }

}
