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

public class SentenceBreakdownTask extends SearchTask<SentenceBreakdownEntry> {

    private static final Pattern SENTENCE_PART_PATTERN = Pattern.compile(
            "^.*<font color=\"\\S+\">\\S+</font>.*<br>$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern INFLECTED_FORM_PATTERN = Pattern.compile(
            "<font color=\"\\S+\">(\\S+)</font>", Pattern.CASE_INSENSITIVE);

    private static final Pattern ENTRY_WITH_EXPLANATION_PATTERN = Pattern
            .compile("^.*<li>\\s*(.+)<br>\\s*(.+)\\sÅy(.+)Åz\\s+(.+)</li>.*$");
    private static final Pattern ENTRY_PATTERN = Pattern
            .compile("^.*<li>\\s*(.+)\\sÅy(.+)Åz\\s+(.+)\\s+(<font.*>(.+)</font>)?</li>.*$");
    private static final Pattern NO_READING_ENTRY_PATTERN = Pattern
            .compile("^.*<li>\\s*(\\S+)\\s+(.+)\\s+(<font.*>(.+)</font>)?</li>.*$");

    private static final Pattern BR_PATTERN = Pattern.compile("^<br>$");

    public SentenceBreakdownTask(String url, int timeoutSeconds,
            ResultListViewBase<SentenceBreakdownEntry> resultListView,
            WwwjdicQuery query) {
        super(url, timeoutSeconds, resultListView, query);
    }

    @Override
    protected List<SentenceBreakdownEntry> parseResult(String html) {
        List<SentenceBreakdownEntry> result = new ArrayList<SentenceBreakdownEntry>();
        List<String> inflectedForms = new ArrayList<String>();

        String[] lines = html.split("\n");
        boolean exampleFollows = false;
        int wordIdx = 0;
        for (String line : lines) {
            Matcher m = BR_PATTERN.matcher(line);
            if (m.matches()) {
                exampleFollows = true;
                continue;
            }

            if (exampleFollows) {
                m = INFLECTED_FORM_PATTERN.matcher(line);
                while (m.find()) {
                    inflectedForms.add(m.group(1));
                }

                exampleFollows = false;
                continue;
            }

            m = SENTENCE_PART_PATTERN.matcher(line);
            if (m.matches()) {
                m = INFLECTED_FORM_PATTERN.matcher(line);
                while (m.find()) {
                    inflectedForms.add(m.group(1));
                }
                continue;
            }

            m = ENTRY_WITH_EXPLANATION_PATTERN.matcher(line);
            if (m.matches()) {
                String explanation = m.group(1).trim();
                String word = m.group(2).trim();
                String reading = m.group(3).trim();
                String translation = m.group(4).trim();
                SentenceBreakdownEntry entry = SentenceBreakdownEntry
                        .createWithExplanation(inflectedForms.get(wordIdx),
                                word, reading, translation, explanation);
                result.add(entry);
                wordIdx++;
                continue;
            }

            m = ENTRY_PATTERN.matcher(line);
            if (m.matches()) {
                String word = m.group(1).trim();
                String reading = m.group(2).trim();
                String translation = m.group(3).trim();

                SentenceBreakdownEntry entry = null;
                if (m.groupCount() > 4 && m.group(5) != null) {
                    String explanation = m.group(5).trim();
                    entry = SentenceBreakdownEntry.createWithExplanation(
                            inflectedForms.get(wordIdx), word, reading,
                            translation, explanation);
                } else {
                    entry = SentenceBreakdownEntry.create(inflectedForms
                            .get(wordIdx), word, reading, translation);
                }
                result.add(entry);
                wordIdx++;
                continue;
            }
            m = NO_READING_ENTRY_PATTERN.matcher(line);
            if (m.matches()) {
                String word = m.group(1).trim();
                String translation = m.group(2).trim();

                SentenceBreakdownEntry entry = null;
                if (m.groupCount() > 3 && m.group(4) != null) {
                    String explanation = m.group(4).trim();
                    entry = SentenceBreakdownEntry.createWithExplanation(
                            inflectedForms.get(wordIdx), word, null,
                            translation, explanation);
                } else {
                    entry = SentenceBreakdownEntry.createNoReading(
                            inflectedForms.get(wordIdx), word, translation);
                }
                result.add(entry);
                wordIdx++;
                continue;
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
