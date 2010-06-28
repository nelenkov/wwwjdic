package org.nick.wwwjdic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import android.util.Log;

public abstract class BackdoorTranslateTask<T> extends TranslateTask<T> {

    protected static final Pattern PRE_START_PATTERN = Pattern
            .compile("^<pre>.*$");

    protected static final Pattern PRE_END_PATTERN = Pattern
            .compile("^</pre>.*$");

    public BackdoorTranslateTask(String url, int timeoutSeconds,
            ResultListView<T> resultListView, SearchCriteria criteria) {
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
                T entry = parseEntry(line);
                result.add(entry);
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

    protected abstract String generateBackdoorCode(SearchCriteria criteria);

}
