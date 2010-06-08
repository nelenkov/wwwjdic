/**
 * 
 */
package org.nick.wwwjdic.sod;

import android.graphics.PointF;

public class Curve {

    private final PointF p1;
    private final PointF p2;
    private final PointF p3;

    private final boolean relative;

    public Curve(PointF p1, PointF p2, PointF p3, boolean relative) {
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.relative = relative;
    }

    public PointF getP1() {
        return p1;
    }

    public PointF getP2() {
        return p2;
    }

    public PointF getP3() {
        return p3;
    }

    public boolean isRelative() {
        return relative;
    }

}
