package org.nick.wwwjdic.hkr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class KanjiRecognizerClient {

    private static final String TAG = KanjiRecognizerClient.class
            .getSimpleName();

    private static final String USER_AGENT_STRING = "Android-WWWJDIC/0.8";

    private String url;
    private DefaultHttpClient httpClient;

    public KanjiRecognizerClient(String endpoint, int timeout) {
        this.url = endpoint;
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setContentCharset(params,
                HTTP.DEFAULT_CONTENT_CHARSET);
        HttpProtocolParams.setUseExpectContinue(params, true);
        HttpConnectionParams.setConnectionTimeout(params, timeout);
        HttpProtocolParams.setUserAgent(params, USER_AGENT_STRING);
        httpClient = new DefaultHttpClient(params);
    }

    private String createRecognizerRequest(List<Stroke> strokes) {
        StringBuffer buff = new StringBuffer();
        buff.append("H ");

        for (Stroke s : strokes) {
            buff.append(s.toBase36Points());
            buff.append("\n");
        }
        buff.append("\n");

        return buff.toString();
    }

    public String[] recognize(List<Stroke> strokes) throws IOException {
        Log.i(TAG, "Sending handwritten kanji recognition request to " + url);
        HttpPost post = new HttpPost(url);
        String krRequest = createRecognizerRequest(strokes);
        System.out.println(krRequest);
        post.setEntity(new KrEntity(krRequest));

        BufferedReader reader = null;
        try {
            HttpResponse resp = httpClient.execute(post);
            reader = new BufferedReader(new InputStreamReader(resp.getEntity()
                    .getContent(), "utf-8"));

            String response = readAllLines(reader);
            JSONObject jsonObj = new JSONObject(response);
            String status = jsonObj.getString("status");
            if (!"OK".equals(status)) {
                throw new RuntimeException("Error calling kanji recognizer: "
                        + status);
            }

            int numResults = jsonObj.getInt("total_results");
            JSONArray jsonArr = jsonObj.getJSONArray("results");
            String[] result = new String[jsonArr.length()];
            for (int i = 0; i < result.length; i++) {
                result[i] = (String) jsonArr.get(i);
            }

            return result;
        } catch (HttpResponseException re) {
            Log.e(TAG, "HTTP response exception", re);
            throw new RuntimeException("HTTP request failed. Status: "
                    + re.getStatusCode());
        } catch (JSONException je) {
            Log.e(TAG, "Error parsing JSON: " + je.getMessage(), je);
            throw new RuntimeException(je.getMessage());
        } finally {
            reader.close();
        }
    }

    private String readAllLines(BufferedReader reader) throws IOException {
        StringBuffer buff = new StringBuffer();
        String line = null;

        while ((line = reader.readLine()) != null) {
            buff.append(line);
            buff.append('\n');
        }

        return buff.toString().trim();
    }
}
