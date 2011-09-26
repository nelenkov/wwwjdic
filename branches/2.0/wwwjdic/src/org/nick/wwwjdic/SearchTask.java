package org.nick.wwwjdic;

import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.protocol.HttpContext;
import org.nick.wwwjdic.client.HttpClientFactory;
import org.nick.wwwjdic.model.WwwjdicQuery;

import android.util.Log;

public abstract class SearchTask<T> implements Runnable {

    private static final String TAG = SearchTask.class.getSimpleName();

    protected ResultList<T> resultListView;
    protected WwwjdicQuery query;

    protected String url;
    protected int timeoutMillis;

    protected HttpContext localContext;
    protected HttpClient httpclient;
    protected ResponseHandler<String> responseHandler;

    public SearchTask(String url, int timeoutSeconds,
            ResultList<T> resultView, WwwjdicQuery query) {
        this.url = url;
        this.timeoutMillis = timeoutSeconds * 1000;
        this.resultListView = resultView;
        this.query = query;

        httpclient = HttpClientFactory.createWwwjdicHttpClient(timeoutMillis);
        responseHandler = HttpClientFactory.createWwwjdicResponseHandler();
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

    public ResultList<T> getResultListView() {
        return resultListView;
    }

    public void setResultListView(ResultList<T> resultListView) {
        this.resultListView = resultListView;
    }

    protected abstract String query(WwwjdicQuery criteria);

    protected abstract List<T> parseResult(String html);

}
