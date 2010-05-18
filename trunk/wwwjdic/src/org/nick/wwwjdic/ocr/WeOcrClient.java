package org.nick.wwwjdic.ocr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.nick.wwwjdic.EntityBasedHttpClient;

import android.graphics.Bitmap;
import android.util.Log;

public class WeOcrClient extends EntityBasedHttpClient {

    private static final String TAG = WeOcrClient.class.getSimpleName();

    public WeOcrClient(String endpoint, int timeout) {
        super(endpoint, timeout);
    }

    public String sendOcrRequest(Bitmap img) throws IOException {
        Log.i(TAG, "Sending OCR request to " + url);
        HttpPost post = createPost(img);

        BufferedReader reader = null;
        try {
            HttpResponse resp = httpClient.execute(post);
            reader = new BufferedReader(new InputStreamReader(resp.getEntity()
                    .getContent(), "utf-8"));

            String status = reader.readLine();
            if (status.length() != 0) {
                status += readAllLines(reader);
                throw new RuntimeException("WeOCR failed. Status: " + status);
            }

            return readAllLines(reader);
        } catch (HttpResponseException re) {
            Log.e(TAG, "HTTP response exception", re);
            throw new RuntimeException("HTTP request failed. Status: "
                    + re.getStatusCode());
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    @Override
    protected AbstractHttpEntity createEntity(Object param) throws IOException {
        return new OcrFormEntity((Bitmap) param);
    }
}
