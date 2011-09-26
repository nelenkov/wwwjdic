package org.nick.wwwjdic;

import org.nick.wwwjdic.model.KanjiEntry;

import android.os.Bundle;

public class KanjiEntryDetail extends DetailActivity {

    public static final String EXTRA_KANJI_ENTRY = "org.nick.wwwjdic.KANJI_ENTRY";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        KanjiEntry entry = (KanjiEntry) getIntent().getSerializableExtra(
                EXTRA_KANJI_ENTRY);
        wwwjdicEntry = entry;

        if (savedInstanceState == null) {
            KanjiEntryDetailFragment details = new KanjiEntryDetailFragment();
            details.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, details).commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
