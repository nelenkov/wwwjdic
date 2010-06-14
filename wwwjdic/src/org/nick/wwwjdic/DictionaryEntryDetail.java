package org.nick.wwwjdic;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DictionaryEntryDetail extends DetailActivity {

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

        TextView entryView = (TextView) findViewById(R.id.wordText);
        entryView.setText(entry.getWord());

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

        CheckBox starCb = (CheckBox) findViewById(R.id.star_word);
        starCb.setOnCheckedChangeListener(null);
        starCb.setChecked(isFavorite);
        starCb.setOnCheckedChangeListener(this);
    }
}
