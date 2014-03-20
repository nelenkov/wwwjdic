package org.nick.wwwjdic.hkr;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.FloatMath;

import java.util.ArrayList;
import java.util.List;

public class Stroke {

    private static final int ANNOTATION_OFFSET = 15;

    private static final float TOUCH_TOLERANCE = 4;

    private List<PointF> points = new ArrayList<PointF>();

    private Path path = new Path();
    private PointF lastPoint;

    public Stroke() {
    }

    public void addPoint(PointF p) {
        if (points.isEmpty()) {
            path.reset();
            path.moveTo(p.x, p.y);
            lastPoint = new PointF(p.x, p.y);
        } else {
            float dx = Math.abs(p.x - lastPoint.x);
            float dy = Math.abs(p.y - lastPoint.y);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                path.quadTo(lastPoint.x, lastPoint.y, (p.x + lastPoint.x) / 2,
                        (p.y + lastPoint.y) / 2);
                lastPoint = new PointF(p.x, p.y);
            }
        }

        points.add(p);
    }

    public void clear() {
        points.clear();
        path.reset();
    }

    public void draw(final Canvas canvas, final Paint paint) {
        path.lineTo(lastPoint.x, lastPoint.y);
        canvas.drawPath(path, paint);
    }

    public void annotate(final Canvas canvas, final Paint paint, int strokeNum) {
        if (points.isEmpty() || points.size() == 1) {
            return;
        }

        float xOffset = ANNOTATION_OFFSET;
        float yOffset = ANNOTATION_OFFSET;
        int annotationGap = 1;

        PointF firstPoint = points.get(0);
        if (points.size() > 5) {
            annotationGap = 5;
        }

        float dx = points.get(annotationGap).x - firstPoint.x;
        float dy = points.get(annotationGap).y - firstPoint.y;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length > 0) {
            dx /= length;
            dy /= length;
            xOffset = -ANNOTATION_OFFSET * dx;
            yOffset = -ANNOTATION_OFFSET * dy;
        }

        int height = canvas.getClipBounds().height();
        int width = canvas.getClipBounds().width();

        float x = firstPoint.x + xOffset;
        float y = firstPoint.y + yOffset;

        if (x < 0) {
            x = ANNOTATION_OFFSET;
        }

        if (y < 0) {
            y = ANNOTATION_OFFSET;
        }

        if (x > width) {
            x = width - ANNOTATION_OFFSET;
        }
        if (y > height) {
            y = height - ANNOTATION_OFFSET;
        }

        String strokeNumStr = Integer.toString(strokeNum);
        canvas.drawText(strokeNumStr, x, y, paint);
    }

    public void annotateMidway(final Canvas canvas, final Paint paint,
            int strokeNum) {
        if (points.isEmpty() || points.size() == 1) {
            return;
        }

        float xOffset = ANNOTATION_OFFSET;
        float yOffset = ANNOTATION_OFFSET;

        int midwayPointIdx = points.size() / 2 - 1;
        if (midwayPointIdx + 2 <= points.size()) {
            float x = points.get(0).x;
            float y = points.get(0).y;
            String strokeNumStr = Integer.toString(strokeNum);
            canvas.drawText(strokeNumStr, x + ANNOTATION_OFFSET, y
                    + ANNOTATION_OFFSET, paint);

            return;
        }

        float x = points.get(midwayPointIdx).x;
        float y = points.get(midwayPointIdx).y;

        float x2 = points.get(midwayPointIdx + 2).x;
        float y2 = points.get(midwayPointIdx + 2).y;

        float dx = x2 - x;
        float dy = y2 - y;

        float length = (float) FloatMath.sqrt(dx * dx + dy * dy);
        if (length >= 1.0) {
            xOffset = -dx * ANNOTATION_OFFSET / length;
            yOffset = (float) (dy * ANNOTATION_OFFSET / length - 0.5 * ANNOTATION_OFFSET);
        }

        String strokeNumStr = Integer.toString(strokeNum);
        canvas.drawText(strokeNumStr, x + xOffset, y + yOffset, paint);
    }

    public List<PointF> getPoints() {
        return points;
    }

    public String toBase36Points() {
        StringBuffer buff = new StringBuffer();

        for (PointF p : points) {
            String pointStr = "";
            int x = Math.abs((int) p.x);
            pointStr += toBase36(x);
            int y = Math.abs((int) p.y);
            pointStr += toBase36(y);

            buff.append(pointStr);
        }
        String result = buff.toString();
        if (result.length() % 4 != 0) {
            result = result.substring(0, result.length() - 2);
        }

        return result;
    }

    public String toPoints() {
        StringBuffer buff = new StringBuffer();

        for (PointF p : points) {
            String pointStr = "";
            int x = (int) p.x;
            pointStr += " " + x;
            int y = (int) p.y;
            pointStr += " " + y;

            buff.append(pointStr);
        }

        return buff.toString();
    }

    private String toBase36(int i) {
        String result = Integer.toString(i, 36);
        if (result.length() == 1) {
            result = "0" + result;
        }

        return result;
    }

    public Path getPath() {
        return path;
    }
}
