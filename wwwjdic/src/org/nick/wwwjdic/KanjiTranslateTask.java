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
}
