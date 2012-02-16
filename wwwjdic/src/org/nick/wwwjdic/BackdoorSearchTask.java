package org.nick.wwwjdic;

import static org.nick.wwwjdic.WwwjdicPreferences.WWWJDIC_DEBUG;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.nick.wwwjdic.model.SearchCriteria;
import org.nick.wwwjdic.model.WwwjdicQuery;
import org.nick.wwwjdic.utils.StringUtils;

import android.util.Log;

public abstract class BackdoorSearchTask<T> extends SearchTask<T> {

    private static final String TAG = BackdoorSearchTask.class.getSimpleName();

    protected static final Pattern PRE_START_PATTERN = Pattern
            .compile("^<pre>.*$");

    protected static final Pattern PRE_END_PATTERN = Pattern
            .compile("^</pre>.*$");

    protected static final String PRE_END_TAG = "</pre>";

    protected static final String FONT_TAG = "<font";

    protected static final String MAINTENANCE_MESSAGE = "WWWJDIC is undergoing file maintenance";

    public BackdoorSearchTask(String url, int timeoutSeconds,
            ResultList<T> resultListView, SearchCriteria criteria) {
        super(url, timeoutSeconds, resultListView, criteria);
    }

    @Override
    protected List<T> parseResult(String html) {
        List<T> result = new ArrayList<T>();

        boolean isInPre = false;
        String[] lines = html.split("\n");
        for (String line : lines) {
            if (StringUtils.isEmpty(line)) {
                continue;
            }

            // / XXX handle!
            if (line.contains(MAINTENANCE_MESSAGE)) {
                throw new RuntimeException(line);
            }

            if (line.startsWith(FONT_TAG)) {
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

                if (WWWJDIC_DEBUG) {
                    Log.d(TAG, "dic entry line: " + line);
                }
                T entry = parseEntry(line);
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

    protected abstract T parseEntry(String entryStr);

    @Override
    protected String query(WwwjdicQuery query) {
        try {
            SearchCriteria criteria = (SearchCriteria) query;
            String lookupUrl = String.format("%s?%s", url,
                    generateBackdoorCode(criteria));
            if (WWWJDIC_DEBUG) {
                Log.d(TAG, "WWWJDIC URL: " + lookupUrl);
            }

            HttpGet get = new HttpGet(lookupUrl);
            String responseStr = httpclient.execute(get, responseHandler,
                    localContext);

            return responseStr;
        } catch (ClientProtocolException cpe) {
            Log.e(TAG, "ClientProtocolException", cpe);
            throw new RuntimeException(cpe);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            throw new RuntimeException(e);
        }
    }

    protected abstract String generateBackdoorCode(SearchCriteria criteria);

}
