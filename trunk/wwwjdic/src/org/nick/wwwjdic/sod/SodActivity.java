package org.nick.wwwjdic.sod;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.HttpClientFactory;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicPreferences;
import org.nick.wwwjdic.utils.Analytics;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class SodActivity extends Activity implements OnClickListener {

    static class GetSodTask extends AsyncTask<String, Void, String> {

        private SodActivity sodActivity;
        private boolean animate;
        private HttpClient httpClient;
        private ResponseHandler<String> responseHandler;

        GetSodTask(SodActivity sodActivity, boolean animate) {
            this.sodActivity = sodActivity;
            this.animate = animate;
            this.httpClient = createHttpClient();
            this.responseHandler = HttpClientFactory
                    .createWwwjdicResponseHandler();
        }

        private HttpClient createHttpClient() {
            return HttpClientFactory.createSodHttpClient(WwwjdicPreferences
                    .getSodServerTimeout(sodActivity));
        }

        @Override
        protected void onPreExecute() {
            if (sodActivity == null) {
                return;
            }

            String message = sodActivity.getResources().getString(
                    R.string.getting_sod_info);
            sodActivity.showProgressDialog(message);
        }

        @Override
        protected String doInBackground(String... params) {
            String unicodeNumber = params[0];
            String lookupUrl = STROKE_PATH_LOOKUP_URL + unicodeNumber;
            HttpGet get = new HttpGet(lookupUrl);

            try {
                String responseStr = httpClient.execute(get, responseHandler);
                Log.d(TAG, "got SOD response: " + responseStr);

                return responseStr;
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);

                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (sodActivity == null) {
                return;
            }

            sodActivity.dismissProgressDialog();
            if (result != null) {
                sodActivity.setStrokePathsStr(result);
                StrokedCharacter character = parseWsReply(result);
                if (character != null) {
                    if (animate) {
                        sodActivity.animate(character);
                    } else {
                        sodActivity.drawSod(character);
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
        }

        void attach(SodActivity sodActivity) {
            this.sodActivity = sodActivity;
        }

        void detach() {
            sodActivity = null;
        }
    }

    private static final String TAG = SodActivity.class.getSimpleName();

    private static final String STROKE_PATH_LOOKUP_URL = "http://wwwjdic-android.appspot.com/kanji/";

    private static final String NOT_FOUND_STATUS = "not found";

    private static final String EXTRA_RIGHT_STROKE_PATHS_STRING = "org.nick.recognizer.quiz.RIGHT_STROKE_PATHS_STRING";

    private static final float KANJIVG_SIZE = 109f;

    private Button drawButton;
    private Button clearButton;
    private Button animateButton;

    private StrokeOrderView strokeOrderView;

    private String unicodeNumber;
    private String kanji;

    private StrokedCharacter character;
    private String strokePathsStr;

    private GetSodTask getSodTask;
    private boolean isRotating = false;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sod);

        findViews();

        drawButton.setOnClickListener(this);
        clearButton.setOnClickListener(this);
        animateButton.setOnClickListener(this);

        unicodeNumber = getIntent().getExtras().getString(
                Constants.KANJI_UNICODE_NUMBER);
        kanji = getIntent().getExtras().getString(Constants.KANJI_GLYPH);

        String message = getResources().getString(R.string.sod_for);
        setTitle(String.format(message, kanji));

        getSodTask = (GetSodTask) getLastNonConfigurationInstance();
        if (getSodTask != null) {
            getSodTask.attach(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (getSodTask != null && !isRotating) {
            getSodTask.cancel(true);
            getSodTask = null;
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        isRotating = true;

        if (getSodTask != null) {
            getSodTask.detach();
        }

        return getSodTask;
    }

    @Override
    protected void onStart() {
        super.onStart();

        Analytics.startSession(this);
    }


    @Override
    protected void onResume() {
        super.onResume();

        drawSod();
    }

    @Override
    protected void onStop() {
        super.onStop();

        Analytics.endSession(this);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null) {
            strokePathsStr = savedInstanceState
                    .getString(EXTRA_RIGHT_STROKE_PATHS_STRING);
            character = parseWsReply(strokePathsStr);
            if (character != null) {
                strokeOrderView.setCharacter(character);
                strokeOrderView.invalidate();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(EXTRA_RIGHT_STROKE_PATHS_STRING, strokePathsStr);
    }

    void drawSod(StrokedCharacter character) {
        this.character = character;

        strokeOrderView.setCharacter(character);
        strokeOrderView.setAnnotateStrokes(true);
        strokeOrderView.invalidate();

    }

    void animate(StrokedCharacter character) {
        this.character = character;

        int animationDelay = WwwjdicPreferences.getStrokeAnimationDelay(this);
        strokeOrderView.setAnimationDelayMillis(animationDelay);
        strokeOrderView.setCharacter(character);
        strokeOrderView.setAnnotateStrokes(true);
        strokeOrderView.startAnimation();
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

    private void drawSod() {
        Analytics.event("drawSod", this);

        if (character == null) {
            getStrokes();
        } else {
            drawSod(character);
        }
    }

    private void getStrokes() {
        if (getSodTask != null) {
            getSodTask.cancel(true);
        }
        getSodTask = new GetSodTask(this, false);
        getSodTask.execute(unicodeNumber);
    }

    private void animate() {
        Analytics.event("animateSod", this);

        if (character == null) {
            getStrokes();
        } else {
            animate(character);
        }
    }

    private static List<StrokePath> parseWsReplyStrokes(String reply) {
        if (reply == null || "".equals(reply)) {

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

    private static StrokedCharacter parseWsReply(String reply) {
        List<StrokePath> strokes = parseWsReplyStrokes(reply);
        if (strokes == null) {
            return null;
        }

        StrokedCharacter result = new StrokedCharacter(strokes, KANJIVG_SIZE,
                KANJIVG_SIZE);

        return result;
    }

    String getKanji() {
        return kanji;
    }

    String getStrokePathsStr() {
        return strokePathsStr;
    }

    void setStrokePathsStr(String strokePathsStr) {
        this.strokePathsStr = strokePathsStr;
    }

    void showProgressDialog(String message) {
        progressDialog = ProgressDialog.show(this, "", message, true);
    }

    void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()
                && !isFinishing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
}
