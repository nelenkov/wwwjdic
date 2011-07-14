package org.nick.wwwjdic;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public class HttpClientFactory {

    private static final String TAG = HttpClientFactory.class.getSimpleName();

    private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    private static final String ENCODING_GZIP = "gzip";
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String HEADER_PRAGMA = "Pragma";
    private static final String NO_CACHE = "no-cache";


    private HttpClientFactory() {
    }

    public static HttpClient createWwwjdicHttpClient(int timeoutMillis) {
        Log.d(TAG, "HTTP timeout: " + timeoutMillis);

        DefaultHttpClient result = new DefaultHttpClient();
        HttpParams httpParams = result.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, timeoutMillis);
        HttpConnectionParams.setSoTimeout(httpParams, timeoutMillis);
        HttpProtocolParams.setUserAgent(httpParams,
                WwwjdicApplication.getUserAgentString());

        result.addRequestInterceptor(new HttpRequestInterceptor() {
            public void process(HttpRequest request, HttpContext context) {
                if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
                    request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
                }
                if (!request.containsHeader(HEADER_CACHE_CONTROL)) {
                    request.addHeader(HEADER_CACHE_CONTROL, NO_CACHE);
                }
                if (!request.containsHeader(HEADER_PRAGMA)) {
                    request.addHeader(HEADER_PRAGMA, NO_CACHE);
                }
            }
        });

        result.addResponseInterceptor(new HttpResponseInterceptor() {
            public void process(HttpResponse response, HttpContext context) {
                // Inflate any responses compressed with gzip
                final HttpEntity entity = response.getEntity();
                final Header encoding = entity.getContentEncoding();
                if (encoding != null) {
                    for (HeaderElement element : encoding.getElements()) {
                        if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
                            response.setEntity(new InflatingEntity(response
                                    .getEntity()));
                            break;
                        }
                    }
                }
            }
        });

        return result;
    }

    public static ResponseHandler<String> createWwwjdicResponseHandler() {
        return new StringResponseHandler();
    }

    static class InflatingEntity extends HttpEntityWrapper {
        public InflatingEntity(HttpEntity wrapped) {
            super(wrapped);
        }

        @Override
        public InputStream getContent() throws IOException {
            return new GZIPInputStream(wrappedEntity.getContent());
        }

        @Override
        public long getContentLength() {
            return -1;
        }
    }

    static class StringResponseHandler implements ResponseHandler<String> {

        public String handleResponse(HttpResponse response)
                throws ClientProtocolException, IOException {
            HttpEntity entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                if (entity != null) {
                    entity.consumeContent();
                }
                throw new RuntimeException("Server error: "
                        + response.getStatusLine());
            }

            String responseStr = null;
            if (entity != null) {
                responseStr = EntityUtils.toString(entity);
            }

            return responseStr;
        }
    }
}
