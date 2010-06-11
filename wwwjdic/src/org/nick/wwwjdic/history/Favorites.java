package org.nick.wwwjdic.history;

import org.nick.wwwjdic.R;

import android.database.Cursor;

public class Favorites extends HistoryBase {

    protected void setupAdapter() {
        try {
            Cursor cursor = db.getFavorites();
            SearchHistoryAdapter adapter = new SearchHistoryAdapter(this,
                    cursor, this);
            setListAdapter(adapter);
        } finally {
            db.close();
        }
    }

    public void refresh() {
        try {
            Cursor cursor = db.getFavorites();
            SearchHistoryAdapter adapter = new SearchHistoryAdapter(this,
                    cursor, this);
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

}
