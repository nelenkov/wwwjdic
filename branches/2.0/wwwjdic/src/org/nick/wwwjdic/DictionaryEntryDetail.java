package org.nick.wwwjdic;

import static org.nick.wwwjdic.Constants.DICTIONARY_TAB_IDX;
import static org.nick.wwwjdic.Constants.SELECTED_TAB_IDX;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;

public class DictionaryEntryDetail extends DetailActivity {

    private static final int ITEM_ID_LOOKUP_KANJI = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DictionaryEntry entry = (DictionaryEntry) getIntent()
                .getSerializableExtra(Constants.ENTRY_KEY);
        wwwjdicEntry = entry;

        if (savedInstanceState == null) {
            DictionaryEntryDetailFragment details = new DictionaryEntryDetailFragment();
            details.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, details).commit();
        }
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
