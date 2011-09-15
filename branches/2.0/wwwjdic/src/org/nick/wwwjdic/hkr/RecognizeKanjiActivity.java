package org.nick.wwwjdic.hkr;

import java.util.Arrays;
import java.util.List;

import org.nick.kanjirecognizer.hkr.CharacterRecognizer;
import org.nick.wwwjdic.Activities;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WebServiceBackedActivity;
import org.nick.wwwjdic.WwwjdicPreferences;
import org.nick.wwwjdic.ocr.WeOcrClient;
import org.nick.wwwjdic.utils.Analytics;
import org.nick.wwwjdic.utils.Dialogs;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.view.MenuItem;
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

    private static final String KR_USAGE_TIP_DIALOG = "kr_usage";

    private static final int OCR_IMAGE_WIDTH = 128;
    private static final int NUM_OCR_CANDIDATES = 20;

    private static final int NUM_KR_CANDIDATES = 10;

    private static final int HKR_RESULT = 1;

    private static final int HKR_RESULT_TYPE_WS = 0;
    private static final int HKR_RESULT_TYPE_OCR = 1;
    private static final int HKR_RESULT_TYPE_KR = 2;

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

        drawView.setAnnotateStrokes(WwwjdicPreferences.isAnnoateStrokes(this));
        drawView.setAnnotateStrokesMidway(WwwjdicPreferences
                .isAnnotateStrokesMidway(this));

        drawView.requestFocus();

        Dialogs.showTipOnce(this, KR_USAGE_TIP_DIALOG, R.string.kr_usage_tip);
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

        if (WwwjdicPreferences.isUseKanjiRecognizer(this) && !bound) {
            bindToKanjiRecognizer();
            setTitle(R.string.offline_hkr);
        } else {
            setTitle(R.string.online_hkr);
        }
    }

    void bindToKanjiRecognizer() {
        boolean success = bindService(new Intent(
                "org.nick.kanjirecognizer.hkr.RECOGNIZE_KANJI"), connection,
                Context.BIND_AUTO_CREATE);
        if (success) {
            Log.d(TAG, "successfully bound to KR service");
            lookAheadCb.setEnabled(false);
        } else {
            Log.d(TAG, "could not bind to KR service");
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

        drawView.setAnnotateStrokes(WwwjdicPreferences.isAnnoateStrokes(this));
        drawView.setAnnotateStrokesMidway(WwwjdicPreferences
                .isAnnotateStrokesMidway(this));
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
                    if (msg.arg2 == HKR_RESULT_TYPE_WS) {
                        if (WwwjdicPreferences.isKrInstalled(krActivity,
                                krActivity.getApplication())) {
                            showEnableKrDialog();
                        } else {
                            showInstallKrDialog();
                        }

                    } else {
                        Toast t = Toast.makeText(krActivity,
                                R.string.hkr_failed, Toast.LENGTH_SHORT);
                        t.show();
                    }
                }
                break;
            default:
                super.handleMessage(msg);
            }
        }

        private void showEnableKrDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(R.string.wskr_unavailable_enable_kr)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    WwwjdicPreferences.setUseKanjiRecognizer(
                                            true, activity);
                                    RecognizeKanjiActivity krActivity = (RecognizeKanjiActivity) activity;
                                    krActivity.bindToKanjiRecognizer();
                                    krActivity.setTitle(R.string.offline_hkr);
                                }
                            })
                    .setNegativeButton(R.string.no,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    dialog.cancel();
                                }
                            });
            AlertDialog dialog = builder.create();
            if (!activity.isFinishing()) {
                dialog.show();
            }
        }

        private void showInstallKrDialog() {
            if (activity.isFinishing()) {
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(R.string.wskr_unavailable_install_kr)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    Intent intent = new Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("market://details?id="
                                                    + WwwjdicPreferences.KR_PACKAGE));
                                    activity.startActivity(intent);
                                }
                            })
                    .setNegativeButton(R.string.no,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    dialog.cancel();
                                }
                            });
            AlertDialog dialog = builder.create();
            dialog.show();
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
                        WwwjdicPreferences
                                .getKrUrl(RecognizeKanjiActivity.this),
                        WwwjdicPreferences
                                .getKrTimeout(RecognizeKanjiActivity.this));
                String[] results = krClient
                        .recognize(strokes, isUseLookahead());
                Log.i(TAG, "go KR result " + Arrays.asList(results));

                Message msg = handler.obtainMessage(HKR_RESULT, 1,
                        HKR_RESULT_TYPE_WS);
                msg.obj = results;
                handler.sendMessage(msg);
            } catch (Exception e) {
                Log.e("TAG", "Character recognition failed", e);
                Message msg = handler.obtainMessage(HKR_RESULT, 0,
                        HKR_RESULT_TYPE_WS);
                handler.sendMessage(msg);
            }
        }

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
        if (!hasStrokes()) {
            return;
        }

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
                WeOcrClient client = new WeOcrClient(
                        WwwjdicPreferences
                                .getWeocrUrl(RecognizeKanjiActivity.this),
                        WwwjdicPreferences
                                .getWeocrTimeout(RecognizeKanjiActivity.this));
                String[] candidates = client.sendCharacterOcrRequest(bitmap,
                        NUM_OCR_CANDIDATES);

                if (candidates != null) {
                    Message msg = handler.obtainMessage(HKR_RESULT, 1,
                            HKR_RESULT_TYPE_OCR);
                    msg.obj = candidates;
                    handler.sendMessage(msg);
                } else {
                    Log.d("TAG", "OCR failed: null returned");
                    Message msg = handler.obtainMessage(HKR_RESULT, 0,
                            HKR_RESULT_TYPE_OCR);
                    handler.sendMessage(msg);
                }
            } catch (Exception e) {
                Log.e("TAG", "OCR failed", e);
                Message msg = handler.obtainMessage(HKR_RESULT, 0,
                        HKR_RESULT_TYPE_OCR);
                handler.sendMessage(msg);
            }
        }
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
        if (!hasStrokes()) {
            return;
        }

        List<Stroke> strokes = drawView.getStrokes();

        if (WwwjdicPreferences.isUseKanjiRecognizer(this)) {
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

    private boolean hasStrokes() {
        List<Stroke> strokes = drawView.getStrokes();

        return strokes != null && !strokes.isEmpty();
    }

    private void recognizeWs(List<Stroke> strokes) {
        Analytics.event("recognizeKanji", this);

        HkrTask task = new HkrTask(strokes, handler);
        String message = getResources().getString(R.string.doing_hkr);
        submitWsTask(task, message);
    }

    private void reconizeKanjiRecognizer(final List<Stroke> strokes) {
        Analytics.event("recognizeKanjiKr", this);

        Runnable krTask = new Runnable() {
            public void run() {
                try {
                    recognizer.startRecognition(drawView.getWidth(),
                            drawView.getHeight());
                    int strokeNum = 0;
                    for (Stroke s : strokes) {
                        for (PointF p : s.getPoints()) {
                            recognizer
                                    .addPoint(strokeNum, (int) p.x, (int) p.y);
                        }
                        strokeNum++;
                    }

                    String[] candidates = recognizer
                            .recognize(NUM_KR_CANDIDATES);
                    if (candidates != null) {
                        Message msg = handler.obtainMessage(HKR_RESULT, 1,
                                HKR_RESULT_TYPE_KR);
                        msg.obj = candidates;
                        handler.sendMessage(msg);
                    } else {
                        Message msg = handler.obtainMessage(HKR_RESULT, 0,
                                HKR_RESULT_TYPE_KR);
                        handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "error calling recognizer", e);
                    Message msg = handler.obtainMessage(HKR_RESULT, 0,
                            HKR_RESULT_TYPE_KR);
                    handler.sendMessage(msg);
                }
            }
        };
        submitWsTask(krTask, getResources().getString(R.string.doing_hkr));
    }

    public void sendToDictionary(String[] results) {
        Intent intent = new Intent(this, HkrCandidates.class);
        Bundle extras = new Bundle();
        extras.putStringArray(HkrCandidates.EXTRA_HKR_CANDIDATES, results);
        intent.putExtras(extras);

        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Activities.home(this);
            return true;
        default:
            // do nothing
        }

        return super.onOptionsItemSelected(item);
    }

}
