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
import org.nick.wwwjdic.DictionaryEntry;
import org.nick.wwwjdic.history.FavoritesEntryParser;

public class DictionaryEntryTest {

    @Test
    public void testParse() {
        String edictStr = "先生方 [せんせいがた] /(n) doctors/teachers/";
        DictionaryEntry entry = DictionaryEntry.parseEdict(edictStr);

        assertEquals("先生方", entry.getWord());
        assertEquals("せんせいがた", entry.getReading());
        assertEquals(2, entry.getMeanings().size());
        assertEquals("(n) doctors", entry.getMeanings().get(0));
        assertEquals("teachers", entry.getMeanings().get(1));
        assertEquals("[せんせいがた]  (n) doctors teachers", entry
                .getTranslationString());
    }

    @Test
    public void testNoReading() {
        String edictStr = "ソルティドッグ /(n) salty dog (cocktail)/";
        DictionaryEntry entry = DictionaryEntry.parseEdict(edictStr);

        assertEquals("ソルティドッグ", entry.getWord());
        assertNull(entry.getReading());
        assertEquals("(n) salty dog (cocktail)", entry.getTranslationString());
        assertEquals(1, entry.getMeanings().size());
        assertEquals("(n) salty dog (cocktail)", entry.getMeanings().get(0));

    }

    @Test
    public void testEdict() throws Exception {
        System.out.println("Testing EDICT...");
        testEdictFile("/home/nick/android/wwwjdic/wwwjdic-test/dict/edict");
    }

    @Test
    public void testEdict2() throws Exception {
        System.out.println("Testing EDICT2...");
        testEdictFile("/home/nick/android/wwwjdic/wwwjdic-test/dict/edict2");
    }

    @Test
    public void testEdiclsd() throws Exception {
        testEdictFile("/home/nick/android/wwwjdic/wwwjdic-test/dict/ediclsd4");
    }

    @Test
    public void testLawgledt() throws Exception {
        testEdictFile("/home/nick/android/wwwjdic/wwwjdic-test/dict/lawgledt");
    }

    @Test
    public void testCompdic() throws Exception {
        testEdictFile("/home/nick/android/wwwjdic/wwwjdic-test/dict/compdic");
    }

    @Test
    public void testEngscidic() throws Exception {
        testEdictFile("/home/nick/android/wwwjdic/wwwjdic-test/dict/engscidic");
    }

    @Test
    public void testRiverwater() throws Exception {
        testEdictFile("/home/nick/android/wwwjdic/wwwjdic-test/dict/riverwater");
    }

    @Test
    public void testBuddhdic() throws Exception {
        testEdictFile("/home/nick/android/wwwjdic/wwwjdic-test/dict/Buddhdic_jp_euc.txt");
    }

    @Test
    public void testFindic() throws Exception {
        testEdictFile("/home/nick/android/wwwjdic/wwwjdic-test/dict/findic");
    }

    @Test
    public void testLingdic() throws Exception {
        testEdictFile("/home/nick/android/wwwjdic/wwwjdic-test/dict/lingdic");
    }

    private void testEdictFile(String filename) throws Exception {
        System.out.println("Testing " + filename);

        FileInputStream fis = new FileInputStream(filename);
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
            DictionaryEntry e = DictionaryEntry.parseEdict(l);
            assertNotNull(e);
            assertNotNull(e.getWord());
            assertNotNull(e.getTranslationString());

            String[] fields = FavoritesEntryParser.toParsedStringArray(e);
            assertNotNull(fields);
            assertEquals(3, fields.length);
            assertEquals(e.getWord(), fields[0]);
            assertEquals(e.getReading(), fields[1]);
        }
    }
}
