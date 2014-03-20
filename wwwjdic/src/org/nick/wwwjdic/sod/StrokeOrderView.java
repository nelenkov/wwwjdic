package org.nick.wwwjdic.sod;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class StrokeOrderView extends View {

    private static final float SEGMENT_LENGTH = 20f;

    private static final float OUTLINE_WIDTH = 2f;

    private Paint outlinePaint;

    private boolean annotateStrokes = true;

    private StrokedCharacter character;

    private int animationDelayMillis;

    private long lastTick = 0;

    private boolean animate = false;

    private float annotationTextSize = -1;

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
        drawOutline(canvas);

        if (character != null && character.hasStrokes()) {
            drawStrokePaths(canvas, annotateStrokes);
        }
    }

    private void drawOutline(Canvas canvas) {
        Rect r = new Rect();
        getDrawingRect(r);
        r.top += 2;
        outlinePaint.setPathEffect(null);
        canvas.drawRect(r, outlinePaint);
    }

    private void drawStrokePaths(Canvas canvas, boolean annotate) {
        int strokeNum = 1;

        float scale = 1;
        float dx = 0;
        float dy = 0;

        int width = getWidth();
        int height = getHeight();

        int dimension = Math.min(width, height);
        if (!character.isTransformed()) {

            float originalDimension = character.getDimension();
            scale = dimension / originalDimension;

            RectF scaledBounds = character.getScaledBounds(scale);

            RectF r = new RectF(0, 0, width, height);
            dx = Math.abs(r.centerX() - scaledBounds.centerX());
            dy = Math.abs(r.centerY() - scaledBounds.centerY());
            if (r.centerX() < scaledBounds.centerX()) {
                dx *= -1;
            }
            if (r.centerY() < scaledBounds.centerY()) {
                dy *= -1;
            }

            character.setCanvasWidth(Float.valueOf(width));
            character.setCanvasHeight(Float.valueOf(height));
            character.setTransformed(true);
        }

        if (!animate) {
            List<StrokePath> strokePaths = character.getStrokes();
            for (StrokePath sp : strokePaths) {
                if (annotationTextSize != -1) {
                    sp.setAnnotationTextSize(annotationTextSize);
                }
                sp.draw(canvas, scale, dx, dy, strokeNum, annotate);
                strokeNum++;
            }
            return;
        }

        character.segmentStrokes(scale, dx, dy, SEGMENT_LENGTH);

        boolean advance = false;
        long time = (System.currentTimeMillis() - lastTick);
        if (time >= animationDelayMillis) {
            lastTick = System.currentTimeMillis();
            advance = true;
        }

        List<StrokePath> strokePaths = character.getStrokes();
        for (int i = 0; i < strokePaths.size(); i++) {
            StrokePath sp = strokePaths.get(i);
            if (sp.isFullyDrawn()) {
                if (annotationTextSize != -1) {
                    sp.setAnnotationTextSize(annotationTextSize);
                }
                sp.draw(canvas, scale, dx, dy, strokeNum, annotate);
                strokeNum++;

                // all strokes drawn, stop animating
                if (i == strokePaths.size() - 1) {
                    animate = false;
                }
            } else {
                if (advance) {
                    sp.advanceSegment();
                }
                sp.drawSegments(canvas);
                break;
            }
        }

        postInvalidate();
    }

    public void clear() {
        if (character != null) {
            character = null;
        }
        lastTick = 0;

        animate = false;
        invalidate();
    }

    public boolean isAnnotateStrokes() {
        return annotateStrokes;
    }

    public void setAnnotateStrokes(boolean annotateStrokes) {
        this.annotateStrokes = annotateStrokes;
    }

    public StrokedCharacter getCharacter() {
        return character;
    }

    public void setCharacter(StrokedCharacter character) {
        this.character = character;
    }

    public List<StrokePath> getStrokePaths() {
        if (character == null) {
            return null;
        }

        return character.getStrokes();
    }

    public int getAnimationDelayMillis() {
        return animationDelayMillis;
    }

    public void setAnimationDelayMillis(int animationDelayMillis) {
        this.animationDelayMillis = animationDelayMillis;
    }

    public void startAnimation() {
        animate = true;
        lastTick = 0;
        if (character != null) {
            character.resetSegments();
        }
        invalidate();
    }

    public float getAnnotationTextSize() {
        return annotationTextSize;
    }

    public void setAnnotationTextSize(float annotationWidth) {
        this.annotationTextSize = annotationWidth;
    }

}
