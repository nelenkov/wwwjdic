/**
 * 
 */
package org.nick.wwwjdic.history;

import org.nick.wwwjdic.SearchCriteria;
import org.nick.wwwjdic.history.HistoryItem.FavoriteStatusChangedListener;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

class SearchHistoryAdapter extends CursorAdapter {

    private FavoriteStatusChangedListener favoriteStatusChanged;

    public SearchHistoryAdapter(Context context, Cursor c,
            FavoriteStatusChangedListener statusChangedListener) {
        super(context, c);
        this.favoriteStatusChanged = statusChangedListener;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        HistoryItem historyItem = (HistoryItem) view;

        SearchCriteria criteria = HistoryDbHelper.createCriteria(cursor);
        historyItem.populate(criteria);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        HistoryItem result = new HistoryItem(context, favoriteStatusChanged);

        return result;
    }
}
