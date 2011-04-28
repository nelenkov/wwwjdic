package org.nick.wwwjdic;

import java.util.List;

import org.nick.wwwjdic.history.FavoritesAndHistorySummaryView;
import org.nick.wwwjdic.history.HistoryDbHelper;
import org.nick.wwwjdic.utils.Analytics;
import org.nick.wwwjdic.utils.StringUtils;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

public class ExampleSearch extends WwwjdicActivityBase implements
        OnClickListener, OnItemSelectedListener {

    private EditText exampleSearchInputText;
    private EditText maxNumExamplesText;
    private CheckBox exampleExactMatchCb;
    private Spinner sentenceModeSpinner;
    private Button exampleSearchButton;

    private FavoritesAndHistorySummaryView examplesHistorySummary;

    private HistoryDbHelper dbHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.example_search);

        findViews();
        setupListeners();
        setupSpinners();

        exampleSearchInputText.requestFocus();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String searchKey = extras.getString(Constants.SEARCH_TEXT_KEY);
            int searchType = extras.getInt(Constants.SEARCH_TYPE);
            if (searchKey != null) {
                switch (searchType) {
                case SearchCriteria.CRITERIA_TYPE_EXAMPLES:
                    exampleSearchInputText.setText(searchKey);
                    break;
                default:
                    // do nothing
                }
                inputTextFromBundle = true;
            }
        }

        dbHelper = HistoryDbHelper.getInstance(this);

        setupExamplesSummary();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        setupExamplesSummary();
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

    private void setupListeners() {
        View exampleSearchButton = findViewById(R.id.exampleSearchButton);
        exampleSearchButton.setOnClickListener(this);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> sentenceModeAdapter = ArrayAdapter
                .createFromResource(this, R.array.sentence_modes,
                        R.layout.spinner_text);
        sentenceModeAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sentenceModeSpinner.setAdapter(sentenceModeAdapter);
        sentenceModeSpinner.setOnItemSelectedListener(this);
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.exampleSearchButton:
            // hideKeyboard();

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

                Analytics.event("sentenceTranslation", this);

                startActivity(intent);
            }
            break;
        default:
            // do nothing
        }
    }

    private void findViews() {
        exampleSearchInputText = (EditText) findViewById(R.id.exampleInputText);
        maxNumExamplesText = (EditText) findViewById(R.id.maxExamplesInput);
        exampleExactMatchCb = (CheckBox) findViewById(R.id.exampleExactMatchCb);
        sentenceModeSpinner = (Spinner) findViewById(R.id.modeSpinner);
        exampleSearchButton = (Button) findViewById(R.id.exampleSearchButton);

        examplesHistorySummary = (FavoritesAndHistorySummaryView) findViewById(R.id.examples_history_summary);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position,
            long id) {
        if (inputTextFromBundle) {
            inputTextFromBundle = false;

            return;
        }

        switch (parent.getId()) {
        case R.id.modeSpinner:
            toggleExampleOptions(position == 0);
            break;
        }
    }

    private void toggleExampleOptions(boolean isEnabled) {
        maxNumExamplesText.setEnabled(isEnabled);
        exampleExactMatchCb.setEnabled(isEnabled);
        maxNumExamplesText.setFocusableInTouchMode(isEnabled);

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
