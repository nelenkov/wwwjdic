package org.nick.wwwjdic.sod;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.HttpClientFactory;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.Wwwjdic;
import org.nick.wwwjdic.WwwjdicPreferences;
import org.nick.wwwjdic.utils.Analytics;
import org.nick.wwwjdic.utils.LoaderBase;
import org.nick.wwwjdic.utils.LoaderResult;
import org.nick.wwwjdic.utils.Pair;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItem;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class SodActivity extends FragmentActivity implements OnClickListener,
        LoaderManager.LoaderCallbacks<LoaderResult<Pair<String, Boolean>>> {

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

            String responseStr = httpClient.execute(get,
                    HttpClientFactory.createWwwjdicResponseHandler());
            Log.d(TAG, "got SOD response: " + responseStr);

            return new Pair<String, Boolean>(responseStr, animate);
        }

        @Override
        protected boolean isActive(LoaderResult<Pair<String, Boolean>> result) {
            return false;
        }
    }

    private static final String TAG = SodActivity.class.getSimpleName();

    private static final String STROKE_PATH_LOOKUP_URL = "http://wwwjdic-android.appspot.com/kanji/";

    private static final String NOT_FOUND_STATUS = "not found";

    private static final float KANJIVG_SIZE = 109f;

    private Button drawButton;
    private Button clearButton;
    private Button animateButton;

    private StrokeOrderView strokeOrderView;

    private String unicodeNumber;
    private String kanji;

    private StrokedCharacter character;
    private String strokePathsStr;

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

        // we need to call this here to initialize the loader
        // otherwise bad stuff happens: loader is not started, state is not 
        // properly retained
        getStrokes();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
            clear();
            break;
        default:
            // do nothing
        }
    }

    private void clear() {
        strokeOrderView.clear();
        strokeOrderView.invalidate();
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
        Bundle args = new Bundle();
        args.putBoolean("animate", false);
        args.putString("unicodeNumber", unicodeNumber);

        Loader<LoaderResult<Pair<String, Boolean>>> loader = getSupportLoaderManager()
                .initLoader(0, args, this);
        if (loader.isStarted()) {
            String message = getResources()
                    .getString(R.string.getting_sod_info);
            showProgressDialog(message);
        }
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

        try {
            // XXX fixme: should return valid JSON even if not found
            if (reply.startsWith(NOT_FOUND_STATUS)) {
                return null;
            }

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
            // XXX do something smarter, need to show message if
            // format is wrong!
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
            LoaderResult<Pair<String, Boolean>> data) {
        dismissProgressDialog();

        boolean isFailed = data.isFailed();
        if (isFailed) {
            Toast.makeText(this, R.string.getting_sod_data_failed,
                    Toast.LENGTH_SHORT).show();

            return;
        }

        Pair<String, Boolean> result = data.getData();
        if (result != null) {
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
            } else {
                Toast t = Toast.makeText(this, String.format(
                        getString(R.string.no_sod_data), getKanji()),
                        Toast.LENGTH_SHORT);
                t.show();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult<Pair<String, Boolean>>> loader) {
        clear();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent intent = new Intent(this, Wwwjdic.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
