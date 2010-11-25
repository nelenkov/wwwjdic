package org.nick.wwwjdic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.nick.wwwjdic.history.FavoritesAndHistorySummaryView;
import org.nick.wwwjdic.history.HistoryDbHelper;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class Dictionary extends WwwjdicActivityBase implements OnClickListener,
        OnFocusChangeListener, OnCheckedChangeListener {

    private static final String TAG = Dictionary.class.getSimpleName();

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

    private EditText inputText;
    private CheckBox exactMatchCb;
    private CheckBox commonWordsCb;
    private CheckBox romanizedJapaneseCb;
    private Spinner dictSpinner;

    private FavoritesAndHistorySummaryView dictHistorySummary;

    private HistoryDbHelper dbHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dict_lookup);

        findViews();
        setupListeners();
        setupSpinners();

        inputText.requestFocus();
        selectDictionary();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String searchKey = extras.getString(Constants.SEARCH_TEXT_KEY);
            int searchType = extras.getInt(Constants.SEARCH_TYPE);
            if (searchKey != null) {
                switch (searchType) {
                case SearchCriteria.CRITERIA_TYPE_DICT:
                    inputText.setText(searchKey);
                    break;
                default:
                    // do nothing
                }
                inputTextFromBundle = true;
            }
        }

        dbHelper = new HistoryDbHelper(this);

        setupDictSummary();
    }

    private void selectDictionary() {
        dictSpinner.setSelection(WwwjdicPreferences
                .getDefaultDictionaryIdx(this));
    }

    @Override
    protected void onResume() {
        super.onResume();

        setupDictSummary();

        selectDictionary();
    }

    private void findViews() {
        inputText = (EditText) findViewById(R.id.inputText);
        exactMatchCb = (CheckBox) findViewById(R.id.exactMatchCb);
        commonWordsCb = (CheckBox) findViewById(R.id.commonWordsCb);
        romanizedJapaneseCb = (CheckBox) findViewById(R.id.romanizedCb);
        dictSpinner = (Spinner) findViewById(R.id.dictionarySpinner);

        dictHistorySummary = (FavoritesAndHistorySummaryView) findViewById(R.id.dict_history_summary);
    }

    private void setupListeners() {
        View translateButton = findViewById(R.id.translateButton);
        translateButton.setOnClickListener(this);

        // inputText.setOnFocusChangeListener(this);

        romanizedJapaneseCb.setOnCheckedChangeListener(this);
        exactMatchCb.setOnCheckedChangeListener(this);
        commonWordsCb.setOnCheckedChangeListener(this);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.dictionaries_array, R.layout.spinner_text);
        adapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dictSpinner.setAdapter(adapter);
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
        default:
            // do nothing
        }
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

    private void hideKeyboard() {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(inputText.getWindowToken(), 0);
    }

    private void showKeyboard() {
        EditText editText = (EditText) findViewById(R.id.inputText);
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

}
