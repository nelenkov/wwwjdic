/**
 * 
 */
package org.nick.wwwjdic.history;

import org.nick.wwwjdic.SearchCriteria;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;

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
