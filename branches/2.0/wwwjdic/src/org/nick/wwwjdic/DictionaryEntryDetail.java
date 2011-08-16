package org.nick.wwwjdic;

import static org.nick.wwwjdic.Constants.DICTIONARY_TAB_IDX;
import static org.nick.wwwjdic.Constants.SELECTED_TAB_IDX;

import org.nick.wwwjdic.history.FavoritesAndHistory;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;

public class DictionaryEntryDetail extends DetailActivity {

    public static final String EXTRA_DICTIONARY_ENTRY = "org.nick.wwwjdic.DICTIONARY_ENTRY";

    private static final int ITEM_ID_LOOKUP_KANJI = 1;

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

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void setHomeActivityExtras(Intent homeActivityIntent) {
        homeActivityIntent.putExtra(SELECTED_TAB_IDX, DICTIONARY_TAB_IDX);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ITEM_ID_LOOKUP_KANJI, 0, R.string.lookup_kanji).setIcon(
                android.R.drawable.ic_menu_search);

        return true;
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
            startActivity(intent);
            return true;
        default:
            // do nothing
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
        case ITEM_ID_HOME:
            Intent intent = new Intent(this, Wwwjdic.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            setHomeActivityExtras(intent);

            startActivity(intent);
            finish();

            return true;
        case ITEM_ID_LOOKUP_KANJI:
            Activities.lookupKanji(this, db, wwwjdicEntry.getHeadword());
            return true;
        default:
            // do nothing
        }

        return super.onMenuItemSelected(featureId, item);
    }

}
