package org.nick.wwwjdic;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public abstract class TranslateTask implements Runnable {

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

    private static final int TIMEOUT_MILLIS = 15 * 1000;

    public TranslateTask(ResultListView resultView,
            SearchCriteria searchCriteria) {
        this.resultListView = resultView;
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
        try {
            List<DictionaryEntry> result = fetchResult(searchCriteria);
            resultListView.setResult(result);
        } catch (Exception e) {
            Log.e("WWWJDIC", e.getMessage(), e);
            resultListView.setError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<DictionaryEntry> fetchResult(SearchCriteria criteria) {
        String payload = query(criteria);

        return (List<DictionaryEntry>) parseResult(payload);
    }

    protected abstract String query(SearchCriteria criteria);

    protected abstract List<?> parseResult(String html);

}
