package org.nick.wwwjdic.hkr;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

public class Stroke {

    private static final int ANNOTATION_OFFSET = 15;

    private List<PointF> points = new ArrayList<PointF>();

    public Stroke() {
    }

    public void addPoint(PointF p) {
        points.add(p);
    }

    public void clear() {
        points.clear();
    }

    public void draw(final Canvas canvas, final Paint paint) {
        PointF lastPoint = null;

        for (PointF p : points) {
            if (lastPoint != null) {
                canvas.drawLine(lastPoint.x, lastPoint.y, p.x, p.y, paint);
            }

            lastPoint = p;
        }
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
        float x = points.get(midwayPointIdx).x;
        float y = points.get(midwayPointIdx).y;

        float x2 = points.get(midwayPointIdx + 2).x;
        float y2 = points.get(midwayPointIdx + 2).y;

        float dx = x2 - x;
        float dy = y2 - y;

        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length >= 1.0) {
            xOffset = -dx * ANNOTATION_OFFSET / length;
            yOffset = (float) (dy * ANNOTATION_OFFSET / length - 0.5 * ANNOTATION_OFFSET);
        }

        String strokeNumStr = Integer.toString(strokeNum);
        canvas.drawText(strokeNumStr, x + xOffset, y + yOffset, paint);
    }

    public String toBase36Points() {
        StringBuffer buff = new StringBuffer();

        for (int i = 0; i < points.size(); i++) {
            String pointStr = "";
            int x = (int) points.get(i).x;
            pointStr += toBase36(x);
            int y = (int) points.get(i).y;
            pointStr += toBase36(y);

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
}
