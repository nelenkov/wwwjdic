package org.nick.wwwjdic;

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

        return buff.toString();
    }
}
