package org.nick.wwwjdic.sod;

import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nick.wwwjdic.ActionBarActivity;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicPreferences;
import org.nick.wwwjdic.client.HttpClientFactory;
import org.nick.wwwjdic.utils.LoaderBase;
import org.nick.wwwjdic.utils.LoaderResult;
import org.nick.wwwjdic.utils.Pair;

import java.util.ArrayList;
import java.util.List;

import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

public class SodActivity extends ActionBarActivity implements OnClickListener,
        LoaderManager.LoaderCallbacks<LoaderResult<Pair<String, Boolean>>> {

    private static final String TAG = SodActivity.class.getSimpleName();

    public static final String EXTRA_KANJI_UNICODE_NUMBER = "unicodeNumber";

    public static final String EXTRA_KANJI_GLYPH = "org.nick.wwwjdic.kanjiGlyph";

    private static final String STROKE_PATH_LOOKUP_URL = "https://wwwjdic-android.appspot.com/kanji/";

    static class SodLoader extends LoaderBase<Pair<String, Boolean>> {

        private String unicodeNumber;
        private boolean animate;

        private HttpClient httpClient;

        SodLoader(Context context, String unicodeNumber, boolean animate) {
            super(context);
            this.unicodeNumber = unicodeNumber;
            this.animate = animate;
            this.httpClient = HttpClientFactory
                    .createSodHttpClient(WwwjdicPreferences
                            .getSodServerTimeout(context));
        }

        @Override
        protected void releaseResult(LoaderResult<Pair<String, Boolean>> result) {
            // just a string, nothing to do
        }

        @Override
        public Pair<String, Boolean> load() throws Exception {
            String lookupUrl = STROKE_PATH_LOOKUP_URL + unicodeNumber
                    + "?f=json";
            HttpGet get = new HttpGet(lookupUrl);

            HttpResponse response = httpClient.execute(get);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                Log.d(TAG, String.format("SOD for %s not found", unicodeNumber));

                return null;
            }

            HttpEntity entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                if (entity != null) {
                    entity.consumeContent();
                }
                Log.w(TAG, "Got error status: " + response.getStatusLine());

                throw new RuntimeException("Server error: "
                        + response.getStatusLine());
            }

            Header contentType = entity.getContentType();
            if (contentType == null
                    || !contentType.getValue().contains(
                            "application/x-javascript")) {
                Log.w(TAG, "Invalid content type: " + contentType);
                throw new RuntimeException(
                        "Invalid response. Check your Internet connection using "
                                + "a browser and make sure you are authenticated to your "
                                + "proxy server, if using one.");
            }

            String responseStr = null;
            if (entity != null) {
                responseStr = EntityUtils.toString(entity);
            }
            Log.d(TAG, "got SOD response: " + responseStr);

            return new Pair<String, Boolean>(responseStr, animate);
        }

        @Override
        protected boolean isActive(LoaderResult<Pair<String, Boolean>> result) {
            return false;
        }
    }

    private static final float KANJIVG_SIZE = 109f;

    private Button drawButton;
    private Button clearButton;
    private Button animateButton;

    private ProgressBar progressSpinner;
    private StrokeOrderView strokeOrderView;

    private String unicodeNumber;
    private String kanji;

    private StrokedCharacter character;
    private String strokePathsStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sod);

        findViews();

        setAnnotationTextSize();

        drawButton.setOnClickListener(this);
        clearButton.setOnClickListener(this);
        animateButton.setOnClickListener(this);

        unicodeNumber = getIntent().getExtras().getString(
                EXTRA_KANJI_UNICODE_NUMBER);
        kanji = getIntent().getExtras().getString(EXTRA_KANJI_GLYPH);

        String message = getResources().getString(R.string.sod_for);
        setTitle(String.format(message, kanji));

        // we need to call this here to initialize the loader
        // otherwise bad stuff happens: loader is not started, state is not
        // properly retained
        getStrokes();
    }

    private void setAnnotationTextSize() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        float annotationWidth = dm.scaledDensity * StrokePath.DEFAULT_ANNOTATION_TEXT_SIZE;
        strokeOrderView.setAnnotationTextSize(annotationWidth);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        drawSod();
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
        progressSpinner = (ProgressBar) findViewById(R.id.progress_spinner);
        strokeOrderView = (StrokeOrderView) findViewById(R.id.sod_draw_view);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.draw_sod_button) {
            drawSod();
        } else if (v.getId() == R.id.animate_button) {
            animate();
        } else if (v.getId() == R.id.clear_sod_button) {
            clear();
        }
    }

    private void clear() {
        strokeOrderView.clear();
        strokeOrderView.invalidate();
    }

    private void drawSod() {
        if (character == null) {
            getStrokes();
        } else {
            drawSod(character);
        }
    }

    private void getStrokes() {
        Bundle args = new Bundle();
        args.putBoolean("animate", false);
        args.putString("unicodeNumber", unicodeNumber);

        Loader<LoaderResult<Pair<String, Boolean>>> loader = LoaderManager.getInstance(this)
                .initLoader(0, args, this);
        if (loader.isStarted()) {
            showProgress();
        }
    }

    private void animate() {
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

        try {
            JSONObject jsonObj = new JSONObject(reply);
            JSONArray strokes = jsonObj.getJSONArray("paths");
            int numStrokes = strokes.length();
            List<StrokePath> result = new ArrayList<StrokePath>();
            for (int i = 0; i < numStrokes; i++) {
                String line = strokes.getString(i);
                if (line != null && !"".equals(line)) {
                    StrokePath strokePath = StrokePath.parsePath(line.trim());
                    result.add(strokePath);
                }
            }

            return result;
        } catch (JSONException e) {
            Log.w(TAG, "error parsing SOD: " + e.getMessage(), e);

            return null;
        }
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

    void showProgress() {
        progressSpinner.setVisibility(View.VISIBLE);
    }

    void dismissProgress() {
        progressSpinner.setVisibility(View.GONE);
    }

    @Override
    public Loader<LoaderResult<Pair<String, Boolean>>> onCreateLoader(int id,
                                                                      Bundle args) {
        String unicodeNumber = args.getString("unicodeNumber");
        boolean animate = args.getBoolean("animate");

        return new SodLoader(this, unicodeNumber, animate);
    }

    @Override
    public void onLoadFinished(
            Loader<LoaderResult<Pair<String, Boolean>>> loader,
            LoaderResult<Pair<String, Boolean>> loaderResult) {
        dismissProgress();

        if (loaderResult.isFailed()) {
            String message = getResources().getString(
                    R.string.getting_sod_data_failed,
                    loaderResult.getError().getMessage());
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

            return;
        }

        Pair<String, Boolean> result = loaderResult.getData();
        if (result == null) {
            Toast.makeText(this,
                    String.format(getString(R.string.no_sod_data), getKanji()),
                    Toast.LENGTH_SHORT).show();

            return;
        }

        String strokePathStr = result.getFirst();
        boolean animate = result.getSecond();

        setStrokePathsStr(strokePathStr);
        StrokedCharacter character = parseWsReply(strokePathStr);
        if (character != null) {
            if (animate) {
                animate(character);
            } else {
                drawSod(character);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult<Pair<String, Boolean>>> loader) {
        clear();
    }

}
