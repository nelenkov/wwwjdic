package org.nick.wwwjdic;

import java.io.Serializable;

public class SearchCriteria implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -6703864987202245997L;

    private static final String KANJI_TEXT_LOOKUP_CODE = "J";
    private static final String KANJI_RADICAL_LOOKUP_CODE = "B";

    private Long id;

    private String queryString;
    private boolean isExactMatch;
    private boolean isKanjiLookup;
    private boolean isRomanizedJapanese;
    private boolean isCommonWordsOnly;
    private String dictionary;
    private String kanjiSearchType;
    private Integer minStrokeCount;
    private Integer maxStrokeCount;

    public static SearchCriteria createForDictionary(String queryString,
            boolean isExactMatch, boolean isRomanized,
            boolean isCommonWordsOnly, String dictionary) {
        return new SearchCriteria(queryString, isExactMatch, false,
                isRomanized, isCommonWordsOnly, dictionary, null, null, null);
    }

    public static SearchCriteria createForKanji(String queryString,
            String searchType) {
        return new SearchCriteria(queryString, false, true, true, false, null,
                searchType, null, null);
    }

    public static SearchCriteria createWithStrokeCount(String queryString,
            String searchType, Integer minStrokeCount, Integer maxStrokeCount) {
        return new SearchCriteria(queryString, false, true, true, false, null,
                searchType, minStrokeCount, maxStrokeCount);
    }

    private SearchCriteria(String queryString, boolean isExactMatch,
            boolean isKanjiLookup, boolean isRomanizedJapanese,
            boolean isCommonWordsOnly, String dictionary,
            String kanjiSearchType, Integer minStrokeCount,
            Integer maxStrokeCount) {
        this.queryString = queryString;
        this.isExactMatch = isExactMatch;
        this.isKanjiLookup = isKanjiLookup;
        this.isRomanizedJapanese = isRomanizedJapanese;
        this.isCommonWordsOnly = isCommonWordsOnly;
        this.dictionary = dictionary;
        this.kanjiSearchType = kanjiSearchType;
        this.minStrokeCount = minStrokeCount;
        this.maxStrokeCount = maxStrokeCount;
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

    public boolean isKanjiRadicalLookup() {
        return KANJI_RADICAL_LOOKUP_CODE.equals(kanjiSearchType);
    }

    public Integer getMinStrokeCount() {
        return minStrokeCount;
    }

    public Integer getMaxStrokeCount() {
        return maxStrokeCount;
    }

    public boolean hasStrokes() {
        return minStrokeCount != null || maxStrokeCount != null;
    }

    public boolean hasMinStrokes() {
        return minStrokeCount != null;
    }

    public boolean hasMaxStrokes() {
        return maxStrokeCount != null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isNarrowedDown() {
        return isExactMatch || isCommonWordsOnly;
    }

}
