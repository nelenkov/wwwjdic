package org.nick.wwwjdic;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import android.util.Log;

public class SentenceBreakdownTask extends
        TranslateTask<SentenceBreakdownEntry> {

    private static final Pattern CLOSING_UL_PATTERN = Pattern
            .compile("^.*</ul>.*$");
    private static final Pattern ENTRY_WITH_EXPLANATION_PATTERN = Pattern
            .compile("^.*<li>\\s*(.+)<br>\\s*(.+)\\sÅy(\\S+)Åz\\s\\t(.+)</li>.*$");
    private static final Pattern ENTRY_PATTERN = Pattern
            .compile("^.*<li>\\s*(.+)\\sÅy(\\S+)Åz\\s\\t(.+)</li>.*$");
    // <li> ÇÁÇµÇ¢ (adj,suf) appears like; ; KD </li>
    private static final Pattern NO_READING_ENTRY_PATTERN = Pattern
            .compile("^.*<li>\\s*(\\S+)\\s\\t(.+)</li>.*$");

    public SentenceBreakdownTask(String url, int timeoutSeconds,
            ResultListViewBase<SentenceBreakdownEntry> resultListView,
            WwwjdicQuery query) {
        super(url, timeoutSeconds, resultListView, query);
    }

    @Override
    protected List<SentenceBreakdownEntry> parseResult(String html) {
        List<SentenceBreakdownEntry> result = new ArrayList<SentenceBreakdownEntry>();

        String[] lines = html.split("\n");
        for (String line : lines) {
            Matcher m = ENTRY_WITH_EXPLANATION_PATTERN.matcher(line);
            if (m.matches()) {
                String explanation = m.group(1).trim();
                String word = m.group(2).trim();
                String reading = m.group(3).trim();
                String translation = m.group(4).trim();
                SentenceBreakdownEntry entry = SentenceBreakdownEntry
                        .createWithExplanation(word, reading, translation,
                                explanation);
                result.add(entry);
                continue;
            }

            m = ENTRY_PATTERN.matcher(line);
            if (m.matches()) {
                String word = m.group(1).trim();
                String reading = m.group(2).trim();
                String translation = m.group(3).trim();
                SentenceBreakdownEntry entry = SentenceBreakdownEntry.create(
                        word, reading, translation);
                result.add(entry);
                continue;
            }
            m = NO_READING_ENTRY_PATTERN.matcher(line);
            if (m.matches()) {
                String word = m.group(1).trim();
                String translation = m.group(2).trim();
                SentenceBreakdownEntry entry = SentenceBreakdownEntry
                        .createNoReading(word, translation);
                result.add(entry);
                continue;
            }

            m = CLOSING_UL_PATTERN.matcher(line);
            if (m.matches()) {
                break;
            }

        }

        return result;
    }

    @Override
    protected String query(WwwjdicQuery query) {
        try {
            String lookupUrl = String.format("%s?%s", url,
                    generateBackdoorCode(query));
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

    private String generateBackdoorCode(WwwjdicQuery query) {
        StringBuffer buff = new StringBuffer();
        // raw
        buff.append("9ZIG");

        try {
            buff.append(URLEncoder.encode(query.getQueryString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        return buff.toString();
    }

}
