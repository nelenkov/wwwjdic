package org.nick.wwwjdic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import android.util.Log;

public abstract class BackdoorTranslateTask<T> extends TranslateTask {

    private static final String BACKDOOR_URL = "http://www.csse.monash.edu.au/~jwb/cgi-bin/wwwjdic.cgi";

    protected static final Pattern PRE_START_PATTERN = Pattern
            .compile("^<pre>.*$");

    protected static final Pattern PRE_END_PATTERN = Pattern
            .compile("^</pre>.*$");

    public BackdoorTranslateTask(ResultListView resultListView,
            SearchCriteria criteria) {
        super(resultListView, criteria);
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
    protected String query(SearchCriteria criteria) {
        try {
            String lookupUrl = String.format("%s?%s", BACKDOOR_URL,
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
