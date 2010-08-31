package org.nick.wwwjdic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.nick.wwwjdic.history.FavoritesAndHistory;
import org.nick.wwwjdic.history.FavoritesAndHistorySummaryView;
import org.nick.wwwjdic.history.HistoryDbHelper;
import org.nick.wwwjdic.ocr.OcrActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView.OnEditorActionListener;

public class Wwwjdic extends TabActivity implements OnClickListener,
        OnFocusChangeListener, OnCheckedChangeListener, OnItemSelectedListener {

    private static final int NUM_RECENT_HISTORY_ENTRIES = 5;

    private static final int ITEM_ID_ABOUT = 1;
    private static final int ITEM_ID_OCR = 2;
    private static final int ITEM_ID_SETTINGS = 3;
    private static final int ITEM_ID_DRAW = 4;
    private static final int ITEM_ID_HISTORY = 5;

    private static final int WHATS_NEW_DIALOG_ID = 1;

    private static final int DICTIONARY_TAB_IDX = 0;
    private static final String DICTIONARY_TAB = "dictionaryTab";
    private static final int KANJI_TAB_IDX = 1;
    private static final String KANJI_TAB = "kanjiTab";
    private static final int EXAMPLE_SEARRCH_TAB_IDX = 2;
    private static final String EXAMPLE_SEARCH_TAB = "exampleSearchTab";

    private static final String TAG = "WWWJDIC";

    private static final String PREF_WHATS_NEW_SHOWN = "pref_whats_new_shown";

    private static final Map<Integer, String> IDX_TO_DICT = new HashMap<Integer, String>();

    static {
        IDX_TO_DICT.put(0, "1");
        IDX_TO_DICT.put(1, "2");
        IDX_TO_DICT.put(2, "3");
        IDX_TO_DICT.put(3, "4");
        IDX_TO_DICT.put(4, "5");
        IDX_TO_DICT.put(5, "6");
        IDX_TO_DICT.put(6, "7");
        IDX_TO_DICT.put(7, "8");
        IDX_TO_DICT.put(8, "A");
        IDX_TO_DICT.put(9, "B");
        IDX_TO_DICT.put(10, "C");
        IDX_TO_DICT.put(11, "D");
        IDX_TO_DICT.put(12, "F");
        IDX_TO_DICT.put(13, "G");
        IDX_TO_DICT.put(14, "H");
        IDX_TO_DICT.put(15, "I");
        IDX_TO_DICT.put(16, "J");
        IDX_TO_DICT.put(17, "K");
        IDX_TO_DICT.put(18, "L");
        IDX_TO_DICT.put(19, "M");
    }

    private static final Map<Integer, String> IDX_TO_CODE = new HashMap<Integer, String>();

    static {
        // Kanji or reading
        IDX_TO_CODE.put(0, "J");
        // Stroke count
        IDX_TO_CODE.put(1, "C");
        // Radical number
        IDX_TO_CODE.put(2, "B");
        // English meaning
        IDX_TO_CODE.put(3, "E");
        // Unicode code (hex)
        IDX_TO_CODE.put(4, "U");
        // JIS code
        IDX_TO_CODE.put(5, "J");
        // SKIP code
        IDX_TO_CODE.put(6, "P");
        // Pinyin reading
        IDX_TO_CODE.put(7, "Y");
        // Korean reading
        IDX_TO_CODE.put(8, "W");
    }

    private EditText inputText;
    private CheckBox exactMatchCb;
    private CheckBox commonWordsCb;
    private CheckBox romanizedJapaneseCb;
    private Spinner dictSpinner;

    private EditText kanjiInputText;
    private Spinner kanjiSearchTypeSpinner;

    private EditText radicalEditText;
    private Button selectRadicalButton;
    private EditText strokeCountMinInput;
    private EditText strokeCountMaxInput;

    private EditText exampleSearchInputText;
    private EditText maxNumExamplesText;
    private CheckBox exampleExactMatchCb;
    private Spinner sentenceModeSpinner;
    private Button exampleSearchButton;

    private TabHost tabHost;

    private FavoritesAndHistorySummaryView dictHistorySummary;
    private FavoritesAndHistorySummaryView kanjiHistorySummary;
    private FavoritesAndHistorySummaryView examplesHistorySummary;

    private boolean inputTextFromBundle;

    private HistoryDbHelper dbHelper;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.main);

        setupTabs();
        findViews();
        setupListeners();
        setupSpinners();
        setupTabOrder();
        toggleRadicalStrokeCountPanel(false);

        inputText.requestFocus();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String searchKey = extras.getString(Constants.SEARCH_TEXT_KEY);
            int searchType = extras.getInt(Constants.SEARCH_TYPE);
            if (searchKey != null) {
                switch (searchType) {
                case SearchCriteria.CRITERIA_TYPE_DICT:
                    inputText.setText(searchKey);
                    tabHost.setCurrentTab(0);
                    break;
                case SearchCriteria.CRITERIA_TYPE_KANJI:
                    kanjiInputText.setText(searchKey);
                    tabHost.setCurrentTab(1);
                    break;
                case SearchCriteria.CRITERIA_TYPE_EXAMPLES:
                    exampleSearchInputText.setText(searchKey);
                    tabHost.setCurrentTab(2);
                    break;
                default:
                    // do nothing
                }
                inputTextFromBundle = true;
            }
        }

        dbHelper = new HistoryDbHelper(this);

        setupFavoritesAndHistorySummary();

        showWhatsNew();
    }

    private void setupFavoritesAndHistorySummary() {
        setupDictSummary();

        setupKanjiSummary();

        setupExamplesSummary();
    }

    private void setupDictSummary() {
        dbHelper.beginTransaction();
        try {
            long numAllFavorites = dbHelper.getDictFavoritesCount();
            List<String> recentFavorites = dbHelper
                    .getRecentDictFavorites(NUM_RECENT_HISTORY_ENTRIES);
            long numAllHistory = dbHelper.getDictHistoryCount();
            List<String> recentHistory = dbHelper
                    .getRecentDictHistory(NUM_RECENT_HISTORY_ENTRIES);
            dictHistorySummary
                    .setFavoritesFilterType(HistoryDbHelper.FAVORITES_TYPE_DICT);
            dictHistorySummary
                    .setHistoryFilterType(HistoryDbHelper.HISTORY_SEARCH_TYPE_DICT);
            dictHistorySummary.setRecentEntries(numAllFavorites,
                    recentFavorites, numAllHistory, recentHistory);
            dbHelper.setTransactionSuccessful();
        } finally {
            dbHelper.endTransaction();
        }
    }

    private void setupKanjiSummary() {
        dbHelper.beginTransaction();
        try {
            long numAllFavorites = dbHelper.getKanjiFavoritesCount();
            List<String> recentFavorites = dbHelper
                    .getRecentKanjiFavorites(NUM_RECENT_HISTORY_ENTRIES);
            long numAllHistory = dbHelper.getKanjiHistoryCount();
            List<String> recentHistory = dbHelper
                    .getRecentKanjiHistory(NUM_RECENT_HISTORY_ENTRIES);

            kanjiHistorySummary
                    .setFavoritesFilterType(HistoryDbHelper.FAVORITES_TYPE_KANJI);
            kanjiHistorySummary
                    .setHistoryFilterType(HistoryDbHelper.HISTORY_SEARCH_TYPE_KANJI);
            kanjiHistorySummary.setRecentEntries(numAllFavorites,
                    recentFavorites, numAllHistory, recentHistory);
            dbHelper.setTransactionSuccessful();
        } finally {
            dbHelper.endTransaction();
        }
    }

    private void setupExamplesSummary() {
        dbHelper.beginTransaction();
        try {
            long numAllHistory = dbHelper.getExamplesHistoryCount();
            List<String> recentHistory = dbHelper
                    .getRecentExamplesHistory(NUM_RECENT_HISTORY_ENTRIES);

            examplesHistorySummary
                    .setHistoryFilterType(HistoryDbHelper.HISTORY_SEARCH_TYPE_EXAMPLES);
            examplesHistorySummary.setRecentEntries(0, null, numAllHistory,
                    recentHistory);
            dbHelper.setTransactionSuccessful();
        } finally {
            dbHelper.endTransaction();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        int currentTab = tabHost.getCurrentTab();
        switch (currentTab) {
        case DICTIONARY_TAB_IDX:
            setupDictSummary();
            break;
        case KANJI_TAB_IDX:
            setupKanjiSummary();
            break;
        case EXAMPLE_SEARRCH_TAB_IDX:
            setupExamplesSummary();
            break;
        default:
            // do nothing
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Analytics.startSession(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Analytics.endSession(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void showWhatsNew() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        String key = PREF_WHATS_NEW_SHOWN + "_" + getVersionName();
        boolean whatsNewShown = prefs.getBoolean(key, false);
        if (!whatsNewShown) {
            prefs.edit().putBoolean(key, true).commit();
            showDialog(WHATS_NEW_DIALOG_ID);
        }

    }

    private void setupListeners() {
        View translateButton = findViewById(R.id.translateButton);
        translateButton.setOnClickListener(this);

        View kanjiSearchButton = findViewById(R.id.kanjiSearchButton);
        kanjiSearchButton.setOnClickListener(this);

        inputText.setOnFocusChangeListener(this);
        kanjiInputText.setOnFocusChangeListener(this);

        romanizedJapaneseCb.setOnCheckedChangeListener(this);
        exactMatchCb.setOnCheckedChangeListener(this);
        commonWordsCb.setOnCheckedChangeListener(this);

        selectRadicalButton.setOnClickListener(this);

        View exampleSearchButton = findViewById(R.id.exampleSearchButton);
        exampleSearchButton.setOnClickListener(this);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.dictionaries_array, R.layout.spinner_text);
        adapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dictSpinner.setAdapter(adapter);

        ArrayAdapter<CharSequence> kajiSearchTypeAdapter = ArrayAdapter
                .createFromResource(this, R.array.kanji_search_types_array,
                        R.layout.spinner_text);
        kajiSearchTypeAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        kanjiSearchTypeSpinner.setAdapter(kajiSearchTypeAdapter);
        kanjiSearchTypeSpinner.setOnItemSelectedListener(this);

        ArrayAdapter<CharSequence> sentenceModeAdapter = ArrayAdapter
                .createFromResource(this, R.array.sentence_modes,
                        R.layout.spinner_text);
        sentenceModeAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sentenceModeSpinner.setAdapter(sentenceModeAdapter);
        sentenceModeSpinner.setOnItemSelectedListener(this);
    }

    private void setupTabOrder() {
        strokeCountMinInput
                .setOnEditorActionListener(new OnEditorActionListener() {
                    public boolean onEditorAction(TextView v, int actionId,
                            KeyEvent event) {
                        switch (actionId) {
                        case EditorInfo.IME_ACTION_NEXT:
                            EditText v1 = (EditText) v
                                    .focusSearch(View.FOCUS_RIGHT);
                            if (v1 != null) {
                                if (!v1.requestFocus(View.FOCUS_RIGHT)) {
                                    throw new IllegalStateException(
                                            "unfocucsable view");
                                }
                            }
                            break;
                        default:
                            break;
                        }
                        return true;
                    }
                });
    }

    private void setupTabs() {
        tabHost = getTabHost();

        tabHost.addTab(tabHost.newTabSpec(DICTIONARY_TAB).setIndicator(
                getResources().getText(R.string.dictionary),
                getResources().getDrawable(R.drawable.ic_tab_dict)).setContent(
                R.id.dictLookupTab));
        tabHost.addTab(tabHost.newTabSpec(KANJI_TAB).setIndicator(
                getResources().getText(R.string.kanji_lookup),
                getResources().getDrawable(R.drawable.ic_tab_kanji))
                .setContent(R.id.kanjiLookupTab));
        tabHost.addTab(tabHost.newTabSpec(EXAMPLE_SEARCH_TAB).setIndicator(
                getResources().getText(R.string.example_search),
                getResources().getDrawable(R.drawable.ic_tab_example))
                .setContent(R.id.exampleSearchTab));

        tabHost.setCurrentTab(DICTIONARY_TAB_IDX);
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.translateButton:
            hideKeyboard();

            String input = inputText.getText().toString();

            try {
                int dictIdx = dictSpinner.getSelectedItemPosition();
                String dict = IDX_TO_DICT.get(dictIdx);
                Log.i(TAG, Integer.toString(dictIdx));
                Log.i(TAG, dict);
                if (dict == null) {
                    // edict
                    dict = "1";
                }

                SearchCriteria criteria = SearchCriteria.createForDictionary(
                        input, exactMatchCb.isChecked(), romanizedJapaneseCb
                                .isChecked(), commonWordsCb.isChecked(), dict);

                Intent intent = new Intent(this, DictionaryResultListView.class);
                intent.putExtra(Constants.CRITERIA_KEY, criteria);

                if (!StringUtils.isEmpty(criteria.getQueryString())) {
                    dbHelper.addSearchCriteria(criteria);
                }

                Analytics.event("dictSearch", this);

                startActivity(intent);
            } catch (RejectedExecutionException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            break;
        case R.id.kanjiSearchButton:
            hideKeyboard();

            String kanjiInput = kanjiInputText.getText().toString();

            try {
                int searchTypeIdx = kanjiSearchTypeSpinner
                        .getSelectedItemPosition();
                String searchType = IDX_TO_CODE.get(searchTypeIdx);
                Log.i(TAG, Integer.toString(searchTypeIdx));
                Log.i(TAG, "kanji search type: " + searchType);
                if (searchType == null) {
                    // reading/kanji
                    searchType = "J";
                }

                String minStr = strokeCountMinInput.getText().toString();
                String maxStr = strokeCountMaxInput.getText().toString();
                Integer minStrokeCount = tryParseInt(minStr);
                Integer maxStrokeCount = tryParseInt(maxStr);
                SearchCriteria criteria = SearchCriteria.createWithStrokeCount(
                        kanjiInput, searchType, minStrokeCount, maxStrokeCount);

                Intent intent = new Intent(this, KanjiResultListView.class);
                intent.putExtra(Constants.CRITERIA_KEY, criteria);

                if (!StringUtils.isEmpty(criteria.getQueryString())) {
                    dbHelper.addSearchCriteria(criteria);
                }

                Analytics.event("kanjiSearch", this);

                startActivity(intent);
            } catch (RejectedExecutionException e) {
                Log.e(TAG, "RejectedExecutionException", e);
            }
            break;
        case R.id.selectRadicalButton:
            Intent i = new Intent(this, RadicalChart.class);

            startActivityForResult(i, Constants.RADICAL_RETURN_RESULT);
            break;
        case R.id.exampleSearchButton:
            hideKeyboard();

            String queryString = exampleSearchInputText.getText().toString();
            if (sentenceModeSpinner.getSelectedItemPosition() == 0) {
                int numMaxResults = Integer.parseInt(maxNumExamplesText
                        .getText().toString());
                SearchCriteria criteria = SearchCriteria
                        .createForExampleSearch(queryString,
                                exampleExactMatchCb.isChecked(), numMaxResults);

                Intent intent = new Intent(this, ExamplesResultListView.class);
                intent.putExtra(Constants.CRITERIA_KEY, criteria);

                if (!StringUtils.isEmpty(criteria.getQueryString())) {
                    dbHelper.addSearchCriteria(criteria);
                }

                Analytics.event("exampleSearch", this);

                startActivity(intent);
            } else {
                Intent intent = new Intent(this, SentenceBreakdown.class);
                intent.putExtra(Constants.SENTENCE, queryString);
                startActivity(intent);
            }
            break;
        default:
            // do nothing
        }
    }

    private Integer tryParseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent intent) {
        if (requestCode == Constants.RADICAL_RETURN_RESULT) {
            if (resultCode == RESULT_OK) {
                Radical radical = (Radical) intent.getExtras().getSerializable(
                        Constants.RADICAL_KEY);
                kanjiInputText.setText(Integer.toString(radical.getNumber()));
                radicalEditText.setText(radical.getGlyph().substring(0, 1));
            }
        }
    }

    public void onFocusChange(View v, boolean hasFocus) {
        switch (v.getId()) {
        case R.id.inputText:
            if (hasFocus) {
                showKeyboard();
            } else {
                hideKeyboard();
            }
            break;
        default:
            // do nothing
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ITEM_ID_OCR, 0, R.string.ocr).setIcon(
                android.R.drawable.ic_menu_camera);
        menu.add(0, ITEM_ID_DRAW, 1, R.string.write_kanji).setIcon(
                android.R.drawable.ic_menu_edit);
        menu.add(0, ITEM_ID_HISTORY, 2, R.string.favorites_hist).setIcon(
                android.R.drawable.ic_menu_recent_history);
        menu.add(0, ITEM_ID_SETTINGS, 3, R.string.settings).setIcon(
                android.R.drawable.ic_menu_preferences);
        menu.add(0, ITEM_ID_ABOUT, 4, R.string.about).setIcon(
                android.R.drawable.ic_menu_info_details);

        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
        case ITEM_ID_ABOUT:
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        case ITEM_ID_OCR:
            intent = new Intent(this, OcrActivity.class);

            startActivity(intent);
            return true;
        case ITEM_ID_SETTINGS:
            intent = new Intent(this, WwwjdicPreferences.class);

            startActivity(intent);
            return true;
        case ITEM_ID_DRAW:
            intent = new Intent("org.nick.wwwjdic.RECOGNIZE_KANJI");

            startActivity(intent);
            return true;
        case ITEM_ID_HISTORY:
            intent = new Intent(this, FavoritesAndHistory.class);

            startActivity(intent);
            return true;
        default:
            // do nothing
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;

        switch (id) {
        case WHATS_NEW_DIALOG_ID:
            dialog = createWhatsNewDialog();
            break;
        default:
            dialog = null;
        }

        return dialog;
    }

    private Dialog createWhatsNewDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.whats_new);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                    }
                });
        String titleTemplate = getResources().getString(
                R.string.whats_new_title);
        String title = String.format(titleTemplate, getVersionName());
        builder.setTitle(title);
        builder.setIcon(android.R.drawable.ic_dialog_info);

        return builder.create();
    }

    private String getVersionName() {
        return WwwjdicApplication.getVersion();
    }

    private void hideKeyboard() {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(inputText.getWindowToken(), 0);
    }

    private void showKeyboard() {
        EditText editText = (EditText) findViewById(R.id.inputText);
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    private void findViews() {
        inputText = (EditText) findViewById(R.id.inputText);
        exactMatchCb = (CheckBox) findViewById(R.id.exactMatchCb);
        commonWordsCb = (CheckBox) findViewById(R.id.commonWordsCb);
        romanizedJapaneseCb = (CheckBox) findViewById(R.id.romanizedCb);
        dictSpinner = (Spinner) findViewById(R.id.dictionarySpinner);
        kanjiInputText = (EditText) findViewById(R.id.kanjiInputText);
        kanjiSearchTypeSpinner = (Spinner) findViewById(R.id.kanjiSearchTypeSpinner);

        radicalEditText = (EditText) findViewById(R.id.radicalInputText);
        strokeCountMinInput = (EditText) findViewById(R.id.strokeCountMinInput);
        strokeCountMaxInput = (EditText) findViewById(R.id.strokeCountMaxInput);
        selectRadicalButton = (Button) findViewById(R.id.selectRadicalButton);

        exampleSearchInputText = (EditText) findViewById(R.id.exampleInputText);
        maxNumExamplesText = (EditText) findViewById(R.id.maxExamplesInput);
        exampleExactMatchCb = (CheckBox) findViewById(R.id.exampleExactMatchCb);
        sentenceModeSpinner = (Spinner) findViewById(R.id.modeSpinner);
        exampleSearchButton = (Button) findViewById(R.id.exampleSearchButton);

        dictHistorySummary = (FavoritesAndHistorySummaryView) findViewById(R.id.dict_history_summary);
        kanjiHistorySummary = (FavoritesAndHistorySummaryView) findViewById(R.id.kanji_history_summary);
        examplesHistorySummary = (FavoritesAndHistorySummaryView) findViewById(R.id.examples_history_summary);
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
        case R.id.exactMatchCb:
            toggleRomanizedCb(isChecked);
            break;
        case R.id.commonWordsCb:
            toggleRomanizedCb(isChecked);
            break;
        case R.id.romanizedCb:
            toggleExactCommonCbs(isChecked);
            break;
        default:
            // do nothing
        }
    }

    private void toggleExactCommonCbs(boolean isChecked) {
        if (isChecked) {
            exactMatchCb.setEnabled(false);
            commonWordsCb.setEnabled(false);
        } else {
            exactMatchCb.setEnabled(true);
            commonWordsCb.setEnabled(true);
        }
    }

    private void toggleRomanizedCb(boolean isChecked) {
        if (isChecked) {
            romanizedJapaneseCb.setEnabled(false);
        } else {
            if (!exactMatchCb.isChecked() && !commonWordsCb.isChecked()) {
                romanizedJapaneseCb.setEnabled(true);
            }
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position,
            long id) {
        if (inputTextFromBundle) {
            inputTextFromBundle = false;

            return;
        }

        switch (parent.getId()) {
        case R.id.kanjiSearchTypeSpinner:
            kanjiInputText.setText("");
            kanjiInputText.requestFocus();

            if (position != 2) {
                toggleRadicalStrokeCountPanel(false);
            } else {
                toggleRadicalStrokeCountPanel(true);
            }
            break;
        case R.id.modeSpinner:
            toggleExampleOptions(position == 0);
            break;
        }
    }

    private void toggleRadicalStrokeCountPanel(boolean isEnabled) {
        selectRadicalButton.setEnabled(isEnabled);
        strokeCountMinInput.setEnabled(isEnabled);
        strokeCountMinInput.setFocusableInTouchMode(isEnabled);
        strokeCountMaxInput.setEnabled(isEnabled);
        strokeCountMaxInput.setFocusableInTouchMode(isEnabled);
        if (!isEnabled) {
            strokeCountMinInput.setText("");
            strokeCountMaxInput.setText("");
            radicalEditText.setText("");
            kanjiInputText.requestFocus();
        }
    }

    private void toggleExampleOptions(boolean isEnabled) {
        maxNumExamplesText.setEnabled(isEnabled);
        exampleExactMatchCb.setEnabled(isEnabled);
        maxNumExamplesText.setFocusableInTouchMode(isEnabled);
        exampleExactMatchCb.setFocusableInTouchMode(isEnabled);

        exampleSearchInputText.setText("");
        exampleSearchInputText.requestFocus();

        if (!isEnabled) {
            exampleSearchInputText.setHint(R.string.enter_japanese_text);
            exampleSearchButton.setText(R.string.translate);
        } else {
            exampleSearchInputText.setHint(R.string.enter_eng_or_jap);
            exampleSearchButton.setText(R.string.search);
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }

}
