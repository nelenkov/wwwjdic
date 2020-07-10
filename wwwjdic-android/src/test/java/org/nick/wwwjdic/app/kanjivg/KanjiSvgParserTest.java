package org.nick.wwwjdic.app.kanjivg;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;

import org.junit.Test;


public class KanjiSvgParserTest {

    @Test
    public void testParse() throws Exception {
        try (FileInputStream fis = new FileInputStream(
                    "/home/nick/android/recognizer/model-src/kanjivg-02.xml.gz")) {
            GZIPInputStream in = new GZIPInputStream(fis);
            KanjSvgParser parser = new KanjSvgParser(in);
            Kanji kanji = parser.parse();
            assertEquals("惇", kanji.getMidashi());
            assertEquals(11, kanji.getStrokes().size());
        }
    }
}
