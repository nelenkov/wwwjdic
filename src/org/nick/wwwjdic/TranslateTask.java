package org.nick.wwwjdic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public class TranslateTask implements Runnable {

    private static class StringResponseHandler implements
            ResponseHandler<String> {
        public String handleResponse(HttpResponse response)
                throws ClientProtocolException, IOException {
            HttpEntity entity = response.getEntity();

            String responseStr = null;
            if (entity != null) {
                responseStr = EntityUtils.toString(entity);
            }

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return responseStr;
            }

            throw new RuntimeException("Server error: " + responseStr);
        }
    };

    protected ResultListView resultListView;
    protected SearchCriteria searchCriteria;

    protected HttpContext localContext;
    protected HttpClient httpclient;
    protected StringResponseHandler responseHandler = new StringResponseHandler();

    private static final String ACTION_URL = "http://www.csse.monash.edu.au/~jwb/cgi-bin/wwwjdic.cgi?1E";
    private static final int TIMEOUT_MILLIS = 15 * 1000;

    private static final Pattern resultPattern = Pattern
    // .compile("^<font size=\"\\+1\">(.+)</font>(.+)</label>.*$");
            .compile("^<font size=\"\\+1\">(.+)</font>(.+)$");

    public TranslateTask(ResultListView wwwjdic, SearchCriteria searchCriteria) {
        this.resultListView = wwwjdic;
        this.searchCriteria = searchCriteria;

        createHttpClient();
    }

    private void createHttpClient() {
        httpclient = new DefaultHttpClient();
        HttpParams httpParams = httpclient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_MILLIS);
        HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_MILLIS);

    }

    public void run() {
        List<DictionaryEntry> result = fetchResult(searchCriteria);
        resultListView.setResult(result);
    }

    private List<DictionaryEntry> fetchResult(SearchCriteria criteria) {
        try {
            String payload = query(criteria);

            return parseResult(payload);
        } catch (Exception e) {
            Log.e("WWWJDIC", "IOException", e);

            List<String> result = new ArrayList<String>();
            result.add("Error fetching search result.");

            // XXX
            return null;
        }
    }

    protected String post(String url, List<NameValuePair> params) {
        try {
            HttpPost post = new HttpPost(url);
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(params,
                    "EUC-JP");
            post.setEntity(formEntity);

            String responseStr = httpclient.execute(post, responseHandler,
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

    protected String query(SearchCriteria criteria) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params
                .add(new BasicNameValuePair("dsrchkey", criteria
                        .getQueryString()));
        params.add(new BasicNameValuePair("dsrchtype", "E"));
        params.add(new BasicNameValuePair("dicsel", criteria.getDictionary()));
        Log.i("WWWJDIC", "using dict= " + criteria.getDictionary());

        if (criteria.isExactMatch()) {
            params.add(new BasicNameValuePair("exactm", "exactm"));
        }

        return post(ACTION_URL, params);
    }

    protected List<DictionaryEntry> parseResult(String html) {
        List<DictionaryEntry> result = new ArrayList<DictionaryEntry>();

        String[] lines = html.split("\n");
        for (String line : lines) {
            Matcher m = resultPattern.matcher(line);
            if (m.matches()) {
                String word = m.group(1).trim().replaceAll("<.+?>", "");
                String translation = m.group(2).trim().replaceAll("<.+?>", "");

                // result.add(String.format("%s: %s", word, translation));
            }
        }

        return result;
    }
}
