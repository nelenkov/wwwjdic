package org.nick.wwwjdic;


public class DictionarySearchTask extends BackdoorSearchTask<DictionaryEntry> {

    public DictionarySearchTask(String url, int timeoutSeconds,
            ResultListFragmentBase<DictionaryEntry> resultListView,
            SearchCriteria criteria) {
        super(url, timeoutSeconds, resultListView, criteria);
    }

    @Override
    protected DictionaryEntry parseEntry(String entryStr) {
        SearchCriteria criteria = (SearchCriteria) query;
        return DictionaryEntry.parseEdict(entryStr.trim(),
                criteria.getDictionaryCode());
    }

    @Override
    protected String generateBackdoorCode(SearchCriteria criteria) {
        return WwwjdicClient.generateDictionaryBackdoorCode(criteria);
    }
}
