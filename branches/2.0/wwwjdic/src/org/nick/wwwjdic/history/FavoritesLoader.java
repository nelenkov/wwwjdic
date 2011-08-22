package org.nick.wwwjdic.history;

import android.content.Context;
import android.database.Cursor;

public class FavoritesLoader extends HistoryLoaderBase {

    public FavoritesLoader(Context context, HistoryDbHelper db) {
        super(context, db);
    }

    @Override
    public Cursor load() {
        if (selectedFilter == FILTER_ALL) {
            lastCursor = db.getFavorites();

        } else {
            lastCursor = db.getFavoritesByType(selectedFilter);
        }

        return lastCursor;
    }

}
