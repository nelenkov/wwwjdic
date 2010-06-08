package org.nick.wwwjdic.sod;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.nick.wwwjdic.GzipStringResponseHandler;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.StringUtils;
import org.nick.wwwjdic.WebServiceBackedActivity;
import org.nick.wwwjdic.hkr.KanjiDrawView;
import org.xmlpull.v1.XmlPullParser;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class SodActivity extends WebServiceBackedActivity implements
        OnClickListener {

    public static class SodHandler extends WsResultHandler {

        public SodHandler(SodActivity sodActivity) {
            super(sodActivity);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            SodActivity sodActivity = (SodActivity) activity;

            switch (msg.what) {
            case STROKE_PATH_MSG:
                sodActivity.dismissProgressDialog();

                if (msg.arg1 == 1) {
                    List<StrokePath> strokes = (List<StrokePath>) msg.obj;
                    if (strokes != null) {
                        sodActivity.drawSod(strokes);
                    } else {
                        Toast t = Toast.makeText(sodActivity,
                                String.format("No SOD data for '%s'",
                                        sodActivity.getKanji()),
                                Toast.LENGTH_SHORT);
                        t.show();
                    }
                } else {
                    Toast t = Toast.makeText(sodActivity,
                            "Getting SOD data failed", Toast.LENGTH_SHORT);
                    t.show();
                }
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }

    private static final String TAG = SodActivity.class.getSimpleName();

    private static final int STROKE_PATH_MSG = 1;

    private static final String STROKE_PATH_LOOKUP_URL = "http://wwwjdic-android.appspot.com/kanji/";

    private Button drawButton;
    private Button clearButton;

    private KanjiDrawView drawView;

    protected HttpContext localContext;
    private HttpClient httpClient;

    private String unicodeNumber;
    private String kanji;

    @Override
    protected void activityOnCreate(Bundle savedInstanceState) {

        setContentView(R.layout.sod);

        findViews();

        drawButton.setOnClickListener(this);
        clearButton.setOnClickListener(this);

        httpClient = createHttpClient();
        unicodeNumber = getIntent().getExtras().getString("unicodeNumber");
        kanji = getIntent().getExtras().getString("kanji");

        setTitle(String.format("Stroke order diragram for '%s'", kanji));
    }

    public void drawSod(List<StrokePath> strokes) {
        drawView.setStrokePaths(strokes);
        drawView.invalidate();

    }

    @Override
    protected WsResultHandler createHandler() {
        return new SodHandler(this);
    }

    private HttpClient createHttpClient() {
        HttpClient result = new DefaultHttpClient();
        HttpParams httpParams = result.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 10 * 1000);
        HttpConnectionParams.setSoTimeout(httpParams, 10 * 1000);

        return result;
    }

    private void findViews() {
        drawButton = (Button) findViewById(R.id.draw_sod_button);
        clearButton = (Button) findViewById(R.id.clear_sod_button);
        drawView = (KanjiDrawView) findViewById(R.id.sod_draw_view);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.draw_sod_button:
            drawSod();
            break;
        case R.id.clear_sod_button:
            drawView.clear();
            drawView.setStrokePaths(null);
            drawView.invalidate();
            break;
        default:
            // do nothing
        }
    }

    class GetStrokePathTask implements Runnable {

        private String unicodeNumber;
        private Handler handler;
        private HttpClient httpClient;

        public GetStrokePathTask(String unicodeNumber, HttpClient httpClient,
                Handler handler) {
            this.unicodeNumber = unicodeNumber;
            this.handler = handler;
            this.httpClient = httpClient;
        }

        public void run() {
            String lookupUrl = STROKE_PATH_LOOKUP_URL + unicodeNumber;
            HttpGet get = new HttpGet(lookupUrl);
            get.addHeader("Accept-Encoding", "gzip");
            get.addHeader("User-Agent", "gzip");

            try {
                String responseStr = httpClient.execute(get,
                        new GzipStringResponseHandler(), localContext);

                List<StrokePath> strokes = parseWsReply(responseStr);
                Message msg = handler.obtainMessage(STROKE_PATH_MSG, strokes);
                msg.arg1 = 1;
                handler.sendMessage(msg);
            } catch (Exception e) {
                Message msg = handler.obtainMessage(STROKE_PATH_MSG);
                msg.arg1 = 0;
                handler.sendMessage(msg);
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    private void drawSod() {
        Runnable getStrokesTask = new GetStrokePathTask(unicodeNumber,
                httpClient, handler);
        submitWsTask(getStrokesTask, "Getting SOD info...");
    }

    private List<StrokePath> parseWsReply(String reply) {
        if (StringUtils.isEmpty(reply)) {
            return null;
        }

        if ("<empty>".equals(reply)) {
            return null;
        }

        String[] lines = reply.split("\n");

        List<StrokePath> result = new ArrayList<StrokePath>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line != null && !"".equals(line)) {
                StrokePath strokePath = StrokePath.parsePath(line.trim());
                result.add(strokePath);
            }
        }

        return result;
    }

    private List<StrokePath> parseKangiVgXml() {
        // File f = new File("/sdcard/wwwjdic/onekanji.xml");
        File f = new File("/sdcard/wwwjdic/k2.xml");

        List<StrokePath> strokes = new ArrayList<StrokePath>();
        XmlPullParser parser = Xml.newPullParser();

        try {
            // auto-detect the encoding from the stream
            parser.setInput(new FileInputStream(f), null);
            int eventType = parser.getEventType();
            boolean done = false;

            while (eventType != XmlPullParser.END_DOCUMENT && !done) {
                String name = null;
                switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    break;
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    if (name.equalsIgnoreCase("stroke")) {
                        String path = parser.getAttributeValue(null, "path");
                        StrokePath strokePath = StrokePath.parsePath(path);
                        strokes.add(strokePath);
                    }
                    break;
                case XmlPullParser.END_TAG:
                    name = parser.getName();

                    break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return strokes;
    }

    public String getUnicodeNumber() {
        return unicodeNumber;
    }

    public String getKanji() {
        return kanji;
    }

}
