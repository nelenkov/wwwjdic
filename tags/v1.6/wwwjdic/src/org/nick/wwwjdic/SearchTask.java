package org.nick.wwwjdic;

import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;

import android.util.Log;

public abstract class SearchTask<T> implements Runnable {

    private static final String TAG = SearchTask.class.getSimpleName();

    protected ResultListView<T> resultListView;
    protected WwwjdicQuery query;

    protected String url;
    protected int timeoutMillis;

    protected HttpContext localContext;
    protected HttpClient httpclient;
    protected ResponseHandler<String> responseHandler = new GzipStringResponseHandler();

    public SearchTask(String url, int timeoutSeconds,
            ResultListView<T> resultView, WwwjdicQuery query) {
        this.url = url;
        this.timeoutMillis = timeoutSeconds * 1000;
        this.resultListView = resultView;
        this.query = query;

        createHttpClient();
    }

    private void createHttpClient() {
        Log.d(TAG, "WWWJDIC URL: " + url);
        Log.d(TAG, "HTTP timeout: " + timeoutMillis);
        httpclient = new DefaultHttpClient();
        HttpParams httpParams = httpclient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, timeoutMillis);
        HttpConnectionParams.setSoTimeout(httpParams, timeoutMillis);
        HttpProtocolParams.setUserAgent(httpParams, WwwjdicApplication
                .getUserAgentString());
    }

    public void run() {
        try {
            List<T> result = fetchResult(query);
            if (resultListView != null) {
                resultListView.setResult(result);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            resultListView.setError(e);
        }
    }

    private List<T> fetchResult(WwwjdicQuery query) {
        String payload = query(query);

        return parseResult(payload);
    }

    public ResultListView<T> getResultListView() {
        return resultListView;
    }

    public void setResultListView(ResultListView<T> resultListView) {
        this.resultListView = resultListView;
    }

    protected abstract String query(WwwjdicQuery criteria);

    protected abstract List<T> parseResult(String html);

}
