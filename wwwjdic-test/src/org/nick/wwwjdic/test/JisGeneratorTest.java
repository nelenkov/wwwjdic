package org.nick.wwwjdic.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.nick.wwwjdic.widgets.JisGenerator;

public class JisGeneratorTest {

    @Test
    public void testGenerate() {
        JisGenerator g = new JisGenerator(true, true);
        String[] unicodeCps = new String[10];
        for (int i = 0; i < unicodeCps.length; i++) {
            unicodeCps[i] = g.selectNextUnicodeCp();
        }
        for (String cp : unicodeCps) {
            System.out.println(cp);
        }
    }

    @Test
    public void testGenerateSequential() {
        JisGenerator g = new JisGenerator(false, true);
        String kanji = g.selectNextKanji();
        assertEquals("ˆŸ", kanji);

        for (int i = 0; i < 94; i++) {
            kanji = g.selectNextKanji();
        }

        assertEquals("‰@", kanji);

        g.setCurrentKanji("˜r");
        kanji = g.selectNextKanji();
        assertEquals("ˆŸ", kanji);

        g = new JisGenerator(false, false);
        kanji = g.selectNextKanji();
        assertEquals("ˆŸ", kanji);

        g.setCurrentKanji("ê¤");
        kanji = g.selectNextKanji();
        assertEquals("ˆŸ", kanji);
    }
}
