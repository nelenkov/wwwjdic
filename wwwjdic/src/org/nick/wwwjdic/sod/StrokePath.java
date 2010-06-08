/**
 * 
 */
package org.nick.wwwjdic.sod;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Paint.Style;
import android.graphics.Path.FillType;
import android.graphics.drawable.Drawable;

public class StrokePath extends Drawable {

    private static final float STROKE_WIDTH = 4f;

    private PointF moveTo;
    private List<Curve> curves = new ArrayList<Curve>();

    private Paint strokePaint;
    private Paint strokeAnnotationPaint;

    private int alpha;
    private ColorFilter colorFilter;

    public StrokePath(PointF moveTo) {
        this.moveTo = moveTo;

        strokePaint = new Paint();
        strokePaint.setColor(Color.WHITE);
        strokePaint.setStyle(Style.FILL);
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

    public void draw(final Canvas canvas, final Paint paint, int strokeNum) {
        Path path = new Path();
        path.moveTo(moveTo.x, moveTo.y);
        path.setFillType(FillType.WINDING);

        paint.setStyle(Style.STROKE);

        PointF firstPoint = moveTo;

        Matrix matrix = new Matrix();
        matrix.postScale(3.0f, 3.0f);

        float[] cs = new float[2];
        cs[0] = firstPoint.x;
        cs[1] = firstPoint.y;
        matrix.mapPoints(cs);

        // canvas.drawCircle(cs[0], cs[1], 2, paint);

        String strokeNumStr = Integer.toString(strokeNum);
        canvas.drawText(strokeNumStr, cs[0] + 7, cs[1] - 7,
                strokeAnnotationPaint);

        for (Curve c : curves) {
            if (c.isRelative()) {
                path.rCubicTo(c.getP1().x, c.getP1().y, c.getP2().x,
                        c.getP2().y, c.getP3().x, c.getP3().y);
            } else {
                path.cubicTo(c.getP1().x, c.getP1().y, c.getP2().x,
                        c.getP2().y, c.getP3().x, c.getP3().y);
            }

            if (c.isRelative()) {
                firstPoint = new PointF(firstPoint.x + c.getP3().x,
                        firstPoint.y + c.getP3().y);
            } else {
                firstPoint = c.getP3();
            }
        }

        path.transform(matrix);
        canvas.drawPath(path, paint);
    }

    public static StrokePath parsePath(String path) {
        boolean isInMoveTo = false;

        StringBuffer buff = new StringBuffer();
        Float x = null;
        Float y = null;

        PointF p1 = null;
        PointF p2 = null;
        PointF p3 = null;

        StrokePath result = null;
        boolean relative = false;

        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == 'M') {
                isInMoveTo = true;
                continue;
            }

            if (Character.isDigit(c) || c == '.') {
                buff.append(Character.toString(c));
            }

            if (c == ',' || c == '-' || c == 'c' || c == 'C'
                    || i == (path.length() - 1)) {
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

                    if (p1 != null && p2 != null && p3 != null) {
                        result.addCurve(new Curve(p1, p2, p3, relative));
                        p1 = null;
                        p2 = null;
                        p3 = null;
                    }
                }
            }

            if (c == 'c' || c == 'C') {
                if (c == 'c') {
                    relative = true;
                } else {
                    relative = false;
                }
                isInMoveTo = false;
            }
        }

        return result;
    }

    @Override
    public void draw(Canvas canvas) {
        // XXX
        draw(canvas, strokePaint, 1);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        colorFilter = cf;
    }
}
