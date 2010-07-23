package org.nick.wwwjdic;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

public abstract class EntityBasedHttpClient {

    protected String url;
    protected DefaultHttpClient httpClient;

    public EntityBasedHttpClient(String endpoint, int timeout) {
        this.url = endpoint;

        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setContentCharset(params,
                HTTP.DEFAULT_CONTENT_CHARSET);
        HttpProtocolParams.setUseExpectContinue(params, true);
        HttpConnectionParams.setConnectionTimeout(params, timeout);
        HttpProtocolParams.setUserAgent(params, WwwjdicApplication
                .getUserAgentString());
        httpClient = new DefaultHttpClient(params);
    }

    protected String readAllLines(BufferedReader reader) throws IOException {
        StringBuffer buff = new StringBuffer();
        String line = null;

        while ((line = reader.readLine()) != null) {
            buff.append(line);
            buff.append('\n');
        }

        return buff.toString().trim();
    }

    protected HttpPost createPost(Object... params) throws IOException {
        HttpPost post = new HttpPost(url);
        post.setEntity(createEntity(params));

        return post;
    }

    protected abstract AbstractHttpEntity createEntity(Object... params)
            throws IOException;

}
