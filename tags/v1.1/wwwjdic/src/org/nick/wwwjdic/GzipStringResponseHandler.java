/**
 * 
 */
package org.nick.wwwjdic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

public class GzipStringResponseHandler implements ResponseHandler<String> {

    public String handleResponse(HttpResponse response)
            throws ClientProtocolException, IOException {
        HttpEntity entity = response.getEntity();
        Header contentEncoding = response.getFirstHeader("Content-Encoding");
        String responseStr = null;
        if (contentEncoding != null
                && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
            GZIPInputStream gz = new GZIPInputStream(entity.getContent());
            ByteArrayOutputStream arr = new ByteArrayOutputStream();
            byte[] buff = new byte[1024];
            int len;
            while ((len = gz.read(buff)) > 0) {
                arr.write(buff, 0, len);
            }

            responseStr = new String(arr.toByteArray(), "UTF-8");
        } else {
            if (entity != null) {
                responseStr = EntityUtils.toString(entity);
            }
        }

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return responseStr;
        }

        throw new RuntimeException("Server error: " + responseStr);
    }
}
