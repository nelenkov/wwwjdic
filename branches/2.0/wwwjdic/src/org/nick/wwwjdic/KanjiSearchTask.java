package org.nick.wwwjdic;

import org.nick.wwwjdic.client.WwwjdicClient;


public class KanjiSearchTask extends BackdoorSearchTask<KanjiEntry> {

    public KanjiSearchTask(String url, int timeoutSeconds,
            ResultListView<KanjiEntry> resultListView, SearchCriteria criteria) {
        super(url, timeoutSeconds, resultListView, criteria);
    }

    @Override
    protected KanjiEntry parseEntry(String entryStr) {
        return KanjiEntry.parseKanjidic(entryStr.trim());
    }

    @Override
    protected String generateBackdoorCode(SearchCriteria criteria) {
        return WwwjdicClient.generateKanjiBackdoorCode(criteria);
    }
}
