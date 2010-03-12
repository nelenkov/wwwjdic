package org.nick.wwwjdic;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DictionaryEntryDetail extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry_details);

        DictionaryEntry entry = (DictionaryEntry) getIntent()
                .getSerializableExtra(Constants.ENTRY_KEY);

        setTitle(String.format("Details for '%s'", entry.getWord()));

        LinearLayout detailLayout = (LinearLayout) findViewById(R.id.detailLayout);

        TextView entryView = (TextView) findViewById(R.id.wordText);
        entryView.setText(entry.getWord());

        if (entry.getReading() != null) {
            TextView readingView = new TextView(this, null,
                    R.style.dict_detail_reading);
            readingView.setText(entry.getReading());
            detailLayout.addView(readingView);
        }

        TextView translationLabel = new TextView(this);
        translationLabel.setText(R.string.translation);
        translationLabel.setBackgroundColor(Color.GRAY);
        translationLabel.setTextColor(Color.WHITE);
        detailLayout.addView(translationLabel);

        for (String meaning : entry.getMeanings()) {
            TextView text = new TextView(this, null,
                    R.style.dict_detail_meaning);
            text.setText(meaning);
            detailLayout.addView(text);
        }
    }
}
