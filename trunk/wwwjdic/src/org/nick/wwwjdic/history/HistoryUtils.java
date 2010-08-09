package org.nick.wwwjdic.history;

import java.util.regex.Pattern;

import org.nick.wwwjdic.R;
import org.nick.wwwjdic.SearchCriteria;

import android.content.Context;

public class HistoryUtils {

    private static final Pattern HEX_PATTERN = Pattern
            .compile("[0-9a-fA-F]{4}");
    private static final int JIS_IDX = 5;

    private HistoryUtils() {
    }

    public static String lookupKanjiSearchName(String kanjiSearchCode,
            String queryString, Context context) {
        String kanjiSearchName = kanjiSearchCode;

        String[] searchCodes = context.getResources().getStringArray(
                R.array.kanji_search_codes_array);
        String[] searchNames = context.getResources().getStringArray(
                R.array.kanji_search_types_array);
        int idx = linearSearch(kanjiSearchCode, searchCodes);

        if (idx != -1 && idx < searchNames.length) {
            kanjiSearchName = searchNames[idx];
        }

        // ugly, but no other way(?) to differentiate between reading search and
        // JIS code search
        if (isJisSearch(kanjiSearchCode, queryString)) {
            kanjiSearchName = searchNames[JIS_IDX];
        }

        return kanjiSearchName;
    }

    private static boolean isJisSearch(String kanjiSearchType,
            String queryString) {
        if (!"J".equals(kanjiSearchType)) {
            return false;
        }

        return HEX_PATTERN.matcher(queryString).matches();
    }

    public static String lookupDictionaryName(SearchCriteria criteria,
            Context context) {
        String dictCode = criteria.getDictionary();
        String dictName = dictCode;

        String[] dictCodes = context.getResources().getStringArray(
                R.array.dictionary_codes_array);
        String[] dictNames = context.getResources().getStringArray(
                R.array.dictionaries_array);
        int idx = linearSearch(dictCode, dictCodes);

        if (idx != -1 && idx < dictNames.length - 1) {
            dictName = dictNames[idx];
        }
        return dictName;
    }

    private static int linearSearch(String key, String[] arr) {
        for (int i = 0; i < arr.length; i++) {
            String code = arr[i];
            if (code.equals(key)) {
                return i;
            }
        }

        return -1;
    }

}
