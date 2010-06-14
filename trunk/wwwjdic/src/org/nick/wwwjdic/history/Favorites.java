package org.nick.wwwjdic.history;

import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.DictionaryEntryDetail;
import org.nick.wwwjdic.KanjiEntryDetail;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicEntry;
import org.nick.wwwjdic.history.FavoritesItem.FavoriteStatusChangedListener;

import android.content.Intent;
import android.database.Cursor;

public class Favorites extends HistoryBase implements
        FavoriteStatusChangedListener {

    protected void setupAdapter() {
        try {
            Cursor cursor = db.getFavorites();
            FavoritesAdapter adapter = new FavoritesAdapter(this, cursor, this);
            setListAdapter(adapter);
        } finally {
            db.close();
        }
    }

    public void refresh() {
        try {
            Cursor cursor = db.getFavorites();
            FavoritesAdapter adapter = new FavoritesAdapter(this, cursor, this);
            setListAdapter(adapter);
        } finally {
            db.close();
        }
    }

    @Override
    protected void deleteAll() {
        db.deleteAllFavorites();
    }

    @Override
    protected int getContentView() {
        return R.layout.favorites;
    }

    @Override
    protected void deleteCurrentItem() {
        Cursor c = getCursor();
        int idx = c.getColumnIndex("_id");
        int id = c.getInt(idx);
        db.deleteFavorite(id);

        refresh();
    }

    @Override
    public void onStatusChanged(boolean isFavorite, WwwjdicEntry entry) {
        if (isFavorite) {
            db.addFavorite(entry);
        } else {
            db.deleteFavorite(entry.getId());
        }
    }

    @Override
    protected void lookupCurrentItem() {
        Cursor c = getCursor();

        WwwjdicEntry entry = HistoryDbHelper.createWwwjdicEntry(c);

        Intent intent = null;
        if (entry.isKanji()) {
            intent = new Intent(this, KanjiEntryDetail.class);
            intent.putExtra(Constants.KANJI_ENTRY_KEY, entry);
            intent.putExtra(Constants.IS_FAVORITE, true);
        } else {
            intent = new Intent(this, DictionaryEntryDetail.class);
            intent.putExtra(Constants.ENTRY_KEY, entry);
            intent.putExtra(Constants.IS_FAVORITE, true);
        }

        startActivity(intent);
    }

}
