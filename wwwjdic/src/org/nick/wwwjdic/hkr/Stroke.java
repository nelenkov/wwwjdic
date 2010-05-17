package org.nick.wwwjdic.hkr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Stroke {

    private List<Float> xs = new ArrayList<Float>();
    private List<Float> ys = new ArrayList<Float>();

    public Stroke() {
    }

    public void addPoint(float x, float y) {
        xs.add(x);
        ys.add(y);
    }

    public void clear() {
        xs.clear();
        ys.clear();
    }

    public void draw(final Canvas canvas, final Paint paint) {
        Iterator<Float> xi = xs.iterator();
        Iterator<Float> yi = ys.iterator();

        float lastX = -1;
        float lastY = -1;

        while (xi.hasNext()) {
            float x = xi.next();
            float y = yi.next();

            if (lastX != -1 && lastY != -1) {
                canvas.drawLine(lastX, lastY, x, y, paint);
            }

            lastX = x;
            lastY = y;
        }
    }

    public Iterator<Float> getXs() {
        return xs.iterator();
    }

    public Iterator<Float> getYs() {
        return ys.iterator();
    }

    public String toBase36Points() {
        StringBuffer buff = new StringBuffer();

        for (int i = 0; i < xs.size(); i++) {
            String pointStr = "";
            int x = xs.get(i).intValue();
            pointStr += toBase36(x);
            int y = ys.get(i).intValue();
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
