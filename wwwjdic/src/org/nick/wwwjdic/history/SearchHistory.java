package org.nick.wwwjdic.history;

import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.DictionaryResultListView;
import org.nick.wwwjdic.ExamplesResultListView;
import org.nick.wwwjdic.KanjiResultListView;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.SearchCriteria;

import android.content.Intent;
import android.database.Cursor;

public class SearchHistory extends HistoryBase {

    protected void setupAdapter() {
        Cursor cursor = db.getHistory();
        startManagingCursor(cursor);
        SearchHistoryAdapter adapter = new SearchHistoryAdapter(this, cursor);
        setListAdapter(adapter);
    }

    @Override
    protected void deleteAll() {
        db.deleteAllHistory();
        refresh();
    }

    @Override
    protected int getContentView() {
        return R.layout.search_history;
    }

    @Override
    protected void lookupCurrentItem() {
        SearchCriteria criteria = getCurrentCriteria();

        Intent intent = null;
        switch (criteria.getType()) {
        case SearchCriteria.CRITERIA_TYPE_DICT:
            intent = new Intent(this, DictionaryResultListView.class);
            break;
        case SearchCriteria.CRITERIA_TYPE_KANJI:
            intent = new Intent(this, KanjiResultListView.class);
            break;
        case SearchCriteria.CRITERIA_TYPE_EXAMPLES:
            intent = new Intent(this, ExamplesResultListView.class);
            break;
        default:
            // do nothing?
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

    @Override
    protected void copyCurrentItem() {
        SearchCriteria criteria = getCurrentCriteria();
        clipboardManager.setText(criteria.getQueryString());
    }

    private SearchCriteria getCurrentCriteria() {
        Cursor c = getCursor();
        SearchCriteria criteria = HistoryDbHelper.createCriteria(c);
        return criteria;
    }

}
