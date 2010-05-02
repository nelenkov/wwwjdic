package org.nick.wwwjdic.ocr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import android.graphics.Bitmap;
import android.util.Log;

public class WeOcrClient {

	private static final String TAG = WeOcrClient.class.getSimpleName();

	private static final int HTTP_TIMEOUT = 10 * 1000;

	private String url;
	private DefaultHttpClient httpClient;

	public WeOcrClient(String endpoint) {
		this.url = endpoint;
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setContentCharset(params,
				HTTP.DEFAULT_CONTENT_CHARSET);
		HttpProtocolParams.setUseExpectContinue(params, true);
		HttpConnectionParams.setConnectionTimeout(params, HTTP_TIMEOUT);
		httpClient = new DefaultHttpClient(params);
	}

	public String sendOcrRequest(Bitmap img) throws IOException {
		Log.i(TAG, "Sending OCR request to " + url);
		HttpPost post = new HttpPost(url);
		post.setEntity(new OcrFormEntity(img));

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
