package org.nick.wwwjdic;

import org.nick.wwwjdic.model.DictionaryEntry;

import android.os.Bundle;

public class DictionaryEntryDetail extends DetailActivity {

    public static final String EXTRA_DICTIONARY_ENTRY = "org.nick.wwwjdic.DICTIONARY_ENTRY";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DictionaryEntry entry = (DictionaryEntry) getIntent()
                .getSerializableExtra(EXTRA_DICTIONARY_ENTRY);
        wwwjdicEntry = entry;

        setContentView(R.layout.dict_entry_details);

        if (savedInstanceState == null) {
            DictionaryEntryDetailFragment details = new DictionaryEntryDetailFragment();
            details.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.entry_details, details).commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

}
