package org.nick.wwwjdic;

public class KanjiTranslateTask extends BackdoorTranslateTask<KanjiEntry> {

    public KanjiTranslateTask(ResultListView resultListView,
            SearchCriteria criteria) {
        super(resultListView, criteria);
    }

    @Override
    protected KanjiEntry parseEntry(String entryStr) {
        return KanjiEntry.parseKanjidic(entryStr.trim());
    }

    @Override
    protected String generateBackdoorCode(SearchCriteria criteria) {
        StringBuffer buff = new StringBuffer();
        // always "1" for kanji?
        buff.append("1");
        // raw
        buff.append("Z");
        if (criteria.isKanjiCodeLookup()) {
            buff.append("K");
        } else {
            buff.append("M");
        }
        buff.append(criteria.getKanjiSearchType());

        return buff.toString();
    }
}
