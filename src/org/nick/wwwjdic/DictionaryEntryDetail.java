package org.nick.wwwjdic;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

public class DictionaryEntryDetail extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry_details);

        DictionaryEntry entry = (DictionaryEntry) getIntent()
                .getSerializableExtra("org.nick.hello.entry");

        TextView entryView = (TextView) findViewById(R.id.wordText);
        entryView.setText(entry.getWord());
        entryView.setTextSize(24f);
        entryView.setTextColor(Color.WHITE);

        TextView translationView = (TextView) findViewById(R.id.translationText);
        translationView.setText(entry.getTranslationString());
        translationView.setTextSize(16f);
        translationView.setTextColor(Color.WHITE);
    }
}
