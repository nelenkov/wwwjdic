package org.nick.wwwjdic.hkr;

import java.util.Arrays;
import java.util.List;

import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WebServiceBackedActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

public class RecognizeKanjiActivity extends WebServiceBackedActivity implements
        OnClickListener {

    private static final String TAG = RecognizeKanjiActivity.class
            .getSimpleName();

    private static final String KR_DEFAULT_URL = "http://kanji.sljfaq.org/kanji16/kanji-0.016.cgi";

    private static final String PREF_KR_URL_KEY = "pref_kr_url";
    private static final String PREF_KR_TIMEOUT_KEY = "pref_kr_timeout";

    private static final int HKR_RESULT = 1;

    private Button clearButton;
    private Button recognizeButton;
    private CheckBox lookAheadCb;

    private KanjiDrawView drawView;

    @Override
    protected void activityOnCreate(Bundle savedInstanceState) {
        setContentView(R.layout.kanji_draw);

        setTitle(R.string.hkr);

        findViews();

        clearButton.setOnClickListener(this);
        recognizeButton.setOnClickListener(this);

        drawView.requestFocus();
    }

    private void findViews() {
        drawView = (KanjiDrawView) this.findViewById(R.id.kanji_draw_view);

        clearButton = (Button) findViewById(R.id.clear_canvas_button);
        recognizeButton = (Button) findViewById(R.id.recognize_button);
        lookAheadCb = (CheckBox) findViewById(R.id.lookAheadCb);
    }

    public static class RecognizeKanjiHandler extends WsResultHandler {

        public RecognizeKanjiHandler(RecognizeKanjiActivity krActivity) {
            super(krActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (activity == null) {
                // we are in the process of rotating the screen, defer handling
                Message newMsg = obtainMessage(msg.what, msg.arg1, msg.arg2);
                newMsg.obj = msg.obj;
                sendMessageDelayed(newMsg, 500);

                return;
            }

            RecognizeKanjiActivity krActivity = (RecognizeKanjiActivity) activity;

            switch (msg.what) {
            case HKR_RESULT:
                krActivity.dismissProgressDialog();

                if (msg.arg1 == 1) {
                    String[] results = (String[]) msg.obj;
                    krActivity.sendToDictionary(results);
                } else {
                    Toast t = Toast.makeText(krActivity, R.string.hkr_failed,
                            Toast.LENGTH_SHORT);
                    t.show();
                }
                break;
            default:
                super.handleMessage(msg);
            }
        }

    }

    @Override
    protected WsResultHandler createHandler() {
        return new RecognizeKanjiHandler(this);
    }

    class HkrTask implements Runnable {

        private List<Stroke> strokes;
        private Handler handler;

        public HkrTask(List<Stroke> strokes, Handler handler) {
            this.strokes = strokes;
            this.handler = handler;
        }

        @Override
        public void run() {
            try {
                KanjiRecognizerClient krClient = new KanjiRecognizerClient(
                        getKrUrl(), getKrTimeout());
                String[] results = krClient
                        .recognize(strokes, isUseLookahead());
                Log.i(TAG, "go KR result " + Arrays.asList(results));

                Message msg = handler.obtainMessage(HKR_RESULT, 1, 0);
                msg.obj = results;
                handler.sendMessage(msg);
            } catch (Exception e) {
                Log.e("TAG", "Character recognition failed", e);
                Message msg = handler.obtainMessage(HKR_RESULT, 0, 0);
                handler.sendMessage(msg);
            }
        }
    }

    private int getKrTimeout() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        String timeoutStr = preferences.getString(PREF_KR_TIMEOUT_KEY, "10");

        return Integer.parseInt(timeoutStr) * 1000;
    }

    private String getKrUrl() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        return preferences.getString(PREF_KR_URL_KEY, KR_DEFAULT_URL);
    }

    private boolean isUseLookahead() {
        return lookAheadCb.isChecked();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.clear_canvas_button:
            clear();

            break;
        case R.id.recognize_button:
            recognizeKanji();

            break;
        default:
            // do nothing
        }
    }

    private void clear() {
        drawView.clear();
    }

    private void recognizeKanji() {
        List<Stroke> strokes = drawView.getStrokes();
        HkrTask task = new HkrTask(strokes, handler);
        submitWsTask(task, "Doing character recognition...");
    }

    public void sendToDictionary(String[] results) {
        Intent intent = new Intent(this, HkrCandidates.class);
        Bundle extras = new Bundle();
        extras.putStringArray(Constants.HKR_CANDIDATES_KEY, results);
        intent.putExtras(extras);

        startActivity(intent);
    }

}
