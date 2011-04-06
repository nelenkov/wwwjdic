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

    // static {
    // java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(
    // java.util.logging.Level.FINEST);
    // java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(
    // java.util.logging.Level.FINEST);
    // java.util.logging.Logger.getLogger("httpclient.wire.header").setLevel(
    // java.util.logging.Level.FINEST);
    // java.util.logging.Logger.getLogger("httpclient.wire.content").setLevel(
    // java.util.logging.Level.FINEST);
    //
    // System.setProperty("org.apache.commons.logging.Log",
    // "org.apache.commons.logging.impl.SimpleLog");
    // System.setProperty("org.apache.commons.logging.simplelog.showdatetime",
    // "true");
    // System.setProperty(
    // "org.apache.commons.logging.simplelog.log.httpclient.wire",
    // "debug");
    // System.setProperty(
    // "org.apache.commons.logging.simplelog.log.org.apache.http",
    // "debug");
    // System.setProperty(
    // "org.apache.commons.logging.simplelog.log.org.apache.http.headers",
    // "debug");
    // }

    public EntityBasedHttpClient(String endpoint, int timeout) {
        this.url = endpoint;

        HttpParams params = createHttpParams(timeout);
        httpClient = new DefaultHttpClient(params);
    }

    protected HttpParams createHttpParams(int timeout) {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setContentCharset(params,
                HTTP.DEFAULT_CONTENT_CHARSET);
        HttpProtocolParams.setUseExpectContinue(params, true);
        HttpConnectionParams.setConnectionTimeout(params, timeout);
        HttpProtocolParams.setUserAgent(params,
                WwwjdicApplication.getUserAgentString());

        return params;
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
