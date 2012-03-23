/**
 * 
 */
package org.nick.wwwjdic.sod;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.nick.wwwjdic.BuildConfig;
import org.xmlpull.v1.XmlPullParser;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.util.Log;
import android.util.Xml;

public class StrokePath {

    private static final String TAG = StrokePath.class.getSimpleName();

    private static final float STROKE_WIDTH = 6f;

    private PointF moveTo;
    private List<Curve> curves = new ArrayList<Curve>();
    private Path strokePath;
    private boolean pathScaled = false;
    private float translationDx = -1;
    private float translationDy = -1;

    private Paint strokePaint;
    private Paint strokeAnnotationPaint;

    private List<Path> segments;
    private int currentSegment;

    public StrokePath(PointF firstPoint, Path path) {
        this.moveTo = firstPoint;
        this.strokePath = path;

        initPaints();
    }

    public StrokePath(PointF moveTo) {
        this.moveTo = moveTo;

        initPaints();
    }

    private void initPaints() {
        strokePaint = new Paint();
        strokePaint.setColor(Color.WHITE);
        strokePaint.setStyle(Style.STROKE);
        strokePaint.setAntiAlias(true);
        strokePaint.setStrokeWidth(STROKE_WIDTH);

        strokeAnnotationPaint = new Paint();
        strokeAnnotationPaint.setColor(Color.GREEN);
        strokeAnnotationPaint.setStyle(Style.FILL);
        strokeAnnotationPaint.setAntiAlias(true);
        strokeAnnotationPaint.setStrokeWidth(4f);
    }

    public void addCurve(Curve curve) {
        curves.add(curve);
    }

    public PointF getMoveTo() {
        return moveTo;
    }

    public List<Curve> getCurves() {
        return curves;
    }

    public void draw(final Canvas canvas, final float scale, float dx,
            float dy, int strokeNum, boolean annotate) {
        Matrix matrix = new Matrix();

        if (strokePath != null) {
            boolean needsScaling = !pathScaled;
            if (needsScaling) {
                matrix.postScale(scale, scale);
                pathScaled = true;
            }
            if (needsTranslation(dx, dy)) {
                matrix.postTranslate(dx, dy);
                translationDx = dx;
                translationDy = dy;
            }

            transformMoveTo(matrix);

            strokePath.transform(matrix);

            if (annotate) {
                annotate(canvas, strokeNum);
            }

        } else {
            strokePath = curvesToPath();

            matrix.postScale(scale, scale);
            matrix.postTranslate(dx, dy);

            transformMoveTo(matrix);
            strokePath.transform(matrix);

            pathScaled = true;
            translationDx = dx;
            translationDy = dy;

            if (annotate) {
                annotate(canvas, strokeNum);
            }
        }

        canvas.drawPath(strokePath, strokePaint);
    }


    private void transformMoveTo(Matrix matrix) {
        float[] cs = new float[2];
        cs[0] = moveTo.x;
        cs[1] = moveTo.y;
        matrix.mapPoints(cs);
        moveTo.x = cs[0];
        moveTo.y = cs[1];
    }

    private boolean needsTranslation(float dx, float dy) {
        return dx != 0 && dy != 0 && dx != translationDx || dy != translationDy;
    }

    private Path curvesToPath() {
        Path path = new Path();
        path.moveTo(moveTo.x, moveTo.y);
        path.setFillType(FillType.WINDING);

        PointF lastPoint = moveTo;
        PointF lastP2 = null;

        int idx = 0;
        for (Curve c : curves) {
            PointF p1 = null;
            PointF p2 = null;
            PointF p3 = null;

            if (c.isRelative()) {
                p1 = calcAbsolute(lastPoint, c.getP1());
                p2 = calcAbsolute(lastPoint, c.getP2());
                p3 = calcAbsolute(lastPoint, c.getP3());
            } else {
                p1 = c.getP1();
                p2 = c.getP2();
                p3 = c.getP3();
            }

            if (c.isSmooth()) {
                p1 = calcReflectionRelToCurrent(lastP2, lastPoint);
            }

            path.cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);

            if (c.isRelative()) {
                lastP2 = new PointF(lastPoint.x + c.getP2().x, lastPoint.y
                        + c.getP2().y);
                lastPoint = new PointF(lastPoint.x + c.getP3().x, lastPoint.y
                        + c.getP3().y);
            } else {
                lastPoint = c.getP3();
                lastP2 = c.getP2();
            }
            idx++;
        }
        return path;
    }

    private void annotate(final Canvas canvas, int strokeNum) {
        float x = moveTo.x;
        float y = moveTo.y;

        String strokeNumStr = Integer.toString(strokeNum);
        canvas.drawText(strokeNumStr, x + 7, y - 9, strokeAnnotationPaint);
    }

    private Path getStrokePath(float scale) {
        Matrix matrix = new Matrix();

        if (strokePath != null) {
            if (!pathScaled) {
                matrix.postScale(scale, scale);
                strokePath.transform(matrix);
                pathScaled = true;
            }
        } else {
            strokePath = curvesToPath();
            matrix.postScale(scale, scale);
            strokePath.transform(matrix);
            pathScaled = true;
        }

        return strokePath;
    }

    public void segmentStroke(float segmentLength, float scale, float dx,
            float dy) {
        Path path = getStrokePath(scale);

        segments = segmentPath(path, segmentLength, scale, dx, dy);
        currentSegment = 0;
    }

    private List<Path> segmentPath(Path path, float segmentLength, float scale,
            float dx, float dy) {
        PathMeasure pm = new PathMeasure(path, false);
        float length = pm.getLength();

        float start = 0;
        float delta = segmentLength;

        List<Path> segments = new ArrayList<Path>();
        while (start <= length) {
            float end = start + delta;
            if (end > length) {
                end = length;
            }

            Path segment = new Path();
            pm.getSegment(start, end, segment, true);
            if (needsTranslation(dx, dy)) {
                Matrix matrix = new Matrix();
                matrix.postTranslate(dx, dy);
                segment.transform(matrix);
                translationDx = dx;
                translationDy = dy;
            }
            segments.add(segment);
            start += delta;
        }

        return segments;
    }

    private PointF calcAbsolute(PointF currentPoint, PointF p) {
        // p1 can be null for smooth curves
        if (p == null) {
            return null;
        }

        return new PointF(p.x + currentPoint.x, p.y + currentPoint.y);
    }

    private PointF calcReflectionRelToCurrent(PointF p, PointF currentPoint) {
        return new PointF((float) (2.0 * currentPoint.x - p.x),
                (float) (2.0 * currentPoint.y - p.y));
    }

    public static StrokePath parsePath(String path) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "parsing " + path);
        }

        boolean isInMoveTo = false;

        StringBuffer buff = new StringBuffer();
        Float x = null;
        Float y = null;

        PointF p1 = null;
        PointF p2 = null;
        PointF p3 = null;

        StrokePath result = null;
        boolean relative = false;
        boolean smooth = false;

        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == 'M' || c == 'm') {
                isInMoveTo = true;
                continue;
            }

            if (Character.isDigit(c) || c == '.') {
                buff.append(Character.toString(c));
            }

            if (c == ',' || c == '-' || c == 'c' || c == 'C' || c == 's'
                    || c == 'S' || i == (path.length() - 1)) {
                String floatStr = buff.toString();
                // System.out.println("i: " + i);
                // System.out.println("c: " + c);
                // System.out.println("floastStr: " + floatStr);
                buff = new StringBuffer();
                if (c == '-') {
                    buff.append(c);
                }

                if ("".equals(floatStr)) {
                    continue;
                }

                float f = Float.parseFloat(floatStr);
                if (x == null) {
                    x = f;
                } else {
                    y = f;
                }
            }

            if (x != null && y != null) {
                PointF p = new PointF(x, y);
                x = null;
                y = null;

                if (isInMoveTo) {
                    result = new StrokePath(p);
                } else {
                    if (p1 == null) {
                        p1 = p;
                    } else if (p1 != null && p2 == null) {
                        p2 = p;
                    } else if (p1 != null && p2 != null && p3 == null) {
                        p3 = p;
                    }

                    if (!smooth) {
                        if (p1 != null && p2 != null && p3 != null) {
                            result.addCurve(new Curve(p1, p2, p3, relative,
                                    smooth));
                            p1 = null;
                            p2 = null;
                            p3 = null;
                        }
                    } else {
                        if (p1 != null && p2 != null) {
                            result.addCurve(new Curve(null, p1, p2, relative,
                                    smooth));
                            p1 = null;
                            p2 = null;
                            p3 = null;
                        }
                    }
                }
            }

            if (c == 'c' || c == 'C' || c == 's' || c == 'S') {
                relative = (c == 'c' || c == 's');
                smooth = (c == 's' || c == 'S');
                isInMoveTo = false;
            }
        }

        return result;
    }

    public static List<StrokePath> parseKangiVgXml(File f) {
        List<StrokePath> strokes = new ArrayList<StrokePath>();
        XmlPullParser parser = Xml.newPullParser();

        try {
            // auto-detect the encoding from the stream
            parser.setInput(new FileInputStream(f), null);
            int eventType = parser.getEventType();
            boolean done = false;

            while (eventType != XmlPullParser.END_DOCUMENT && !done) {
                String name = null;
                switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    break;
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    if (name.equalsIgnoreCase("stroke")) {
                        String path = parser.getAttributeValue(null, "path");
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "parsing " + path);
                        }
                        if (path != null && !"".equals(path)) {
                            StrokePath strokePath = StrokePath.parsePath(path);
                            strokes.add(strokePath);
                        }
                    }
                    if (name.equalsIgnoreCase("kanji")) {
                        String unicode = parser.getAttributeValue(null, "id");
                        Log.d(TAG, unicode);
                    }
                    break;
                case XmlPullParser.END_TAG:
                    name = parser.getName();

                    break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return strokes;
    }

    public List<Path> getSegments() {
        return segments;
    }

    public void advanceSegment() {
        currentSegment++;
    }

    public void resetSegments() {
        if (segments != null) {
            segments.clear();
        }
        currentSegment = 0;
    }

    public boolean isFullyDrawn() {
        if (segments == null) {
            return true;
        }

        return currentSegment == segments.size() - 1;
    }

    public boolean isSegmented() {
        return segments != null && !segments.isEmpty();
    }

    public void drawSegments(Canvas canvas) {
        if (segments.isEmpty()) {
            return;
        }

        Path linkedPath = new Path();
        for (int i = 0; i <= currentSegment; i++) {
            Path segment = segments.get(i);
            linkedPath.addPath(segment);
        }
        canvas.drawPath(linkedPath, strokePaint);
    }

    public Path getStrokePath() {
        if (strokePath == null) {
            strokePath = curvesToPath();
        }

        return strokePath;
    }

    public void setStrokePath(Path strokePath) {
        this.strokePath = strokePath;
    }

    public void reset() {
        strokePath = null;
        pathScaled = false;
        translationDx = -1;
        translationDy = -1;
    }

    public boolean isScaled() {
        return pathScaled;
    }

    public void setStrokePaintColor(int color) {
        strokePaint.setColor(color);
    }

}
