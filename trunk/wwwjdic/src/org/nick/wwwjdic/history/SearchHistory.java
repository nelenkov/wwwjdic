package org.nick.wwwjdic.history;

import org.nick.wwwjdic.R;

import android.database.Cursor;

public class SearchHistory extends HistoryBase {

    protected void setupAdapter() {
        try {
            Cursor cursor = db.getHistory();
            SearchHistoryAdapter adapter = new SearchHistoryAdapter(this,
                    cursor, this);
            setListAdapter(adapter);
        } finally {
            db.close();
        }
    }

    public void refresh() {
        try {
            Cursor cursor = db.getHistory();
            SearchHistoryAdapter adapter = new SearchHistoryAdapter(this,
                    cursor, this);
            setListAdapter(adapter);
        } finally {
            db.close();
        }
    }

    @Override
    protected void deleteAll() {
        db.deleteAllHistory();
    }

    @Override
    protected int getContentView() {
        return R.layout.search_history;
    }

    @Override
    protected void deleteCurrentItem() {
        Cursor c = getCursor();
        int idx = c.getColumnIndex("_id");
        int id = c.getInt(idx);
        db.deleteHistoryItem(id);

        refresh();
    }

}
