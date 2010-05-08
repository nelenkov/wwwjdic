package org.nick.wwwjdic;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class KanjiTranslateTask extends BackdoorTranslateTask<KanjiEntry> {

    public KanjiTranslateTask(String url, int timeoutSeconds,
            ResultListView resultListView, SearchCriteria criteria) {
        super(url, timeoutSeconds, resultListView, criteria);
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
        try {
            buff.append(URLEncoder.encode(criteria.getQueryString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        // stroke count
        if (criteria.hasStrokes()) {
            buff.append("=");
        }
        if (criteria.hasMinStrokes()) {
            buff.append(criteria.getMinStrokeCount().toString());
        }
        if (criteria.hasMaxStrokes()) {
            buff.append("-");
            buff.append(criteria.getMaxStrokeCount().toString());
        }
        if (criteria.hasStrokes()) {
            buff.append("=");
        }

        return buff.toString();
    }
}
