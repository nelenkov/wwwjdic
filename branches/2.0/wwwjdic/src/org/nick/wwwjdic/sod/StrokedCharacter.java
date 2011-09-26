package org.nick.wwwjdic.sod;

import java.util.ArrayList;
import java.util.List;

import org.nick.wwwjdic.hkr.Stroke;

import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

public class StrokedCharacter {

    private List<StrokePath> strokes;

    private Float canvasWidth;
    private Float canvasHeight;
    private RectF bounds;

    private boolean transformed;
    private boolean segemented;

    private boolean needsPaddding;

    public StrokedCharacter() {
        this.strokes = new ArrayList<StrokePath>();
    }

    public StrokedCharacter(List<StrokePath> strokes, float canvasWidth,
            float canvasHeight) {
        this.strokes = strokes;
        this.canvasHeight = canvasHeight;
        this.canvasWidth = canvasWidth;
    }

    public StrokedCharacter(List<Stroke> strokesList) {
        this.strokes = new ArrayList<StrokePath>();
        for (Stroke s : strokesList) {
            PointF fp = s.getPoints().get(0);
            PointF firstPoint = new PointF(fp.x, fp.y);
            strokes.add(new StrokePath(firstPoint, new Path(s.getPath())));
        }
    }

    public List<StrokePath> getStrokes() {
        return strokes;
    }

    public float getCanvasWidth() {
        if (canvasWidth == null) {
            return getBounds().width();
        }

        return canvasWidth;
    }

    public void setCanvasWidth(Float canvasWidth) {
        this.canvasWidth = canvasWidth;
    }

    public float getCanvasHeight() {
        if (canvasHeight == null) {
            return getBounds().height();
        }

        return canvasHeight;
    }

    public void setCanvasHeight(Float canvasHeight) {
        this.canvasHeight = canvasHeight;
    }

    public RectF getBounds() {
        if (bounds != null) {
            return bounds;
        }

        bounds = new RectF();
        for (StrokePath s : strokes) {
            RectF strokeBounds = new RectF();
            s.getStrokePath().computeBounds(strokeBounds, true);
            bounds.union(strokeBounds);
        }

        return bounds;
    }

    public RectF getScaledBounds(float scale) {
        RectF scaledBounds = new RectF();
        Matrix m = new Matrix();
        m.postScale(scale, scale);
        for (StrokePath sp : strokes) {
            Path p = new Path(sp.getStrokePath());
            sp.getStrokePath().transform(m, p);
            RectF b = new RectF();
            p.computeBounds(b, true);
            scaledBounds.union(b);
        }

        return scaledBounds;
    }

    public void addStroke(StrokePath stroke) {
        strokes.add(stroke);
    }

    public boolean hasStrokes() {
        return strokes != null && !strokes.isEmpty();
    }

    public boolean isTransformed() {
        return transformed;
    }

    public void setTransformed(boolean transformed) {
        this.transformed = transformed;
    }

    public float getDimension() {
        return Math.max(getCanvasWidth(), getCanvasHeight());
    }

    public void segmentStrokes(float scale, float dx, float dy,
            float segmentLength) {
        if (!segemented) {
            for (StrokePath sp : strokes) {
                sp.segmentStroke(segmentLength, scale, dx, dy);
            }
            segemented = true;
        }
    }

    public boolean isSegemented() {
        return segemented;
    }

    public void clear() {
        if (strokes != null) {
            strokes.clear();
        }

        canvasWidth = null;
        canvasHeight = null;
        bounds = null;

        transformed = false;
        segemented = false;
    }

    public void resetSegments() {
        for (StrokePath sp : strokes) {
            sp.resetSegments();
        }
        segemented = false;
    }

    public boolean isNeedsPaddding() {
        return needsPaddding;
    }

    public void setNeedsPaddding(boolean needsPaddding) {
        this.needsPaddding = needsPaddding;
    }

}
