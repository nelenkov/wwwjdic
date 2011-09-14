package org.nick.wwwjdic;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.nick.wwwjdic.utils.StringUtils;

import android.content.Context;
import android.util.Log;

public class WwwjdicClient {

    private static final String TAG = WwwjdicClient.class.getSimpleName();

    private static final Pattern PRE_START_PATTERN = Pattern
            .compile("^<pre>.*$");
    private static final Pattern PRE_END_PATTERN = Pattern
            .compile("^</pre>.*$");
    private static final String PRE_END_TAG = "</pre>";

    private String url;
    private int timeoutMillis;

    private HttpClient httpclient;
    private ResponseHandler<String> responseHandler;

    public WwwjdicClient(Context context) {
        url = WwwjdicPreferences.getWwwjdicUrl(context);
        timeoutMillis = WwwjdicPreferences.getWwwjdicTimeoutSeconds(context) * 1000;
        httpclient = HttpClientFactory.createWwwjdicHttpClient(timeoutMillis);
        responseHandler = HttpClientFactory.createWwwjdicResponseHandler();
    }

    public List<KanjiEntry> findKanji(String kanjiOrReading) {
        SearchCriteria criteria = SearchCriteria
                .createForKanjiOrReading(kanjiOrReading);
        String html = queryKanji(criteria);

        return parseKanji(html);
    }

    private String queryKanji(SearchCriteria criteria) {
        try {
            String lookupUrl = String.format("%s?%s", url,
                    generateKanjiBackdoorCode(criteria));
            Log.d(TAG, "WWWJDIC URL: " + lookupUrl);

            HttpGet get = new HttpGet(lookupUrl);
            String responseStr = httpclient.execute(get, responseHandler);

            return responseStr;
        } catch (ClientProtocolException cpe) {
            Log.e(TAG, "ClientProtocolException", cpe);
            throw new RuntimeException(cpe);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            throw new RuntimeException(e);
        }
    }

    private List<KanjiEntry> parseKanji(String html) {
        List<KanjiEntry> result = new ArrayList<KanjiEntry>();

        boolean isInPre = false;
        String[] lines = html.split("\n");
        for (String line : lines) {
            if (StringUtils.isEmpty(line)) {
                continue;
            }

            Matcher m = PRE_START_PATTERN.matcher(line);
            if (m.matches()) {
                isInPre = true;
                continue;
            }

            m = PRE_END_PATTERN.matcher(line);
            if (m.matches()) {
                break;
            }

            if (isInPre) {
                boolean hasEndPre = false;
                // some entries have </pre> on the same line
                if (line.contains(PRE_END_TAG)) {
                    hasEndPre = true;
                    line = line.replaceAll(PRE_END_TAG, "");
                }
                Log.d(TAG, "dic entry line: " + line);
                KanjiEntry entry = parseKanjiEntry(line);
                if (entry != null) {
                    result.add(entry);
                }

                if (hasEndPre) {
                    break;
                }
            }
        }

        return result;
    }

    public static String generateKanjiBackdoorCode(SearchCriteria criteria) {
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

    public static String generateDictionaryBackdoorCode(SearchCriteria criteria) {
        StringBuffer buff = new StringBuffer();
        buff.append(criteria.getDictionaryCode());
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
        if (criteria.isKanjiCompoundSearch()) {
            if (criteria.isCommonWordsOnly()) {
                buff.append("P");
            } else {
                if (criteria.isStartingKanjiCompoundSearch()) {
                    buff.append("K");
                } else {
                    buff.append("L");
                }
            }
        } else {
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
        }
        try {
            buff.append(URLEncoder.encode(criteria.getQueryString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        return buff.toString();
    }

    public static String generateExamplesBackdoorCode(SearchCriteria criteria,
            boolean randomExamples) {
        StringBuffer buff = new StringBuffer();
        // dictionary code always 1 for examples?
        buff.append("1");
        // raw
        buff.append("Z");
        // examples
        buff.append("E");
        // Unicode
        buff.append("U");

        try {
            buff.append(URLEncoder.encode(criteria.getQueryString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        if (randomExamples) {
            // use =1= to get random examples
            buff.append("=1=");
        } else {
            // up to 100 sentences starting at 0
            buff.append("=0=");
        }

        return buff.toString();
    }

    private KanjiEntry parseKanjiEntry(String entryStr) {
        return KanjiEntry.parseKanjidic(entryStr.trim());
    }


}
