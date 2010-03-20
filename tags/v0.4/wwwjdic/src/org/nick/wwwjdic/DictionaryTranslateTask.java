package org.nick.wwwjdic;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class DictionaryTranslateTask extends
        BackdoorTranslateTask<DictionaryEntry> {

    public DictionaryTranslateTask(ResultListViewBase resultListView,
            SearchCriteria criteria) {
        super(resultListView, criteria);
    }

    @Override
    protected DictionaryEntry parseEntry(String entryStr) {
        return DictionaryEntry.parseEdict(entryStr.trim());
    }

    @Override
    protected String generateBackdoorCode(SearchCriteria criteria) {
        StringBuffer buff = new StringBuffer();
        buff.append(criteria.getDictionary());
        // raw
        buff.append("Z");

        // search type
        if (criteria.isRomanizedJapanese()) {
            // ASCII, etc.
            buff.append("D");
        } else {
            // Unicode
            buff.append("U");
        }

        // key type
        if (criteria.isExactMatch() && !criteria.isCommonWordsOnly()) {
            buff.append("Q");
        } else if (criteria.isExactMatch() && criteria.isCommonWordsOnly()) {
            buff.append("R");
        } else if (!criteria.isExactMatch() && criteria.isCommonWordsOnly()) {
            buff.append("P");
        } else {
            if (criteria.isRomanizedJapanese()) {
                // Japanese
                buff.append("J");
            } else {
                // English
                buff.append("E");
            }
        }
        try {
            buff.append(URLEncoder.encode(criteria.getQueryString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        return buff.toString();
    }
}
