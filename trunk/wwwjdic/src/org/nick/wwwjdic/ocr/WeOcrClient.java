package org.nick.wwwjdic.ocr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.nick.wwwjdic.EntityBasedHttpClient;

import android.graphics.Bitmap;
import android.util.Log;

public class WeOcrClient extends EntityBasedHttpClient {

    private static final String TAG = WeOcrClient.class.getSimpleName();

    private static final String ECLASS_CHAR = "character";

    public WeOcrClient(String endpoint, int timeout) {
        super(endpoint, timeout);
    }

    public String sendLineOcrRequest(Bitmap img) throws IOException {
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

    public String[] sendCharacterOcrRequest(Bitmap img, int numCandidates)
            throws IOException {
        Log.i(TAG, "Sending OCR request to " + url);
        HttpPost post = createPost(img, ECLASS_CHAR, numCandidates);

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

            String responseStr = readAllLines(reader);
            Log.d(TAG, "WeOCR response: " + responseStr);
            List<String> result = extractCandidates(responseStr);

            return result.toArray(new String[result.size()]);
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

    private List<String> extractCandidates(String responseStr) {
        List<String> result = new ArrayList<String>();

        String[] lines = responseStr.split("\n");
        for (String l : lines) {
            String line = l.trim();
            if (line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("R")) {
                String[] fields = line.split("\t");
                result.add(fields[2]);
            }
        }

        return result;
    }

    @Override
    protected AbstractHttpEntity createEntity(Object... params)
            throws IOException {
        if (params.length == 1) {
            return new OcrFormEntity((Bitmap) params[0]);
        }

        return new OcrFormEntity((Bitmap) params[0], (String) params[1],
                (Integer) params[2]);

    }
}
