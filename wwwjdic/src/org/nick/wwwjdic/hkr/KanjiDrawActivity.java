package org.nick.wwwjdic.hkr;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.nick.wwwjdic.R;
import org.nick.wwwjdic.hkr.KanjiDrawView.OnStrokesChangedListener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class KanjiDrawActivity extends Activity implements OnClickListener,
        OnStrokesChangedListener {

    private static final String TAG = KanjiDrawActivity.class.getSimpleName();

    private static final int HKR_RESULT = 1;

    private Button clearButton;
    private Button recognizeButton;
    private TextView numStrokesText;

    private KanjiDrawView drawView;

    private ExecutorService ocrThread;
    private Future transPending;
    private Handler handler;

    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kanji_draw);

        drawView = (KanjiDrawView) this.findViewById(R.id.kanji_draw_view);
        drawView.setOnStrokesChangedListener(this);

        clearButton = (Button) findViewById(R.id.clear_canvas_button);
        recognizeButton = (Button) findViewById(R.id.recognize_button);
        numStrokesText = (TextView) findViewById(R.id.num_strokes);

        clearButton.setOnClickListener(this);
        recognizeButton.setOnClickListener(this);

        initThreading();

        drawView.requestFocus();
    }

    private void initThreading() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case HKR_RESULT:
                    progressDialog.dismiss();

                    if (msg.arg1 == 1) {
                        String[] results = (String[]) msg.obj;
                        sendToDictionary(results);
                    } else {
                        Toast t = Toast.makeText(KanjiDrawActivity.this,
                                "Character recognition failed",
                                Toast.LENGTH_SHORT);
                        t.show();
                    }
                    break;
                default:
                    super.handleMessage(msg);
                }
            }
        };
        ocrThread = Executors.newSingleThreadExecutor();
    }

    class HkrTask implements Runnable {

        private List<Stroke> strokes;
        private Handler handler;

        public HkrTask(List<Stroke> strokes, Handler h) {
            this.strokes = strokes;
            handler = h;
        }

        @Override
        public void run() {
            try {
                String url = "http://kanji.sljfaq.org/kanji16/kanji-0.016.cgi";
                KanjiRecognizerClient krClient = new KanjiRecognizerClient(url,
                        10 * 1000);
                String[] results = krClient.recognize(strokes);
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

    private void submitHkrTask(HkrTask ocrTask) {
        progressDialog = ProgressDialog.show(this, "",
                "Doing character recognition...", true);
        transPending = ocrThread.submit(ocrTask);
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
        numStrokesText.setText("0");
    }

    private void recognizeKanji() {
        List<Stroke> strokes = drawView.getStrokes();
        HkrTask task = new HkrTask(strokes, handler);
        submitHkrTask(task);
    }

    private void sendToDictionary(String[] results) {
        Intent intent = new Intent(this, HkrCandidates.class);
        Bundle extras = new Bundle();
        extras.putStringArray("hkrCandidates", results);
        intent.putExtras(extras);

        startActivity(intent);
    }

    @Override
    public void strokesUpdated(int numStrokes) {
        numStrokesText.setText(Integer.toString(numStrokes));
    }
}
