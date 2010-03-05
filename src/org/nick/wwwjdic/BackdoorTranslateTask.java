package org.nick.wwwjdic;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import android.util.Log;

public class BackdoorTranslateTask extends TranslateTask {

    private static final String BACKDOOR_URL = "http://www.csse.monash.edu.au/~jwb/cgi-bin/wwwjdic.cgi";

    private static final Pattern WORD_PATTERN = Pattern.compile("^<br>(.+)$");
    private static final Pattern KANJI_PATTERN = Pattern
            .compile("</HEAD><BODY><br>(.+)$");

    public BackdoorTranslateTask(ResultListView resultListView,
            SearchCriteria criteria) {
        super(resultListView, criteria);
    }

    @Override
    protected List<DictionaryEntry> parseResult(String html) {
        List<DictionaryEntry> result = new ArrayList<DictionaryEntry>();

        Pattern pattern = selectPattern();
        String[] lines = html.split("\n");
        for (String line : lines) {
            Matcher m = pattern.matcher(line);
            if (m.matches()) {
                DictionaryEntry entry = DictionaryEntry.parseEdict(m.group(1)
                        .trim());
                // String word = m.group(1).trim().replaceAll("<.+?>", "");
                // String translation = m.group(2).trim().replaceAll("<.+?>",
                // "");

                result.add(entry);
            }
        }

        return result;
    }

    private Pattern selectPattern() {
        if (searchCriteria.isKanjiLookup()) {
            return KANJI_PATTERN;
        }

        return WORD_PATTERN;
    }

    @Override
    protected String query(SearchCriteria criteria) {
        try {
            String lookupUrl = String.format("%s?%s%s", BACKDOOR_URL,
                    generateBackdoorCode(criteria), URLEncoder.encode(criteria
                            .getQueryString(), "UTF-8"));
            HttpGet get = new HttpGet(lookupUrl);

            String responseStr = httpclient.execute(get, responseHandler,
                    localContext);

            return responseStr;
        } catch (ClientProtocolException cpe) {
            Log.e("WWWJDIC", "ClientProtocolException", cpe);
            throw new RuntimeException(cpe);
        } catch (IOException e) {
            Log.e("WWWJDIC", "IOException", e);
            throw new RuntimeException(e);
        }
    }

    private String generateBackdoorCode(SearchCriteria criteria) {
        StringBuffer buff = new StringBuffer();
        buff.append(criteria.getDictionary());
        // raw
        buff.append("Z");
        if (criteria.isKanjiLookup()) {
            buff.append("M");
        } else {
            // unicode
            buff.append("U");
        }
        if (criteria.isExactMatch()) {
            buff.append("Q");
        } else {
            if (criteria.isKanjiLookup()) {
                buff.append("J");
            } else {
                // English/Kanji
                buff.append("E");
                // for romanized Japanese
                // buff.append("J");
            }
        }

        return buff.toString();
    }

}
