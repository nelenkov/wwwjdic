/**
 * 
 */
package org.nick.wwwjdic.history;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import org.nick.wwwjdic.model.SearchCriteria;

class SearchHistoryAdapter extends CursorAdapter {

    public SearchHistoryAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        HistoryItem historyItem = (HistoryItem) view;

        SearchCriteria criteria = HistoryDbHelper.createCriteria(cursor);
        historyItem.populate(criteria);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        HistoryItem result = new HistoryItem(context);

        return result;
    }
}
