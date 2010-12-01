package org.nick.wwwjdic.app.kanjivg;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;

import org.junit.Test;

public class KanjivgParserTest {

    @Test
    public void testParse() throws Exception {
        FileInputStream fis = new FileInputStream(
                "C:/home/nick/android/Wwwjdic/kanjivg/kanjivg3.xml.gz");
        GZIPInputStream in = new GZIPInputStream(fis);
        KanjivgParser parser = new KanjivgParser(in);

        int numKanjis = 0;
        while (parser.hasNext()) {
            Kanji kanji = parser.next();
            if (kanji != null) {
                numKanjis++;
                System.out.println("kanji #" + numKanjis + ": "
                        + kanji.getMidashi() + " " + kanji.getUnicodeNumber()
                        + " (" + kanji.getStrokes().size() + ")");
                if (!kanji.getStrokes().isEmpty()) {
                    System.out.println(kanji.getStrokes().get(0).getPath());
                }
            }
        }

        System.out.println("parsed: " + numKanjis);
    }

    @Test
    public void testFF() throws Exception {
        FileInputStream fis = new FileInputStream(
                "C:/home/nick/android/Wwwjdic/kanjivg/kanjivg1.xml.gz");
        GZIPInputStream in = new GZIPInputStream(fis);
        KanjivgParser parser = new KanjivgParser(in);

        parser.fastForward(30);
        Kanji kanji = parser.next();
        assertEquals("O", kanji.getMidashi());
    }
}
