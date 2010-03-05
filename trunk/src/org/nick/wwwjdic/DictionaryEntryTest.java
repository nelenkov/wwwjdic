package org.nick.wwwjdic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DictionaryEntryTest {

    @Test
    public void testParse() {
        String edictStr = "先生方 [せんせいがた] /(n) doctors/teachers/";
        DictionaryEntry entry = DictionaryEntry.parseEdict(edictStr);

        assertEquals("先生方", entry.getWord());
        assertEquals("せんせいがた", entry.getReading());
        assertEquals("(n)", entry.getPartOfSpeech());
        assertEquals(2, entry.getMeanings().size());
        assertEquals("doctors", entry.getMeanings().get(0));
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
        assertEquals("(n)", entry.getPartOfSpeech());
        assertEquals(1, entry.getMeanings().size());
        assertEquals("salty dog (cocktail)", entry.getMeanings().get(0));

    }
}
