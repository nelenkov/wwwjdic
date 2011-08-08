package org.nick.wwwjdic.test;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.nick.wwwjdic.GzipStringResponseHandler;
import org.nick.wwwjdic.utils.Pair;

public class ExampleSearchTest extends TestCase {

    public void testSearchGet() throws Exception {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setContentCharset(params,
                HTTP.DEFAULT_CONTENT_CHARSET);
        HttpProtocolParams.setUseExpectContinue(params, true);
        HttpConnectionParams.setConnectionTimeout(params, 20 * 1000);
        // HttpProtocolParams.setUserAgent(params, "test");
        HttpClient httpClient = new DefaultHttpClient(params);

        String url = "http://www.csse.monash.edu.au/~jwb/cgi-bin/wwwjdic.cgi?1Q";
        HttpGet get = new HttpGet(url + URLEncoder.encode("”L_0__", "EUC-JP"));

        GzipStringResponseHandler handler = new GzipStringResponseHandler();
        String responseStr = httpClient.execute(get, handler);
        System.out.println(responseStr);
    }

    public void testSearch() throws Exception {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setContentCharset(params,
                HTTP.DEFAULT_CONTENT_CHARSET);
        HttpProtocolParams.setUseExpectContinue(params, true);
        HttpConnectionParams.setConnectionTimeout(params, 20 * 1000);
        // HttpProtocolParams.setUserAgent(params, "test");
        HttpClient httpClient = new DefaultHttpClient(params);

        String url = "http://www.csse.monash.edu.au/~jwb/cgi-bin/wwwjdic.cgi?11";
        HttpPost post = new HttpPost(url);
        List<NameValuePair> p = new ArrayList<NameValuePair>();
        NameValuePair pair = new BasicNameValuePair("exsrchstr", "dog");
        p.add(pair);
        pair = new BasicNameValuePair("exsrchnum", "20");
        p.add(pair);

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(p, "EUC-JP");
        post.setEntity(formEntity);

        GzipStringResponseHandler handler = new GzipStringResponseHandler();
        String responseStr = httpClient.execute(post, handler);
        String[] lines = responseStr.split("\n");
        Pattern ulPattern = Pattern.compile("^.*<ul>.*$");
        Pattern closingUlPattern = Pattern.compile("^.*</ul>.*$");
        Pattern liPattern = Pattern.compile("^.*<li>.*$");
        Pattern inputPattern = Pattern.compile("^.*<INPUT.*$");

        final int IN_EXAMPLES_BLOCK = 0;
        final int EXAMPLE_FOLLOWS = 1;
        final int TRANSLATION_FOLLOWS = 2;
        final int EXAMPLES_FINISHED = 3;
        int state = -1;
        String example = null;
        String translation = null;
        List<Pair<String, String>> examples = new ArrayList<Pair<String, String>>();
        for (String line : lines) {
            if (ulPattern.matcher(line).matches()) {
                state = IN_EXAMPLES_BLOCK;
                continue;
            }
            if (liPattern.matcher(line).matches()) {
                state = EXAMPLE_FOLLOWS;
                continue;
            }
            if (inputPattern.matcher(line).matches()) {
                state = TRANSLATION_FOLLOWS;
                continue;
            }
            if (closingUlPattern.matcher(line).matches()) {
                state = EXAMPLES_FINISHED;
                break;
            }

            switch (state) {
            case EXAMPLE_FOLLOWS:
                example = line.trim();
                break;
            case TRANSLATION_FOLLOWS:
                if (example != null) {
                    translation = line.trim();
                    examples
                            .add(new Pair<String, String>(example, translation));
                }
                break;
            default:
                continue;
            }
        }
        assertEquals(20, examples.size());
        for (Pair<String, String> e : examples) {
            System.out.println(e.getFirst() + " " + e.getSecond());
        }
    }
}
