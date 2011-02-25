package org.nick.wwwjdic.krad;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.KanjiResultListView;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.SearchCriteria;
import org.nick.wwwjdic.hkr.HkrCandidates;
import org.nick.wwwjdic.utils.Analytics;
import org.nick.wwwjdic.utils.Dialogs;
import org.nick.wwwjdic.utils.IntentSpan;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

public class KradChart extends Activity implements OnClickListener,
        OnItemClickListener {

    private static final int NUM_SUMMARY_CHARS = 10;

    private static final String TAG = KradChart.class.getSimpleName();

    private static final String MULTI_RADICAL_TIP = "multi_radical_tip";

    private static final List<String> NUM_STROKES = Arrays.asList(new String[] {
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12",
            "13", "14", "17" });

    private static final Map<String, String> KRAD_TO_DISPLAY = new HashMap<String, String>();
    static {
        KRAD_TO_DISPLAY.put("⺅", "亻");
        KRAD_TO_DISPLAY.put("⺾", "艹");
        KRAD_TO_DISPLAY.put("辶", "辶");
        KRAD_TO_DISPLAY.put("⻏", "邦");
        KRAD_TO_DISPLAY.put("⻖", "阡");
        KRAD_TO_DISPLAY.put("⺌", "尚");
        KRAD_TO_DISPLAY.put("𠆢", "个");
        KRAD_TO_DISPLAY.put("⺹", "耂");
    }
    private static final List<String> REPLACED_CHARS = Arrays
            .asList(new String[] { "邦", "阡", "尚", "个" });

    private List<String> radicals = new ArrayList<String>();

    private static final String STATE_KEY = "org.nick.wwwjdic.kradChartState";

    static class State implements Serializable {
        /**
         * 
         */
        private static final long serialVersionUID = -6074503793592867534L;

        Set<String> selectedRadicals = new HashSet<String>();
        Set<String> enabledRadicals = new HashSet<String>();
        Set<String> matchingKanjis = new HashSet<String>();
    }

    private State state = new State();

    private TextView matchedKanjiText;
    private TextView totalMatchesText;
    private Button showAllButton;
    private Button clearButton;

    private GridView radicalChartGrid;
    private KradAdapter adapter;

    private KradDb kradDb = new KradDb();

    private ProgressDialog progressDialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.krad_chart);

        matchedKanjiText = (TextView) findViewById(R.id.matched_kanji);
        totalMatchesText = (TextView) findViewById(R.id.total_matches);
        displayTotalMatches();

        showAllButton = (Button) findViewById(R.id.show_all_button);
        showAllButton.setOnClickListener(this);
        clearButton = (Button) findViewById(R.id.clear_button);
        clearButton.setOnClickListener(this);
        toggleButtons();

        radicalChartGrid = (GridView) findViewById(R.id.kradChartGrid);
        radicalChartGrid.setOnItemClickListener(this);

        setTitle(R.string.kanji_multi_radical_search);

        new AsyncTask<Void, Void, Boolean>() {

            private Throwable error;

            @Override
            protected void onPreExecute() {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();

                }
                progressDialog = new ProgressDialog(KradChart.this);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage(getString(R.string.loading));
                progressDialog.setCancelable(false);
                progressDialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    try {
                        initKradDb();

                        for (String numStrokesStr : NUM_STROKES) {
                            String labelStr = new String(numStrokesStr);
                            radicals.add(labelStr);

                            String arrayName = "_" + numStrokesStr + "_stroke";
                            Field field = R.array.class.getField(arrayName);
                            int resourceId = (Integer) field.get(null);
                            String[] radicalArr = getResources()
                                    .getStringArray(resourceId);
                            radicals.addAll(Arrays.asList(radicalArr));
                        }
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }

                    return true;
                } catch (Exception e) {
                    error = e;
                    Log.d(TAG, "Error loading radkfile-u", e);

                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();

                    if (result) {
                        enableAllRadicals();
                        adapter = new KradAdapter(KradChart.this,
                                R.layout.krad_item, radicals);
                        radicalChartGrid.setAdapter(adapter);

                        Dialogs.showTipOnce(KradChart.this, MULTI_RADICAL_TIP,
                                R.string.multi_radical_search_tip);
                    } else {
                        Toast t = Toast.makeText(
                                KradChart.this,
                                "error loading radkfile-u "
                                        + error.getMessage(),
                                Toast.LENGTH_SHORT);
                        t.show();
                    }
                }
            }
        }.execute();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        state = (State) savedInstanceState.getSerializable(STATE_KEY);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(STATE_KEY, state);
    }


    private void displayTotalMatches() {
        String totalMatchesTemplate = getResources().getString(
                R.string.total_matches);
        totalMatchesText.setText(String.format(totalMatchesTemplate,
                state.matchingKanjis.size()));
    }

    private void initKradDb() {
        if (!kradDb.isInitialized()) {
            try {
                InputStream in = getAssets().open("radkfile-u-jis208.txt");
                kradDb.readFromStream(in);
            } catch (IOException e) {
                Log.e(TAG, "error reading radkfile-u", e);
                throw new RuntimeException(e);
            }
        }
    }

    private void enableAllRadicals() {
        for (String radical : radicals) {
            if (!isStrokeNumLabel(radical)) {
                state.enabledRadicals.add(radical.trim());
            }
        }
    }

    public class KradAdapter extends ArrayAdapter<String> {

        public KradAdapter(Context context, int textViewResourceId,
                List<String> objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            TextView result = (TextView) super.getView(position, convertView,
                    viewGroup);
            result.setTextColor(Color.WHITE);
            result.setBackgroundColor(Color.TRANSPARENT);
            result.setTextSize(24f);

            String modelStr = getItem(position);
            if (isStrokeNumLabel(modelStr)) {
                result.setBackgroundColor(Color.GRAY);
            } else {
                String radical = modelStr.trim();
                String displayStr = toDisplayStr(radical);
                result.setText(displayStr);
                if (REPLACED_CHARS.contains(displayStr)) {
                    result.setTextColor(Color.LTGRAY);
                }
            }

            if (isSelected(modelStr)) {
                result.setBackgroundColor(Color.GREEN);
            }

            if (isDisabled(modelStr)) {
                result.setBackgroundColor(Color.DKGRAY);
            }

            return result;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            String text = getItem(position);
            return !isStrokeNumLabel(text) && !isDisabled(text);
        }
    }

    private static boolean isStrokeNumLabel(String str) {
        return NUM_STROKES.contains(str.trim());
    }

    private boolean isSelected(String radical) {
        return state.selectedRadicals.contains(radical);
    }

    private boolean isDisabled(String radical) {
        return !isStrokeNumLabel(radical)
                && !state.enabledRadicals.contains(radical);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        Analytics.event("multiradicalSelect", this);

        String radical = radicals.get(position).trim();
        if (state.selectedRadicals.contains(radical)) {
            state.selectedRadicals.remove(radical);
        } else {
            state.selectedRadicals.add(radical);
        }

        if (state.selectedRadicals.isEmpty()) {
            enableAllRadicals();
            state.matchingKanjis.clear();
            matchedKanjiText.setText("");
        } else {
            state.matchingKanjis = kradDb
                    .getKanjisForRadicals(state.selectedRadicals);
            addClickableKanji(matchedKanjiText);

            Log.d(TAG, "matching kanjis: " + state.matchingKanjis);
            state.enabledRadicals = kradDb
                    .getRadicalsForKanjis(state.matchingKanjis);
            Log.d(TAG, "enabled radicals: " + state.enabledRadicals);
        }

        toggleButtons();

        displayTotalMatches();

        adapter.notifyDataSetChanged();
    }

    private void addClickableKanji(TextView textView) {
        if (state.matchingKanjis.isEmpty()) {
            return;
        }

        String[] matchingChars = state.matchingKanjis
                .toArray(new String[state.matchingKanjis.size()]);
        Arrays.sort(matchingChars);

        String[] charsToDisplay = new String[NUM_SUMMARY_CHARS];
        if (matchingChars.length < charsToDisplay.length) {
            charsToDisplay = new String[matchingChars.length];
        }
        System.arraycopy(matchingChars, 0, charsToDisplay, 0,
                charsToDisplay.length);
        String text = TextUtils.join(" ", charsToDisplay);
        String ellipsis = "...";
        if (matchingChars.length > charsToDisplay.length) {
            text += " " + ellipsis;
        }
        SpannableString str = new SpannableString(text);

        for (String c : charsToDisplay) {
            int idx = text.indexOf(c);
            if (idx != -1) {
                Intent intent = createCharDetailsIntent(c);
                int end = idx + 1;
                if (end > str.length() - 1) {
                    end = str.length();
                }
                str.setSpan(new IntentSpan(this, intent), idx, idx + 1,
                        Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            }
        }

        int idx = text.indexOf(ellipsis);
        if (idx != -1) {
            Intent intent = createShowAllIntent();
            str.setSpan(new IntentSpan(this, intent), idx,
                    idx + ellipsis.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        }

        textView.setText(str);
        textView.setLinkTextColor(Color.WHITE);
        MovementMethod m = textView.getMovementMethod();
        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            if (textView.getLinksClickable()) {
                textView.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }

    private Intent createCharDetailsIntent(String kanji) {
        Intent intent = new Intent(this, KanjiResultListView.class);
        SearchCriteria criteria = SearchCriteria.createForKanjiOrReading(kanji);
        intent.putExtra(Constants.CRITERIA_KEY, criteria);

        return intent;
    }

    private void toggleButtons() {
        boolean matchesFound = !state.matchingKanjis.isEmpty();
        showAllButton.setEnabled(matchesFound);
        clearButton.setEnabled(matchesFound);
    }

    private static String toDisplayStr(String radical) {
        String displayChar = KRAD_TO_DISPLAY.get(radical);
        if (displayChar == null) {
            displayChar = radical;
        }

        if (displayChar != radical) {
            Log.d(TAG, String.format("%s %s", radical, displayChar));
        }

        return displayChar;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.show_all_button:
            showCandidates();
            break;
        case R.id.clear_button:
            clearSelection();
            break;
        default:
            // do nothing
        }
    }

    private void clearSelection() {
        state.selectedRadicals.clear();
        state.matchingKanjis.clear();
        enableAllRadicals();

        matchedKanjiText.setText("");
        displayTotalMatches();

        toggleButtons();

        adapter.notifyDataSetChanged();
    }

    private void showCandidates() {
        Analytics.event("multiradicalShowAll", this);

        Intent intent = createShowAllIntent();
        startActivity(intent);
    }

    private Intent createShowAllIntent() {
        String[] matchingChars = state.matchingKanjis
                .toArray(new String[state.matchingKanjis.size()]);
        Arrays.sort(matchingChars);

        Intent intent = new Intent(this, HkrCandidates.class);
        intent.putExtra(Constants.HKR_CANDIDATES_KEY, matchingChars);
        return intent;
    }
}
