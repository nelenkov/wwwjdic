package org.nick.wwwjdic;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DictionaryEntryDetail extends DetailActivity implements
        OnClickListener {

    private static final String COMMON_USAGE_MARKER = "(P)";

    private static final String VARIATION_DELIMITER = ";";

    private static final int DEFAULT_MAX_NUM_EXAMPLES = 20;

    private TextView entryView;
    private CheckBox starCb;
    private Button exampleSearchButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry_details);

        DictionaryEntry entry = (DictionaryEntry) getIntent()
                .getSerializableExtra(Constants.ENTRY_KEY);
        wwwjdicEntry = entry;
        isFavorite = getIntent().getBooleanExtra(Constants.IS_FAVORITE, false);

        setTitle(String.format("Details for '%s'", entry.getWord()));

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
            meaningsLayout.addView(text);
        }

        starCb = (CheckBox) findViewById(R.id.star_word);
        starCb.setOnCheckedChangeListener(null);
        starCb.setChecked(isFavorite);
        starCb.setOnCheckedChangeListener(this);

        exampleSearchButton = (Button) findViewById(R.id.examples_button);
        exampleSearchButton.setOnClickListener(this);

        disableExampleSearchIfSingleKanji();
    }

    private void disableExampleSearchIfSingleKanji() {
        if (wwwjdicEntry.isSingleKanji()) {
            exampleSearchButton.setEnabled(false);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.examples_button:
            Intent intent = new Intent(this, ExamplesResultListView.class);
            String searchKey = extractSearchKey();
            SearchCriteria criteria = SearchCriteria.createForExampleSearch(
                    searchKey, false, DEFAULT_MAX_NUM_EXAMPLES);
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
