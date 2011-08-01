/**
 * 
 */
package org.nick.wwwjdic.history;

import org.nick.wwwjdic.WwwjdicEntry;
import org.nick.wwwjdic.history.FavoritesItem.FavoriteStatusChangedListener;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;

class FavoritesAdapter extends CursorAdapter {

    private FavoriteStatusChangedListener favoriteStatusChanged;

    public FavoritesAdapter(Context context, Cursor c,
            FavoriteStatusChangedListener statusChangedListener) {
        super(context, c, 0);
        this.favoriteStatusChanged = statusChangedListener;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        FavoritesItem favorite = (FavoritesItem) view;

        WwwjdicEntry entry = HistoryDbHelper.createWwwjdicEntry(cursor);
        favorite.populate(entry);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        FavoritesItem result = new FavoritesItem(context, favoriteStatusChanged);

        return result;
    }
}
