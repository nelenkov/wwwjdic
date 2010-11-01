package org.nick.wwwjdic.hkr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nick.wwwjdic.EntityBasedHttpClient;

import android.util.Log;

public class KanjiRecognizerClient extends EntityBasedHttpClient {

    private static final String TAG = KanjiRecognizerClient.class
            .getSimpleName();

    public KanjiRecognizerClient(String endpoint, int timeout) {
        // super(endpoint, timeout);
        super("http://kanji.sljfaq.org/kanji.cgi", timeout);
    }

    private String createRecognizerRequest(List<Stroke> strokes,
            boolean useLookAhead) {
        StringBuffer buff = new StringBuffer();
        if (useLookAhead) {
            buff.append("HL ");
        } else {
            buff.append("H ");
        }

        for (Stroke s : strokes) {
            // buff.append(s.toBase36Points());
            buff.append(s.toPoints());
            buff.append("\n");
        }
        buff.append("\n");

        return buff.toString();
    }

    public String[] recognize(List<Stroke> strokes, boolean useLookAhead)
            throws IOException {
        Log.i(TAG, "Sending handwritten kanji recognition request to " + url);

        String krRequest = createRecognizerRequest(strokes, useLookAhead);
        Log.d(TAG, String.format("kanji recognizer request (%d strokes): %s",
                strokes.size(), krRequest));
        HttpPost post = createPost(krRequest);

        BufferedReader reader = null;
        try {
            HttpResponse resp = httpClient.execute(post);
            reader = new BufferedReader(new InputStreamReader(resp.getEntity()
                    .getContent(), "utf-8"));

            String response = readAllLines(reader);
            Log.d(TAG, "kanji recognizer response: " + response);

            String[] result = parseResponse(response);

            return result;
        } catch (HttpResponseException re) {
            Log.e(TAG, "HTTP response exception", re);
            throw new RuntimeException("HTTP request failed. Status: "
                    + re.getStatusCode());
        } catch (JSONException je) {
            Log.e(TAG, "Error parsing JSON: " + je.getMessage(), je);
            throw new RuntimeException(je.getMessage());
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private String[] parseResponse(String response) throws JSONException {
        JSONObject jsonObj = new JSONObject(response);
        String status = jsonObj.getString("status");
        if (!"OK".equals(status)) {
            throw new RuntimeException("Error calling kanji recognizer: "
                    + status);
        }

        JSONArray jsonArr = jsonObj.getJSONArray("results");
        String[] result = new String[jsonArr.length()];
        for (int i = 0; i < result.length; i++) {
            // result[i] = (String) jsonArr.get(i);
            result[i] = (String) ((JSONArray) jsonArr.get(i)).get(0);
        }

        return result;
    }

    @Override
    protected AbstractHttpEntity createEntity(Object... params)
            throws IOException {
        return new KrEntity((String) params[0]);
    }
}
