package org.nick.wwwjdic.sod;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.nick.wwwjdic.Analytics;
import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.GzipStringResponseHandler;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.StringUtils;
import org.nick.wwwjdic.WebServiceBackedActivity;
import org.nick.wwwjdic.WwwjdicApplication;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
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
            final SodActivity sodActivity = (SodActivity) activity;

            switch (msg.what) {
            case STROKE_PATH_MSG:
                sodActivity.dismissProgressDialog();

                if (msg.arg1 == 1) {
                    final List<StrokePath> strokes = (List<StrokePath>) msg.obj;
                    boolean animate = msg.arg2 == 1;
                    if (strokes != null) {
                        if (animate) {
                            sodActivity.animate(strokes);
                        } else {
                            sodActivity.drawSod(strokes);
                        }
                    } else {
                        Toast t = Toast.makeText(sodActivity, String.format(
                                sodActivity.getString(R.string.no_sod_data),
                                sodActivity.getKanji()), Toast.LENGTH_SHORT);
                        t.show();
                    }
                } else {
                    Toast t = Toast.makeText(sodActivity,
                            R.string.getting_sod_data_failed, Toast.LENGTH_SHORT);
                    t.show();
                }
                break;
            case REDRAW_SOD_MSG:
                final List<StrokePath> strokes = (List<StrokePath>) msg.obj;
                sodActivity.redraw(strokes);
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }

    private static final String TAG = SodActivity.class.getSimpleName();

    private static final int STROKE_PATH_MSG = 1;

    private static final int REDRAW_SOD_MSG = 2;

    private static final String STROKE_PATH_LOOKUP_URL = "http://wwwjdic-android.appspot.com/kanji/";

    private static final String PREF_SOD_ANIMATION_DELAY = "pref_sod_animation_delay";
    private static final String PREF_SOD_TIMEOUT = "pref_sod_server_timeout";

    private static final String NOT_FOUND_STATUS = "not found";

    private Button drawButton;
    private Button clearButton;
    private Button animateButton;

    private StrokeOrderView strokeOrderView;

    protected HttpContext localContext;
    private HttpClient httpClient;

    private String unicodeNumber;
    private String kanji;

    private List<StrokePath> strokes;

    @Override
    protected void activityOnCreate(Bundle savedInstanceState) {

        setContentView(R.layout.sod);

        findViews();

        drawButton.setOnClickListener(this);
        clearButton.setOnClickListener(this);
        animateButton.setOnClickListener(this);

        httpClient = createHttpClient();
        unicodeNumber = getIntent().getExtras().getString(
                Constants.KANJI_UNICODE_NUMBER);
        kanji = getIntent().getExtras().getString(Constants.KANJI_GLYPH);

        String message = getResources().getString(R.string.sod_for);
        setTitle(String.format(message, kanji));

        drawSod();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Analytics.startSession(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Analytics.endSession(this);
    }

    public void drawSod(List<StrokePath> strokes) {
        this.strokes = new ArrayList<StrokePath>(strokes);

        strokeOrderView.setStrokePaths(strokes);
        strokeOrderView.setAnnotateStrokes(true);
        strokeOrderView.invalidate();

    }

    public void redraw(List<StrokePath> strokes) {
        strokeOrderView.setStrokePaths(strokes);
        strokeOrderView.invalidate();
    }

    public void animate(final List<StrokePath> strokes) {
        this.strokes = new ArrayList<StrokePath>(strokes);

        strokeOrderView.setAnnotateStrokes(false);
        final int animationDelay = getStrokeAnimationDelay();

        Runnable animationTask = new Runnable() {
            public void run() {
                List<StrokePath> toDraw = new ArrayList<StrokePath>();
                for (StrokePath s : strokes) {
                    toDraw.add(s);
                    Message msg = handler.obtainMessage(REDRAW_SOD_MSG);
                    msg.obj = toDraw;
                    handler.sendMessage(msg);
                    try {
                        Thread.sleep(animationDelay);
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            }
        };
        getApp().getExecutorService().submit(animationTask);
    }

    @Override
    protected WsResultHandler createHandler() {
        return new SodHandler(this);
    }

    private HttpClient createHttpClient() {
        HttpClient result = new DefaultHttpClient();
        HttpParams httpParams = result.getParams();
        int timeout = getSodServerTimeout();
        HttpConnectionParams.setConnectionTimeout(httpParams, timeout);
        HttpConnectionParams.setSoTimeout(httpParams, timeout);

        return result;
    }

    private void findViews() {
        drawButton = (Button) findViewById(R.id.draw_sod_button);
        clearButton = (Button) findViewById(R.id.clear_sod_button);
        animateButton = (Button) findViewById(R.id.animate_button);
        strokeOrderView = (StrokeOrderView) findViewById(R.id.sod_draw_view);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.draw_sod_button:
            drawSod();
            break;
        case R.id.animate_button:
            animate();
            break;
        case R.id.clear_sod_button:
            strokeOrderView.clear();
            strokeOrderView.invalidate();
            break;
        default:
            // do nothing
        }
    }

    private int getStrokeAnimationDelay() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        String delayStr = preferences
                .getString(PREF_SOD_ANIMATION_DELAY, "700");

        return Integer.parseInt(delayStr);
    }

    private int getSodServerTimeout() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        String delayStr = preferences.getString(PREF_SOD_TIMEOUT, "30");

        return Integer.parseInt(delayStr) * 1000;
    }

    class GetStrokePathTask implements Runnable {

        private String unicodeNumber;
        private boolean animate;

        private Handler handler;
        private HttpClient httpClient;

        public GetStrokePathTask(String unicodeNumber, boolean animate,
                HttpClient httpClient, Handler handler) {
            this.unicodeNumber = unicodeNumber;
            this.animate = animate;
            this.handler = handler;
            this.httpClient = httpClient;
        }

        public void run() {
            String lookupUrl = STROKE_PATH_LOOKUP_URL + unicodeNumber;
            HttpGet get = new HttpGet(lookupUrl);
            get.addHeader("Accept-Encoding", "gzip");
            get.addHeader("User-Agent", "gzip");
            get.addHeader("X-User-Agent", WwwjdicApplication
                    .getUserAgentString());
            get.addHeader("X-Device-Version", getDeviceVersionStr());

            try {
                String responseStr = httpClient.execute(get,
                        new GzipStringResponseHandler(), localContext);
                Log.d(TAG, "got SOD response: " + responseStr);

                List<StrokePath> strokes = parseWsReply(responseStr);
                Message msg = handler.obtainMessage(STROKE_PATH_MSG, strokes);
                msg.arg1 = 1;
                msg.arg2 = animate ? 1 : 0;
                handler.sendMessage(msg);
            } catch (Exception e) {
                Message msg = handler.obtainMessage(STROKE_PATH_MSG);
                msg.arg1 = 0;
                handler.sendMessage(msg);
                Log.e(TAG, e.getMessage(), e);
            }
        }

        private String getDeviceVersionStr() {
            return String.format("%s/%s", Build.MODEL, Build.VERSION.RELEASE);
        }
    }

    private void drawSod() {
        Analytics.event("drawSod", this);

        if (strokes == null) {
            Runnable getStrokesTask = new GetStrokePathTask(unicodeNumber,
                    false, httpClient, handler);
            submitWsTask(getStrokesTask, getResources().getString(
                    R.string.getting_sod_info));
        } else {
            drawSod(strokes);
        }
    }

    private void animate() {
        Analytics.event("animateSod", this);

        if (strokes == null) {
            Runnable getStrokesTask = new GetStrokePathTask(unicodeNumber,
                    true, httpClient, handler);
            submitWsTask(getStrokesTask, getResources().getString(
                    R.string.getting_sod_info));
        } else {
            animate(strokes);
        }
    }

    private List<StrokePath> parseWsReply(String reply) {
        if (StringUtils.isEmpty(reply)) {
            return null;
        }

        if (reply.startsWith(NOT_FOUND_STATUS)) {
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

    public String getUnicodeNumber() {
        return unicodeNumber;
    }

    public String getKanji() {
        return kanji;
    }

}
