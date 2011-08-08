package org.nick.wwwjdic.test;

import java.io.File;

import junit.framework.TestCase;

import org.nick.wwwjdic.sod.Curve;
import org.nick.wwwjdic.sod.StrokePath;

public class StrokePathTest extends TestCase {

    private String[] strokes = new String[] {
            "M23.04,19.27c0.84,0.84,1.27,1.73,1.27,3.22c0,3.75,0.04,21.88,0.04,25.75c0,3.2-0.04,5.42-0.04,5.55",
            "M25.19,21.42c4.18-0.54,17.43-2.5,19.41-2.65c1.65-0.13,2.58,0.98,2.58,2.72c0,2.76,0.13,16.23,0.04,24.76c-0.03,3.25-0.05,5.55-0.05,5.77",
            "M25.58,35.83c4.05-0.33,17.14-2.13,20.45-2.13",
            "M25.44,50.92c7.93-0.79,13.79-1.64,20.58-1.87",
            "M59.91,13.72c0.81,0.81,1.2,1.91,1.25,3.28C62,40.88,59.25,52.75,49.32,62.75",
            "M61.91,15.56c1.99-0.15,21.24-2.5,22.73-2.58c2.12-0.11,3.32,0.77,3.32,2.82c0,3.45,0,27.85,0,39.88c0,9.06-6.46,1.93-7.44,1.34",
            "M62.66,29.03c6.84-0.78,18.41-1.74,24.39-2.18",
            "M61.19,41.22c5.32-0.45,19.63-1.34,25.77-1.64",
            "M27.34,70.6c0.97,0.97,1.34,1.76,1.44,2.77s1.47,13,2.07,19.63",
            "M29.57,72.13c10.67-0.75,37.1-3.03,46.93-3.88c4.23-0.37,6,1.12,4.89,6.04c-0.96,4.24-2.18,10-3.66,16.65",
            "M44.36,72.12c0.63,0.63,1.13,1.37,1.19,2.45c0.2,3.56,0.57,10.69,1.1,17.34",
            "M62.9,70.87c0.49,0.49,0.8,1.7,0.73,2.36c-0.38,3.15-1,11.15-1.89,18.42",
            "M13.88,94.23c3,0.52,6.65,0.81,9.75,0.55C40,93.38,62.5,92,86.88,91.09c3.58-0.13,7.23-0.03,10.75,0.73" };

    public void testParsePath() {
        String path = "M27.34,70.6c0.97,0.97,1.34,1.76,1.44,2.77s1.47,13,2.07,19.63";
        StrokePath strokePath = StrokePath.parsePath(path);
        assertEquals(2, strokePath.getCurves().size());

        assertTrue(strokePath.getMoveTo().equals(27.34f, 70.6f));

        Curve c = strokePath.getCurves().get(0);
        assertFalse(c.isSmooth());
        assertTrue(c.isRelative());
        assertTrue(c.getP1().equals(0.97f, 0.97f));
        assertTrue(c.getP2().equals(1.34f, 1.76f));
        assertTrue(c.getP3().equals(1.44f, 2.77f));

        c = strokePath.getCurves().get(1);
        assertTrue(c.isSmooth());
        assertTrue(c.isRelative());
        assertNull(c.getP1());
        assertTrue(c.getP2().equals(1.47f, 13f));
        assertTrue(c.getP3().equals(2.07f, 19.63f));
    }

    public void testParseKanji() {
        for (String pathStr : strokes) {
            StrokePath strokePath = StrokePath.parsePath(pathStr);
            assertFalse(strokePath.getCurves().isEmpty());
        }
    }

    public void testParseAll() throws Exception {
        StrokePath.tryParseKangiVgXml(new File(
        //                "/sdcard/wwwjdic/kanjivg-20100513.xml"));
                "/sdcard/wwwjdic/kanjivg-20100823.xml"));
    }
}
