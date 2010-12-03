package org.nick.wwwjdic;

import java.util.regex.Matcher;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DictionaryEntryDetail extends DetailActivity implements
        OnClickListener {

    private static final String TAG = DictionaryEntryDetail.class
            .getSimpleName();

    private static final String COMMON_USAGE_MARKER = "(P)";

    private static final String VARIATION_DELIMITER = ";";

    private static final int DEFAULT_MAX_NUM_EXAMPLES = 20;

    private TextView entryView;
    private CheckBox starCb;
    private Button exampleSearchButton;

    private String exampleSearchKey;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry_details);

        DictionaryEntry entry = (DictionaryEntry) getIntent()
                .getSerializableExtra(Constants.ENTRY_KEY);
        wwwjdicEntry = entry;
        isFavorite = getIntent().getBooleanExtra(Constants.IS_FAVORITE, false);

        String message = getResources().getString(R.string.details_for);
        setTitle(String.format(message, entry.getWord()));

        LinearLayout wordReadingLayout = (LinearLayout) findViewById(R.id.word_reading_layout);

        entryView = (TextView) findViewById(R.id.wordText);
        entryView.setText(entry.getWord());
        entryView.setOnLongClickListener(this);

        if (entry.getReading() != null) {
            TextView readingView = new TextView(this, null,
                    R.style.dict_detail_reading);
            readingView.setText(entry.getReading());
            wordReadingLayout.addView(readingView);
        }

        LinearLayout meaningsLayout = (LinearLayout) findViewById(R.id.meanings_layout);

        for (String meaning : entry.getMeanings()) {
            TextView text = new TextView(this, null,
                    R.style.dict_detail_meaning);
            text.setText(meaning);
            Matcher m = CROSS_REF_PATTERN.matcher(meaning);
            if (m.matches()) {
                Intent intent = createCrossRefIntent(m.group(1));
                int start = m.start(1);
                int end = m.end(1);
                makeClickable(text, start, end, intent);
            }
            meaningsLayout.addView(text);
        }

        starCb = (CheckBox) findViewById(R.id.star_word);
        starCb.setOnCheckedChangeListener(null);
        starCb.setChecked(isFavorite);
        starCb.setOnCheckedChangeListener(this);

        exampleSearchButton = (Button) findViewById(R.id.examples_button);
        exampleSearchButton.setOnClickListener(this);

        exampleSearchKey = extractSearchKey();
        disableExampleSearchIfSingleKanji();
    }

    private Intent createCrossRefIntent(String word) {
        String dictionary = getApp().getCurrentDictionary();
        Log.d(TAG, String.format(
                "Will look for compounds in dictionary: %s(%s)", getApp()
                        .getCurrentDictionaryName(), dictionary));
        SearchCriteria criteria = SearchCriteria.createForDictionary(word,
                true, false, false, dictionary);
        Intent intent = new Intent(DictionaryEntryDetail.this,
                DictionaryResultListView.class);
        intent.putExtra(Constants.CRITERIA_KEY, criteria);
        return intent;
    }

    private void disableExampleSearchIfSingleKanji() {
        if (exampleSearchKey.length() == 1) {
            exampleSearchButton.setEnabled(false);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.examples_button:
            Intent intent = new Intent(this, ExamplesResultListView.class);
            SearchCriteria criteria = SearchCriteria.createForExampleSearch(
                    exampleSearchKey, false, DEFAULT_MAX_NUM_EXAMPLES);
            intent.putExtra(Constants.CRITERIA_KEY, criteria);

            startActivity(intent);
            break;
        default:
            // do nothing
        }
    }

    private String extractSearchKey() {
        String searchKey = wwwjdicEntry.getHeadword();
        if (searchKey.indexOf(VARIATION_DELIMITER) != -1) {
            String[] variations = searchKey.split(VARIATION_DELIMITER);
            searchKey = variations[0];
            searchKey = searchKey.replace(COMMON_USAGE_MARKER, "");
        }
        return searchKey;
    }

}
