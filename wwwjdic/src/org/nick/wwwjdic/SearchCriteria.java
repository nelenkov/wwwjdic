package org.nick.wwwjdic;

import java.io.Serializable;

public class SearchCriteria implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -6703864987202245997L;

    private String queryString;
    private boolean isExactMatch;
    private boolean isKanjiLookup;;
    private String dictionary;

    public SearchCriteria(String queryString, boolean isExactMatch,
            boolean isKanjiLookup, String dictionary) {
        this.queryString = queryString;
        this.isExactMatch = isExactMatch;
        this.isKanjiLookup = isKanjiLookup;
        this.dictionary = dictionary;
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

    public String getDictionary() {
        return dictionary;
    }

}
