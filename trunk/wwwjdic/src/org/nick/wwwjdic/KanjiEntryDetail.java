package org.nick.wwwjdic;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class KanjiEntryDetail extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kanji_entry_details);

        KanjiEntry entry = (KanjiEntry) getIntent().getSerializableExtra(
                "org.nick.hello.kanjiEntry");

        setTitle(String.format("Details for '%s'", entry.getKanji()));

        LinearLayout detailLayout = (LinearLayout) findViewById(R.id.kanjiDetailLayout);

        TextView entryView = (TextView) findViewById(R.id.kanjiText);
        entryView.setText(entry.getKanji());
        entryView.setTextSize(40f);
        entryView.setTextColor(Color.WHITE);

        LinearLayout readingLayout = (LinearLayout) findViewById(R.id.readingLayout);

        if (entry.getReading() != null) {
            TextView onyomiView = new TextView(this);
            onyomiView.setText(entry.getOnyomi());
            onyomiView.setTextSize(18f);
            onyomiView.setTextColor(Color.WHITE);
            readingLayout.addView(onyomiView);

            TextView kunyomiView = new TextView(this);
            kunyomiView.setText(entry.getKunyomi());
            kunyomiView.setTextSize(18f);
            kunyomiView.setTextColor(Color.WHITE);
            readingLayout.addView(kunyomiView);
        }

        for (String meaning : entry.getMeanings()) {
            TextView text = new TextView(this);
            text.setTextSize(18f);
            text.setText(meaning);
            detailLayout.addView(text);
        }
    }
}
