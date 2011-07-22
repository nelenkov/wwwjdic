package org.nick.wwwjdic.app.kanjivg;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;

import org.junit.Test;


public class KanjiSvgParserTest {

    @Test
    public void testParse() throws Exception {
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(
                    "C:/home/nick/android/recognizer-misc/kanjivg/SVG/6158.svg");
            KanjSvgParser parser = new KanjSvgParser(fis);
            Kanji kanji = parser.parse();
            assertEquals("œÌ", kanji.getMidashi());
            assertEquals(14, kanji.getStrokes().size());
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }
}
