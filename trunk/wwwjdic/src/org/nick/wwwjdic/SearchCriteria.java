package org.nick.wwwjdic;

import java.io.Serializable;

public class SearchCriteria implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -6703864987202245997L;

    private static final String KANJI_TEXT_LOOKUP_CODE = "J";

    private String queryString;
    private boolean isExactMatch;
    private boolean isKanjiLookup;
    private boolean isRomanizedJapanese;
    private boolean isCommonWordsOnly;
    private String dictionary;
    private String kanjiSearchType;

    public static SearchCriteria createForDictionary(String queryString,
            boolean isExactMatch, boolean isRomanized,
            boolean isCommonWordsOnly, String dictionary) {
        return new SearchCriteria(queryString, isExactMatch, false,
                isRomanized, isCommonWordsOnly, dictionary, null);
    }

    public static SearchCriteria createForKanji(String queryString,
            String searchType) {
        return new SearchCriteria(queryString, false, true, true, false, null,
                searchType);
    }

    private SearchCriteria(String queryString, boolean isExactMatch,
            boolean isKanjiLookup, boolean isRomanizedJapanese,
            boolean isCommonWordsOnly, String dictionary, String kanjiSearchType) {
        this.queryString = queryString;
        this.isExactMatch = isExactMatch;
        this.isKanjiLookup = isKanjiLookup;
        this.isRomanizedJapanese = isRomanizedJapanese;
        this.isCommonWordsOnly = isCommonWordsOnly;
        this.dictionary = dictionary;
        this.kanjiSearchType = kanjiSearchType;
    }

    public String getQueryString() {
        return queryString;
    }

    public boolean isExactMatch() {
        return isExactMatch;
    }

    public boolean isKanjiLookup() {
        return isKanjiLookup;
    }

    public boolean isRomanizedJapanese() {
        return isRomanizedJapanese;
    }

    public boolean isCommonWordsOnly() {
        return isCommonWordsOnly;
    }

    public String getDictionary() {
        return dictionary;
    }

    public String getKanjiSearchType() {
        return kanjiSearchType;
    }

    public boolean isKanjiCodeLookup() {
        return !KANJI_TEXT_LOOKUP_CODE.equals(kanjiSearchType);
    }

}
