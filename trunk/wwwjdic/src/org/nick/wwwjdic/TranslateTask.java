package org.nick.wwwjdic;

import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import android.util.Log;

public abstract class TranslateTask implements Runnable {

    private static final String TAG = TranslateTask.class.getSimpleName();

    protected ResultListView resultListView;
    protected SearchCriteria searchCriteria;

    protected String url;
    protected int timeoutMillis;

    protected HttpContext localContext;
    protected HttpClient httpclient;
    protected ResponseHandler<String> responseHandler = new GzipStringResponseHandler();

    public TranslateTask(String url, int timeoutSeconds,
            ResultListView resultView, SearchCriteria searchCriteria) {
        this.url = url;
        this.timeoutMillis = timeoutSeconds * 1000;
        this.resultListView = resultView;
        this.searchCriteria = searchCriteria;

        createHttpClient();
    }

    private void createHttpClient() {
        Log.d(TAG, "WWWJDIC URL: " + url);
        Log.d(TAG, "HTTP timeout: " + timeoutMillis);
        httpclient = new DefaultHttpClient();
        HttpParams httpParams = httpclient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, timeoutMillis);
        HttpConnectionParams.setSoTimeout(httpParams, timeoutMillis);
    }

    public void run() {
        try {
            List<DictionaryEntry> result = fetchResult(searchCriteria);
            if (resultListView != null) {
                resultListView.setResult(result);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            resultListView.setError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<DictionaryEntry> fetchResult(SearchCriteria criteria) {
        String payload = query(criteria);

        return (List<DictionaryEntry>) parseResult(payload);
    }

    public ResultListView getResultListView() {
        return resultListView;
    }

    public void setResultListView(ResultListView resultListView) {
        this.resultListView = resultListView;
    }

    protected abstract String query(SearchCriteria criteria);

    protected abstract List<?> parseResult(String html);

}
