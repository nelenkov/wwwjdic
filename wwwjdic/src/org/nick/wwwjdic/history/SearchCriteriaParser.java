package org.nick.wwwjdic.history;

import org.nick.wwwjdic.SearchCriteria;
import org.nick.wwwjdic.StringUtils;

public class SearchCriteriaParser {

    private static final int NUM_FILEDS = 12;

    public static final int TYPE_IDX = 0;
    public static final int QUERY_STRING_IDX = 1;
    public static final int EXACT_MATCH_IDX = 2;
    public static final int KANJI_LOOKUP_IDX = 3;
    public static final int ROMANIZED_JAP_IDX = 4;
    public static final int COMMON_WORDS_ONLY_IDX = 5;
    public static final int DICTIONARY_IDX = 6;
    public static final int KANJI_SEARCH_TYPE_IDX = 7;
    public static final int MIN_STROKE_COUNT_IDX = 8;
    public static final int MAX_STROKE_COUNT_IDX = 9;
    public static final int NUM_MAX_RESULTS_IDX = 10;
    public static final int TIME_IDX = 11;

    private SearchCriteriaParser() {
    }

    public static String[] toStringArray(SearchCriteria criteria, long time) {
        String[] result = new String[NUM_FILEDS];
        result[TYPE_IDX] = Integer.toString(criteria.getType());
        result[QUERY_STRING_IDX] = criteria.getQueryString();
        result[EXACT_MATCH_IDX] = toTfInt(criteria.isExactMatch());
        result[KANJI_LOOKUP_IDX] = toTfInt(criteria.isKanjiLookup());
        result[ROMANIZED_JAP_IDX] = toTfInt(criteria.isRomanizedJapanese());
        result[COMMON_WORDS_ONLY_IDX] = toTfInt(criteria.isCommonWordsOnly());
        result[DICTIONARY_IDX] = criteria.getDictionary();
        result[KANJI_SEARCH_TYPE_IDX] = criteria.getKanjiSearchType();
        result[MIN_STROKE_COUNT_IDX] = toIntStr(criteria.getMinStrokeCount());
        result[MAX_STROKE_COUNT_IDX] = toIntStr(criteria.getMaxStrokeCount());
        result[NUM_MAX_RESULTS_IDX] = criteria.getNumMaxResults() == null ? null
                : Integer.toString(criteria.getNumMaxResults());
        result[TIME_IDX] = Long.toString(time);

        return result;
    }

    private static String toIntStr(Integer i) {
        return i == null ? null : i.toString();
    }

    private static String toTfInt(boolean b) {
        return b ? "1" : "0";
    }

    public static SearchCriteria fromStringArray(String[] record) {
        SearchCriteria result = null;

        int type = Integer.parseInt(record[0]);
        switch (type) {
        case SearchCriteria.CRITERIA_TYPE_DICT:
            result = SearchCriteria.createForDictionary(
                    record[QUERY_STRING_IDX],
                    parseTfStr(record[EXACT_MATCH_IDX]),
                    parseTfStr(record[ROMANIZED_JAP_IDX]),
                    parseTfStr(record[COMMON_WORDS_ONLY_IDX]),
                    record[DICTIONARY_IDX]);
            break;
        case SearchCriteria.CRITERIA_TYPE_KANJI:
            Integer minStrokes = StringUtils
                    .isEmpty(record[MIN_STROKE_COUNT_IDX]) ? null : Integer
                    .parseInt(record[8]);
            Integer maxStrokes = StringUtils
                    .isEmpty(record[MAX_STROKE_COUNT_IDX]) ? null : Integer
                    .parseInt(record[9]);
            result = SearchCriteria.createWithStrokeCount(
                    record[QUERY_STRING_IDX], record[KANJI_SEARCH_TYPE_IDX],
                    minStrokes, maxStrokes);
            break;
        case SearchCriteria.CRITERIA_TYPE_EXAMPLES:
            result = SearchCriteria.createForExampleSearch(
                    record[QUERY_STRING_IDX],
                    parseTfStr(record[EXACT_MATCH_IDX]), Integer
                            .parseInt(record[NUM_MAX_RESULTS_IDX]));
            break;
        default:
            throw new IllegalArgumentException("Unknown criteria type: " + type);
        }

        return result;
    }

    private static boolean parseTfStr(String str) {
        if ("1".equals(str)) {
            return true;
        }

        return false;
    }
}
