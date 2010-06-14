package org.nick.wwwjdic.history;

import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.DictionaryResultListView;
import org.nick.wwwjdic.KanjiResultListView;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.SearchCriteria;

import android.content.Intent;
import android.database.Cursor;

public class SearchHistory extends HistoryBase {

    protected void setupAdapter() {
        try {
            Cursor cursor = db.getHistory();
            SearchHistoryAdapter adapter = new SearchHistoryAdapter(this,
                    cursor);
            setListAdapter(adapter);
        } finally {
            db.close();
        }
    }

    public void refresh() {
        try {
            Cursor cursor = db.getHistory();
            SearchHistoryAdapter adapter = new SearchHistoryAdapter(this,
                    cursor);
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
    protected void lookupCurrentItem() {
        Cursor c = getCursor();

        SearchCriteria criteria = HistoryDbHelper.createCriteria(c);

        Intent intent = null;
        if (criteria.isKanjiLookup()) {
            intent = new Intent(this, KanjiResultListView.class);
        } else {
            intent = new Intent(this, DictionaryResultListView.class);
        }
        intent.putExtra(Constants.CRITERIA_KEY, criteria);

        startActivity(intent);
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
