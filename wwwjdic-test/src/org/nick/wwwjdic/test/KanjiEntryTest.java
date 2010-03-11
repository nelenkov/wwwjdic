package org.nick.wwwjdic.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.nick.wwwjdic.KanjiEntry;

public class KanjiEntryTest {

    @Test
    public void testParse() {
        String kanjidicStr = "ˆá 3063 U9055 B162 G8 S13 S12 F344 J2 N4720 V6099 H3151 DK2014 L1644 K496 DO883 MN39067P MP11.0144 E1006 IN814 DF965 DC274 DJ385 DG713 DM1659 P3-3-10 I2q10.5 Q3430.4 DR1655 ZSP3-2-10 Ywei2 Wwi ƒC ‚¿‚ª.‚¤ ‚¿‚ª.‚¢ ‚¿‚ª.‚¦‚é -‚¿‚ª.‚¦‚é ‚½‚ª.‚¤ ‚½‚ª.‚¦‚é {difference} {differ} ";
        KanjiEntry entry = KanjiEntry.parseKanjidic(kanjidicStr);
        assertNotNull(entry);

        assertEquals("ˆá", entry.getKanji());
        assertNull(entry.getClassicalRadicalNumber());
        assertEquals(344, entry.getFrequncyeRank().intValue());
        assertEquals(8, entry.getGrade().intValue());
        assertEquals("3063", entry.getJisCode());
        assertEquals(2, entry.getJlptLevel().intValue());
        assertEquals("wi", entry.getKoreanReading());
        assertEquals("wei2", entry.getPinyin());
        assertEquals(162, entry.getRadicalNumber());
        assertEquals("3-3-10", entry.getSkipCode());
        assertEquals(13, entry.getStrokeCount());
        assertEquals("9055", entry.getUnicodeNumber());

        assertEquals("ƒC ‚¿‚ª.‚¤ ‚¿‚ª.‚¢ ‚¿‚ª.‚¦‚é -‚¿‚ª.‚¦‚é ‚½‚ª.‚¤ ‚½‚ª.‚¦‚é", entry.getReading());
        assertEquals("ƒC", entry.getOnyomi());
        assertEquals("‚¿‚ª.‚¤ ‚¿‚ª.‚¢ ‚¿‚ª.‚¦‚é -‚¿‚ª.‚¦‚é ‚½‚ª.‚¤ ‚½‚ª.‚¦‚é", entry.getKunyomi());
        assertEquals(2, entry.getMeanings().size());
        assertEquals("difference", entry.getMeanings().get(0));
        assertEquals("differ", entry.getMeanings().get(1));
    }

    @Test
    public void testKanjidic() throws Exception {
        FileInputStream fis = new FileInputStream(
                "C:/home/nick/android/wwwjdic/wwwjdic-test/dict/kanjidic");
        List<String> lines = new ArrayList<String>();
        BufferedReader r = new BufferedReader(new InputStreamReader(fis,
                "EUC-JP"));
        String line = null;

        while ((line = r.readLine()) != null) {
            lines.add(line);
        }

        r.close();

        for (String l : lines) {
            if (l.charAt(0) == '#') {
                continue;
            }

            System.out.println("parsing : " + l);
            KanjiEntry entry = KanjiEntry.parseKanjidic(l);
            assertNotNull(entry);
            assertNotNull(entry.getKanji());
            assertNotNull(entry.getReading());
            // assertFalse(entry.getMeanings().isEmpty());
        }

    }
}
