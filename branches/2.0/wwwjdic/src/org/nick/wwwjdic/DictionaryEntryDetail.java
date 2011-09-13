package org.nick.wwwjdic;

import static org.nick.wwwjdic.Constants.DICTIONARY_TAB_IDX;
import static org.nick.wwwjdic.Constants.SELECTED_TAB_IDX;

import org.nick.wwwjdic.history.FavoritesAndHistory;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.view.MenuInflater;

public class DictionaryEntryDetail extends DetailActivity {

    public static final String EXTRA_DICTIONARY_ENTRY = "org.nick.wwwjdic.DICTIONARY_ENTRY";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DictionaryEntry entry = (DictionaryEntry) getIntent()
                .getSerializableExtra(EXTRA_DICTIONARY_ENTRY);
        wwwjdicEntry = entry;

        if (savedInstanceState == null) {
            DictionaryEntryDetailFragment details = new DictionaryEntryDetailFragment();
            details.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, details).commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void setHomeActivityExtras(Intent homeActivityIntent) {
        homeActivityIntent.putExtra(SELECTED_TAB_IDX, DICTIONARY_TAB_IDX);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dict_detail, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent intent = new Intent(this,
                    isFavorite ? FavoritesAndHistory.class
                            : DictionaryResultListView.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            // setHomeActivityExtras(intent);
            startActivity(intent);
            return true;
        case R.id.menu_dict_detail_lookup_kanji:
            Activities.lookupKanji(this, db, wwwjdicEntry.getHeadword());
            return true;
        default:
            // do nothing
        }

        return super.onOptionsItemSelected(item);
    }

}
