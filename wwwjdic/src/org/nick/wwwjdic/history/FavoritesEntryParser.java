package org.nick.wwwjdic.history;

import org.nick.wwwjdic.DictionaryEntry;
import org.nick.wwwjdic.KanjiEntry;
import org.nick.wwwjdic.WwwjdicEntry;

public class FavoritesEntryParser {

    private static final int NUM_RECORDS = 4;

    public static final int TYPE_IDX = 0;
    public static final int HEADWORD_IDX = 1;
    public static final int DICT_STR_IDX = 2;
    public static final int TIME_IDX = 3;

    private static final int TYPE_DICT = 0;
    private static final int TYPE_KANJI = 1;

    private FavoritesEntryParser() {
    }

    public static String[] toStringArray(WwwjdicEntry entry, long time) {
        String[] result = new String[NUM_RECORDS];
        result[TYPE_IDX] = entry.isKanji() ? "1" : "0";
        result[HEADWORD_IDX] = entry.getHeadword();
        result[DICT_STR_IDX] = entry.getDictString();
        result[TIME_IDX] = Long.toString(time);

        return result;
    }

    public static WwwjdicEntry fromStringArray(String[] record) {
        WwwjdicEntry result = null;

        int type = Integer.parseInt(record[TYPE_IDX]);
        switch (type) {
        case TYPE_DICT:
            result = DictionaryEntry.parseEdict(record[DICT_STR_IDX]);
            break;
        case TYPE_KANJI:
            result = KanjiEntry.parseKanjidic(record[DICT_STR_IDX]);
            break;
        default:
            throw new IllegalArgumentException("Uknown entry type " + type);

        }

        return result;
    }
}
