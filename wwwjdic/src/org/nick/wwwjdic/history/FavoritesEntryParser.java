package org.nick.wwwjdic.history;

import org.nick.wwwjdic.DictionaryEntry;
import org.nick.wwwjdic.KanjiEntry;
import org.nick.wwwjdic.StringUtils;
import org.nick.wwwjdic.WwwjdicEntry;

public class FavoritesEntryParser {

    private static final int NUM_FIELDS = 4;

    public static final int TYPE_IDX = 0;
    public static final int HEADWORD_IDX = 1;
    public static final int DICT_STR_IDX = 2;
    public static final int TIME_IDX = 3;

    private static final int DICT_DETAILS_NUM_FIELDS = 3;

    private static final int KANJI_DETAILS_NUM_FIELDS = 17;

    private static final int TYPE_DICT = 0;
    private static final int TYPE_KANJI = 1;

    private FavoritesEntryParser() {
    }

    public static String[] toStringArray(WwwjdicEntry entry, long time) {
        String[] result = new String[NUM_FIELDS];
        result[TYPE_IDX] = entry.isKanji() ? "1" : "0";
        result[HEADWORD_IDX] = entry.getHeadword();
        result[DICT_STR_IDX] = entry.getDictString();
        result[TIME_IDX] = Long.toString(time);

        return result;
    }

    public static String[] toParsedStringArray(WwwjdicEntry entry) {
        if (entry.isKanji()) {
            return generateKanjiCsv((KanjiEntry) entry);
        }

        return generateDictCsv((DictionaryEntry) entry);
    }

    private static String[] generateKanjiCsv(KanjiEntry entry) {
        String[] result = new String[KANJI_DETAILS_NUM_FIELDS];

        int idx = 0;
        result[idx++] = entry.getKanji();

        result[idx++] = entry.getOnyomi();
        result[idx++] = entry.getKunyomi();
        result[idx++] = entry.getNanori();
        result[idx++] = entry.getRadicalName();
        result[idx++] = Integer.toString(entry.getRadicalNumber());
        result[idx++] = Integer.toString(entry.getStrokeCount());
        result[idx++] = toStr(entry.getClassicalRadicalNumber());
        String meaningStr = StringUtils.join(entry.getMeanings(), "\n", 0);
        result[idx++] = meaningStr;

        result[idx++] = entry.getJisCode();
        result[idx++] = entry.getUnicodeNumber();
        result[idx++] = toStr(entry.getFrequncyeRank());
        result[idx++] = toStr(entry.getGrade());
        result[idx++] = toStr(entry.getJlptLevel());
        result[idx++] = entry.getSkipCode();

        result[idx++] = entry.getKoreanReading();
        result[idx++] = entry.getPinyin();

        return result;
    }

    private static String toStr(Integer i) {
        return i == null ? null : i.toString();
    }

    private static String[] generateDictCsv(DictionaryEntry entry) {
        String[] result = new String[DICT_DETAILS_NUM_FIELDS];
        int idx = 0;
        result[idx++] = entry.getWord();
        result[idx++] = entry.getReading();
        result[idx++] = StringUtils.join(entry.getMeanings(), "\n", 0);

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
