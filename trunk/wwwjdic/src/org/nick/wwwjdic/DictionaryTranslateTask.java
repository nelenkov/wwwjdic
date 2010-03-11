package org.nick.wwwjdic;


public class DictionaryTranslateTask extends
        BackdoorTranslateTask<DictionaryEntry> {

    public DictionaryTranslateTask(DictionaryResultListView resultListView,
            SearchCriteria criteria) {
        super(resultListView, criteria);
    }

    @Override
    protected DictionaryEntry parseEntry(String entryStr) {
        return DictionaryEntry.parseEdict(entryStr.trim());
    }
}
