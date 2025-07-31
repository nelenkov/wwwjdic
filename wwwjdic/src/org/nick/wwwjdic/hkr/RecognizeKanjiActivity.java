
package org.nick.wwwjdic.hkr;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.nick.kanjirecognizer.hkr.CharacterRecognizer;
import org.nick.wwwjdic.Activities;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WebServiceBackedActivity;
import org.nick.wwwjdic.WwwjdicApplication;
import org.nick.wwwjdic.WwwjdicPreferences;
import org.nick.wwwjdic.sod.StrokePath;
import org.nick.wwwjdic.utils.Dialogs;

public class RecognizeKanjiActivity extends WebServiceBackedActivity implements OnClickListener {

    private static final String TAG = RecognizeKanjiActivity.class.getSimpleName();

    private static final String KR_USAGE_TIP_DIALOG = "kr_usage";

    private static final int NUM_KR_CANDIDATES = 10;

    private static final int HKR_RESULT = 1;

    private static final int HKR_RESULT_TYPE_KR = 2;

    private static final Pattern KANJI_PATTERN = Pattern.compile(
            "\\p{InCJKUnifiedIdeographs}", Pattern.COMMENTS);

    private static final boolean USE_LOCAL_KR = true;

    private static String[] filterOutNonKanji(String[] results) {
        List<String> kanjiCandidates = new ArrayList<>();
        for (String s : results) {
            Matcher m = KANJI_PATTERN.matcher(s);
            if (m.matches()) {
                kanjiCandidates.add(s);
            }
        }
        return kanjiCandidates.toArray(new String[0]);
    }

    private Button recognizeButton;
    private Button removeStrokeButton;
    private Button clearButton;

    private KanjiDrawView drawView;

    private CharacterRecognizer recognizer;
    private boolean bound;

    private  KanjiCharacterRecognizer localRecognizer;

    @Override
    @SuppressWarnings("deprecation")
    protected void activityOnCreate(Bundle savedInstanceState) {
        setContentView(R.layout.kanji_draw);

        setTitle(R.string.hkr);

        findViews();

        recognizeButton.setOnClickListener(this);
        removeStrokeButton.setOnClickListener(this);
        clearButton.setOnClickListener(this);

        drawView.setAnnotateStrokes(WwwjdicPreferences.isAnnoateStrokes(this));
        drawView.setAnnotateStrokesMidway(WwwjdicPreferences
                .isAnnotateStrokesMidway(this));
        setAnnotationTextSize();

        drawView.requestFocus();

        File modelFile = new File(getFilesDir(), KanjiCharacterRecognizer.HKR_COMBINED_CMP_MODEL_FILENAME);
        if (!modelFile.exists()) {
            recognizeButton.setEnabled(false);
            Log.d(TAG, "Model file doesn't existing, copying: " + modelFile.getAbsolutePath());
            new CopyModelTask().execute(modelFile);
        }
        localRecognizer = new KanjiCharacterRecognizer(this);

        Dialogs.showTipOnce(this, KR_USAGE_TIP_DIALOG, R.string.kr_usage_tip);
    }

     @SuppressLint("StaticFieldLeak")
     @SuppressWarnings("deprecation")
     class CopyModelTask extends AsyncTask<File, Void, File> {

        @Override
        protected File doInBackground(File... files) {
            File modelFile = files[0];
            Log.d(TAG, "Copying model file to " + modelFile.getAbsolutePath());
            copyModelFromResource(modelFile);

            return modelFile;
        }

         @Override
         protected void onPostExecute(File modelFile) {
             super.onPostExecute(modelFile);
             try {
                 Log.d(TAG, "initializing recognizer with " + modelFile.getAbsolutePath());
                 localRecognizer.initialize(modelFile);
                 recognizeButton.setEnabled(true);
             } catch(Exception e) {
                 Toast.makeText(RecognizeKanjiActivity.this,
                     R.string.kr_error, Toast.LENGTH_LONG).show();
             }
         }
     }

    private void copyModelFromResource(File targetFile) {
        try (InputStream in = getResources().openRawResource(R.raw.kr_ja_model)) {
            try (OutputStream out = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer, 0, buffer.length)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Copying model failed: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void setAnnotationTextSize() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        float annotationWidth = dm.scaledDensity * StrokePath.DEFAULT_ANNOTATION_TEXT_SIZE;
        drawView.setAnnotationTextSize(annotationWidth);
    }

    private final ServiceConnection connection = new ServiceConnection() {
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

        if (WwwjdicPreferences.isUseKanjiRecognizer(this) && !bound) {
            if (!USE_LOCAL_KR) {
                bindToKanjiRecognizer();
            }
            setTitle(R.string.offline_hkr);
        } else {
            setTitle(R.string.online_hkr);
        }

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    void bindToKanjiRecognizer() {
        Intent intent = new Intent("org.nick.kanjirecognizer.hkr.RECOGNIZE_KANJI");
        intent.setPackage("org.nick.kanjirecognizer");
        boolean success = bindService(intent, connection,
                Context.BIND_AUTO_CREATE);
        if (success) {
            Log.d(TAG, "successfully bound to KR service");
        } else {
            Log.d(TAG, "could not bind to KR service");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

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
        recognizeButton = findViewById(R.id.recognize_button);
        removeStrokeButton = findViewById(R.id.remove_stroke_button);
        clearButton = findViewById(R.id.clear_canvas_button);
        drawView = findViewById(R.id.kanji_draw_view);
    }

    public static class RecognizeKanjiHandler extends WsResultHandler {

        public RecognizeKanjiHandler(RecognizeKanjiActivity krActivity) {
            super(krActivity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (activity == null) {
                // we are in the process of rotating the screen, defer handling
                Message newMsg = obtainMessage(msg.what, msg.arg1, msg.arg2);
                newMsg.obj = msg.obj;
                sendMessageDelayed(newMsg, 500);

                return;
            }

            RecognizeKanjiActivity krActivity = (RecognizeKanjiActivity) activity;

          if (msg.what == HKR_RESULT) {
            krActivity.dismissProgressDialog();

            if (msg.arg1 == 1) {
              String[] results = (String[]) msg.obj;
              String[] candidates = filterOutNonKanji(results);
              krActivity.sendToDictionary(candidates);
            } else {
              if (msg.arg2 == HKR_RESULT_TYPE_KR) {
                Toast.makeText(krActivity, R.string.kr_error, Toast.LENGTH_SHORT).show();
              } else {
                Toast.makeText(krActivity, R.string.hkr_failed, Toast.LENGTH_SHORT).show();
              }
            }
          } else {
            super.handleMessage(msg);
          }
        }
    }

    @Override
    protected WsResultHandler createHandler() {
        return new RecognizeKanjiHandler(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.recognize_button) {
            if (!USE_LOCAL_KR && !WwwjdicPreferences.isKrInstalled(this,
                WwwjdicApplication.getInstance())) {
                WwwjdicPreferences.showInstallKrDialog(this);
                return;
            }

            recognizeKanji();
        } else if (v.getId() == R.id.remove_stroke_button) {
            drawView.removeLastStroke();
        } else if (v.getId() == R.id.clear_canvas_button) {
            clear();
        }
    }

    private void clear() {
        drawView.clear();
    }

    private void recognizeKanji() {
        if (noStrokes()) {
            return;
        }

        List<Stroke> strokes = drawView.getStrokes();

        if (USE_LOCAL_KR) {
            recognizeLocal(strokes);
        } else {
            if (WwwjdicPreferences.isUseKanjiRecognizer(this)) {
                if (recognizer == null) {
                    Toast.makeText(this, R.string.kr_not_initialized,
                        Toast.LENGTH_SHORT).show();
                } else {
                    recognizeKanjiRecognizer(strokes);
                }
            }
        }
    }

    private boolean noStrokes() {
        List<Stroke> strokes = drawView.getStrokes();

        return strokes == null || strokes.isEmpty();
    }

    private void recognizeLocal(final List<Stroke> strokes) {
        try {
            localRecognizer.startRecognition(drawView.getWidth(), drawView.getHeight());
            int strokeNum = 0;
            for (Stroke s : strokes) {
                for (PointF p : s.getPoints()) {
                    localRecognizer.addPoint(strokeNum, (int) p.x, (int) p.y);
                }
                strokeNum++;
            }

            String[] candidates = localRecognizer.recognize(NUM_KR_CANDIDATES);
            Message msg;
            if (candidates != null) {
                msg = handler.obtainMessage(HKR_RESULT, 1, HKR_RESULT_TYPE_KR);
                msg.obj = candidates;
            } else {
                msg = handler.obtainMessage(HKR_RESULT, 0, HKR_RESULT_TYPE_KR);
            }
            handler.sendMessage(msg);
        } catch (Exception e) {
            Log.d(TAG, "error calling local recognizer", e);
            Message msg = handler.obtainMessage(HKR_RESULT, 0, HKR_RESULT_TYPE_KR);
            handler.sendMessage(msg);
        }
    }

    private void recognizeKanjiRecognizer(final List<Stroke> strokes) {
        Runnable krTask = () -> {
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
              Message msg;
              if (candidates != null) {
                msg = handler.obtainMessage(HKR_RESULT, 1, HKR_RESULT_TYPE_KR);
                    msg.obj = candidates;
              } else {
                msg = handler.obtainMessage(HKR_RESULT, 0, HKR_RESULT_TYPE_KR);
              }
              handler.sendMessage(msg);
            } catch (Exception e) {
                Log.d(TAG, "error calling recognizer", e);
                Message msg = handler.obtainMessage(HKR_RESULT, 0, HKR_RESULT_TYPE_KR);
                handler.sendMessage(msg);
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
      if (item.getItemId() == android.R.id.home) {
        Activities.home(this);
        return true;
      }

      return super.onOptionsItemSelected(item);
    }

}
