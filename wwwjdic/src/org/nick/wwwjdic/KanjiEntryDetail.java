package org.nick.wwwjdic;

import static org.nick.wwwjdic.Constants.KANJI_TAB_IDX;
import static org.nick.wwwjdic.Constants.SELECTED_TAB_IDX;

import org.nick.wwwjdic.history.FavoritesAndHistory;
import org.nick.wwwjdic.model.KanjiEntry;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.view.MenuInflater;

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

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.kanji_detail, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent intent = new Intent(this,
                    isFavorite ? FavoritesAndHistory.class
                            : KanjiResultListView.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        case R.id.menu_kanji_detail_copy:
            copy();
            return true;
        default:
            // do nothing
        }

        return super.onOptionsItemSelected(item);
    }

    protected void setHomeActivityExtras(Intent homeActivityIntent) {
        homeActivityIntent.putExtra(SELECTED_TAB_IDX, KANJI_TAB_IDX);
    }

}
