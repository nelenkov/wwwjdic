package org.nick.wwwjdic.hkr;

import java.util.Arrays;
import java.util.List;

import org.nick.kanjirecognizer.hkr.CharacterRecognizer;
import org.nick.wwwjdic.Analytics;
import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WebServiceBackedActivity;
import org.nick.wwwjdic.ocr.WeOcrClient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
    private static final String PREF_KR_ANNOTATE = "pref_kr_annotate";
    private static final String PREF_KR_ANNOTATE_MIDWAY = "pref_kr_annotate_midway";
    private static final String PREF_KR_USE_KANJI_RECOGNIZER_KEY = "pref_kr_use_kanji_recognizer";

    private static final String WEOCR_DEFAULT_URL = "http://maggie.ocrgrid.org/cgi-bin/weocr/nhocr.cgi";
    private static final String PREF_WEOCR_URL_KEY = "pref_weocr_url";
    private static final String PREF_WEOCR_TIMEOUT_KEY = "pref_weocr_timeout";

    private static final int OCR_IMAGE_WIDTH = 128;
    private static final int NUM_OCR_CANDIDATES = 20;

    private static final int NUM_KR_CANDIDATES = 10;

    private static final int HKR_RESULT = 1;

    private Button recognizeButton;
    private Button ocrButton;
    private Button removeStrokeButton;
    private Button clearButton;
    private CheckBox lookAheadCb;

    private KanjiDrawView drawView;

    private CharacterRecognizer recognizer;
    private boolean bound;

    @Override
    protected void activityOnCreate(Bundle savedInstanceState) {
        setContentView(R.layout.kanji_draw);

        setTitle(R.string.hkr);

        findViews();

        recognizeButton.setOnClickListener(this);
        ocrButton.setOnClickListener(this);
        removeStrokeButton.setOnClickListener(this);
        clearButton.setOnClickListener(this);

        drawView.setAnnotateStrokes(isAnnoateStrokes());
        drawView.setAnnotateStrokesMidway(isAnnotateStrokesMidway());

        drawView.requestFocus();
    }

    private ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            recognizer = CharacterRecognizer.Stub.asInterface(service);
            bound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            recognizer = null;
            bound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        Analytics.startSession(this);

        if (isUseKanjiRecognizer() && !bound) {
            boolean success = bindService(new Intent(
                    "org.nick.kanjirecognizer.hkr.RECOGNIZE_KANJI"),
                    connection, Context.BIND_AUTO_CREATE);
            if (success) {
                Log.d(TAG, "successfully bound to KR service");
                lookAheadCb.setEnabled(false);
            } else {
                Log.d(TAG, "could not bind to KR service");
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        Analytics.endSession(this);

        if (bound) {
            bound = false;
            unbindService(connection);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        drawView.setAnnotateStrokes(isAnnoateStrokes());
        drawView.setAnnotateStrokesMidway(isAnnotateStrokesMidway());
    }

    private void findViews() {
        drawView = (KanjiDrawView) this.findViewById(R.id.kanji_draw_view);

        recognizeButton = (Button) findViewById(R.id.recognize_button);
        ocrButton = (Button) findViewById(R.id.ocr_button);
        removeStrokeButton = (Button) findViewById(R.id.remove_stroke_button);
        clearButton = (Button) findViewById(R.id.clear_canvas_button);
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

    private boolean isAnnotateStrokesMidway() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        return preferences.getBoolean(PREF_KR_ANNOTATE_MIDWAY, false);
    }

    private boolean isAnnoateStrokes() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        return preferences.getBoolean(PREF_KR_ANNOTATE, true);
    }

    private boolean isUseKanjiRecognizer() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        return preferences.getBoolean(PREF_KR_USE_KANJI_RECOGNIZER_KEY, false);
    }

    private boolean isUseLookahead() {
        return lookAheadCb.isChecked();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.recognize_button:
            recognizeKanji();
            break;
        case R.id.ocr_button:
            ocrKanji();
            break;
        case R.id.remove_stroke_button:
            drawView.removeLastStroke();
            break;
        case R.id.clear_canvas_button:
            clear();
            break;
        default:
            // do nothing
        }
    }

    private void ocrKanji() {
        Bitmap bitmap = drawingToBitmap();
        OcrTask task = new OcrTask(bitmap, handler);
        String message = getResources().getString(R.string.doing_hkr);
        submitWsTask(task, message);

        Analytics.event("recognizeKanjiOcr", this);
    }

    class OcrTask implements Runnable {

        private Bitmap bitmap;
        private Handler handler;

        public OcrTask(Bitmap b, Handler h) {
            bitmap = b;
            handler = h;
        }

        @Override
        public void run() {
            try {
                WeOcrClient client = new WeOcrClient(getWeocrUrl(),
                        getWeocrTimeout());
                String[] candidates = client.sendCharacterOcrRequest(bitmap,
                        NUM_OCR_CANDIDATES);

                if (candidates != null) {
                    Message msg = handler.obtainMessage(HKR_RESULT, 1, 0);
                    msg.obj = candidates;
                    handler.sendMessage(msg);
                } else {
                    Log.d("TAG", "OCR failed: null returned");
                    Message msg = handler.obtainMessage(HKR_RESULT, 0, 0);
                    handler.sendMessage(msg);
                }
            } catch (Exception e) {
                Log.e("TAG", "OCR failed", e);
                Message msg = handler.obtainMessage(HKR_RESULT, 0, 0);
                handler.sendMessage(msg);
            }
        }
    }

    private int getWeocrTimeout() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        String timeoutStr = preferences.getString(PREF_WEOCR_TIMEOUT_KEY, "10");

        return Integer.parseInt(timeoutStr) * 1000;
    }

    private String getWeocrUrl() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        return preferences.getString(PREF_WEOCR_URL_KEY, WEOCR_DEFAULT_URL);
    }

    private Bitmap drawingToBitmap() {
        Bitmap b = Bitmap.createBitmap(drawView.getWidth(),
                drawView.getWidth(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);

        boolean annotate = drawView.isAnnotateStrokes();
        drawView.setAnnotateStrokes(false);
        drawView.setBackgroundColor(0xff888888);
        drawView.setStrokePaintColor(Color.BLACK);

        drawView.draw(c);

        drawView.setAnnotateStrokes(annotate);
        drawView.setBackgroundColor(Color.BLACK);
        drawView.setStrokePaintColor(Color.WHITE);

        int width = drawView.getWidth();
        int newWidth = OCR_IMAGE_WIDTH;
        float scale = ((float) newWidth) / width;
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        c.scale(scale, scale);
        Bitmap resized = Bitmap.createBitmap(b, 0, 0, width, width, matrix,
                true);
        return resized;
    }

    private void clear() {
        drawView.clear();
    }

    private void recognizeKanji() {
        List<Stroke> strokes = drawView.getStrokes();

        if (isUseKanjiRecognizer()) {
            if (recognizer == null) {
                Toast t = Toast.makeText(this, R.string.kr_not_initialized,
                        Toast.LENGTH_SHORT);
                t.show();
                recognizeWs(strokes);
            } else {
                reconizeKanjiRecognizer(strokes);
            }
        } else {
            recognizeWs(strokes);
        }
    }

    private void recognizeWs(List<Stroke> strokes) {
        Analytics.event("recognizeKanji", this);

        HkrTask task = new HkrTask(strokes, handler);
        String message = getResources().getString(R.string.doing_hkr);
        submitWsTask(task, message);
    }

    private void reconizeKanjiRecognizer(List<Stroke> strokes) {
        Analytics.event("recognizeKanjiKr", this);

        try {
            recognizer.startRecognition(drawView.getWidth(), drawView
                    .getHeight());
            int strokeNum = 0;
            for (Stroke s : strokes) {
                for (PointF p : s.getPoints()) {
                    recognizer.addPoint(strokeNum, (int) p.x, (int) p.y);
                }
                strokeNum++;
            }

            String[] candidates = recognizer.recognize(NUM_KR_CANDIDATES);
            if (candidates != null) {
                Message msg = handler.obtainMessage(HKR_RESULT, 1, 0);
                msg.obj = candidates;
                handler.sendMessage(msg);
            } else {
                Message msg = handler.obtainMessage(HKR_RESULT, 0, 0);
                handler.sendMessage(msg);
            }
        } catch (Exception e) {
            Log.d(TAG, "error calling recognizer", e);
            Message msg = handler.obtainMessage(HKR_RESULT, 0, 0);
            handler.sendMessage(msg);
        }
    }

    public void sendToDictionary(String[] results) {
        Intent intent = new Intent(this, HkrCandidates.class);
        Bundle extras = new Bundle();
        extras.putStringArray(Constants.HKR_CANDIDATES_KEY, results);
        intent.putExtras(extras);

        startActivity(intent);
    }

}
