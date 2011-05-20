package org.nick.wwwjdic;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class DictionarySearchTask extends BackdoorSearchTask<DictionaryEntry> {

    public DictionarySearchTask(String url, int timeoutSeconds,
            ResultListViewBase<DictionaryEntry> resultListView,
            SearchCriteria criteria) {
        super(url, timeoutSeconds, resultListView, criteria);
    }

    @Override
    protected DictionaryEntry parseEntry(String entryStr) {
        SearchCriteria criteria = (SearchCriteria) query;
        return DictionaryEntry.parseEdict(entryStr.trim(),
                criteria.getDictionary());
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
        if (criteria.isKanjiCompoundSearch()) {
            if (criteria.isCommonWordsOnly()) {
                buff.append("P");
            } else {
                if (criteria.isStartingKanjiCompoundSearch()) {
                    buff.append("K");
                } else {
                    buff.append("L");
                }
            }
        } else {
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
        }
        try {
            buff.append(URLEncoder.encode(criteria.getQueryString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        return buff.toString();
    }
}
