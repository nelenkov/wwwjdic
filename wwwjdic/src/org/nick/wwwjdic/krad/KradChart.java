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

import org.nick.wwwjdic.ActionBarActivity;
import org.nick.wwwjdic.CandidatesAdapter;
import org.nick.wwwjdic.KanjiResultList;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.Wwwjdic;
import org.nick.wwwjdic.hkr.HkrCandidates;
import org.nick.wwwjdic.model.SearchCriteria;
import org.nick.wwwjdic.utils.Dialogs;
import org.nick.wwwjdic.utils.UIUtils;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class KradChart extends ActionBarActivity implements OnClickListener,
        OnItemClickListener {

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

    private static final String STATE_KEY = "org.nick.wwwjdic.kradChartState";

    static class State implements Serializable {
        /**
         * 
         */
        private static final long serialVersionUID = -6074503793592867534L;

        Set<String> selectedRadicals = new HashSet<String>();
        Set<String> enabledRadicals = new HashSet<String>();
        Set<String> matchingKanjis = new HashSet<String>();
        List<String> radicals = new ArrayList<String>();
    }

    private State state = new State();

    private Gallery candidatesGallery;
    private TextView totalMatchesText;
    private Button showAllButton;
    private Button clearButton;

    private GridView radicalChartGrid;
    private KradAdapter adapter;

    private String[] candidates = new String[0];

    private KradDb kradDb;

    private ProgressBar progressSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.krad_chart);

        candidatesGallery = (Gallery) findViewById(R.id.candidates_gallery);
        candidatesGallery.setOnItemClickListener(this);
        candidatesGallery.setGravity(Gravity.CENTER_VERTICAL);
        candidatesGallery.setSelected(true);

        totalMatchesText = (TextView) findViewById(R.id.total_matches);
        displayTotalMatches();

        showAllButton = (Button) findViewById(R.id.show_all_button);
        showAllButton.setOnClickListener(this);
        clearButton = (Button) findViewById(R.id.clear_button);
        clearButton.setOnClickListener(this);
        toggleButtons();

        progressSpinner = (ProgressBar) findViewById(R.id.progress_spinner);
        radicalChartGrid = (GridView) findViewById(R.id.kradChartGrid);
        radicalChartGrid.setOnItemClickListener(this);

        setTitle(R.string.kanji_multi_radical_search);

        kradDb = KradDb.getInstance();
        if (savedInstanceState != null) {
            state = (State) savedInstanceState.getSerializable(STATE_KEY);

            enableAllRadicals();
            adapter = new KradAdapter(KradChart.this, R.layout.krad_item,
                    state.radicals);
            radicalChartGrid.setAdapter(adapter);

            updateRadicalsAndMatches();

            return;
        }

        new AsyncTask<Void, Void, Boolean>() {

            private Throwable error;

            @Override
            protected void onPreExecute() {
                progressSpinner.setVisibility(View.VISIBLE);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    try {
                        initKradDb();

                        for (String numStrokesStr : NUM_STROKES) {
                            String labelStr = new String(numStrokesStr);
                            state.radicals.add(labelStr);

                            String arrayName = "_" + numStrokesStr + "_stroke";
                            Field field = R.array.class.getField(arrayName);
                            int resourceId = (Integer) field.get(null);
                            String[] radicalArr = getResources()
                                    .getStringArray(resourceId);
                            state.radicals.addAll(Arrays.asList(radicalArr));
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
                progressSpinner.setVisibility(View.INVISIBLE);

                if (isFinishing()) {
                    return;
                }

                if (result) {
                    enableAllRadicals();
                    adapter = new KradAdapter(KradChart.this,
                            R.layout.krad_item, state.radicals);
                    radicalChartGrid.setAdapter(adapter);

                    Dialogs.showTipOnce(KradChart.this, MULTI_RADICAL_TIP,
                            R.string.multi_radical_search_tip);
                } else {
                    Toast.makeText(KradChart.this,
                            "error loading radkfile-u " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private void setupGallerySelection() {
        if (candidatesGallery != null && candidatesGallery.getAdapter() != null) {
            int numItems = candidatesGallery.getAdapter().getCount();
            Display display = getWindowManager().getDefaultDisplay();
            int galleryWidth = display.getWidth();
            if (numItems > 0 && galleryWidth > 0) {
                DisplayMetrics dm = new DisplayMetrics();
                display.getMetrics(dm);

                // 8 is padding, etc.
                int itemWidth = (int) (getResources().getDimension(
                        R.dimen.candidates_item_size) + dm.density * 8);
                int numVisibleItems = galleryWidth / itemWidth;
                int selectionIdx = numVisibleItems / 2;
                if (numItems <= selectionIdx) {
                    selectionIdx = numItems / 2;
                }
                candidatesGallery.setSelection(selectionIdx);
            }
        }
    }

    private void showCandidates(String[] candidates) {
        if (candidates != null) {
            CandidatesAdapter adapter = new CandidatesAdapter(this,
                    R.layout.candidates_item,
                    candidates);

            candidatesGallery.setAdapter(adapter);
            setupGallerySelection();
        }
    }

    private void clearCandidates() {
        candidates = new String[0];
        if (candidatesGallery != null) {
            CandidatesAdapter adapter = new CandidatesAdapter(this,
                    R.layout.candidates_item, candidates);
            candidatesGallery.setAdapter(adapter);
        }
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

    @Override
    protected void onStart() {
        super.onStart();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupGallerySelection();
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
        for (String radical : state.radicals) {
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
            UIUtils.setJpTextLocale(result);
            result.setTextColor(UIUtils.fetchOnBackgroundColor(getApplicationContext()));
            result.setBackgroundColor(Color.TRANSPARENT);
            float textSize = getResources().getDimension(
                    R.dimen.krad_chart_text_size);
            result.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

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
        if (parent.getId() == R.id.candidates_gallery) {
            if (position >= 0 && position < candidates.length) {
                String kanji = candidates[position];
                Intent intent = createCharDetailsIntent(kanji);
                startActivity(intent);
            }
        } else if (parent.getId() == R.id.kradChartGrid) {
            String radical = state.radicals.get(position).trim();
            if (state.selectedRadicals.contains(radical)) {
                state.selectedRadicals.remove(radical);
            } else {
                state.selectedRadicals.add(radical);
            }
            updateRadicalsAndMatches();
        }
    }

    private void updateRadicalsAndMatches() {
        if (state.selectedRadicals.isEmpty()) {
            enableAllRadicals();
            state.matchingKanjis.clear();
            clearCandidates();
        } else {
            state.matchingKanjis = kradDb
                    .getKanjisForRadicals(state.selectedRadicals);
            candidates = state.matchingKanjis
                    .toArray(new String[state.matchingKanjis.size()]);
            Arrays.sort(candidates);
            showCandidates(candidates);

            Log.d(TAG, "matching kanjis: " + state.matchingKanjis);
            state.enabledRadicals = kradDb
                    .getRadicalsForKanjis(state.matchingKanjis);
            Log.d(TAG, "enabled radicals: " + state.enabledRadicals);
        }

        toggleButtons();

        displayTotalMatches();

        adapter.notifyDataSetChanged();
    }

    private Intent createCharDetailsIntent(String kanji) {
        Intent intent = new Intent(this, KanjiResultList.class);
        SearchCriteria criteria = SearchCriteria.createForKanjiOrReading(kanji);
        intent.putExtra(Wwwjdic.EXTRA_CRITERIA, criteria);

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
        if (view.getId() == R.id.show_all_button) {
            showCandidates();
        } else if (view.getId() == R.id.clear_button) {
            clearSelection();
        }
    }

    private void clearSelection() {
        state.selectedRadicals.clear();
        state.matchingKanjis.clear();
        enableAllRadicals();

        clearCandidates();
        displayTotalMatches();

        toggleButtons();

        adapter.notifyDataSetChanged();
    }

    private void showCandidates() {
        Intent intent = createShowAllIntent();
        startActivity(intent);
    }

    private Intent createShowAllIntent() {
        String[] matchingChars = state.matchingKanjis
                .toArray(new String[state.matchingKanjis.size()]);
        Arrays.sort(matchingChars);

        Intent intent = new Intent(this, HkrCandidates.class);
        intent.putExtra(HkrCandidates.EXTRA_HKR_CANDIDATES, matchingChars);
        return intent;
    }


}
