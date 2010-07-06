package org.nick.wwwjdic.history;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.DictionaryResultListView;
import org.nick.wwwjdic.ExamplesResultListView;
import org.nick.wwwjdic.KanjiResultListView;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.SearchCriteria;

import android.content.Intent;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVWriter;

public class SearchHistory extends HistoryBase {

    private static final String TAG = SearchHistory.class.getSimpleName();

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

    @Override
    protected void exportItems() {
        CSVWriter writer = null;

        try {
            Cursor c = db.getHistory();
            File extStorage = Environment.getExternalStorageDirectory();
            writer = new CSVWriter(new FileWriter(extStorage.getAbsolutePath()
                    + "/favorites.csv"));

            while (c.moveToNext()) {
                SearchCriteria criteria = HistoryDbHelper.createCriteria(c);
                String[] criteriaStr = toStringArr(criteria);
                writer.writeNext(criteriaStr);

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Log.w(TAG, "error closing CSV writer", e);
                }

            }
        }
        Toast t = Toast
                .makeText(this, "Favorites exported", Toast.LENGTH_SHORT);
        t.show();
    }

    private String[] toStringArr(SearchCriteria criteria) {
        String[] result = new String[10];
        result[0] = Integer.toString(criteria.getType());
        result[1] = criteria.getQueryString();
        result[2] = toTfInt(criteria.isExactMatch());
        result[3] = toTfInt(criteria.isKanjiLookup());
        result[4] = toTfInt(criteria.isRomanizedJapanese());
        result[5] = toTfInt(criteria.isCommonWordsOnly());
        result[6] = criteria.getDictionary();
        result[7] = criteria.getKanjiSearchType();
        result[8] = toIntStr(criteria.getMinStrokeCount());
        result[9] = toIntStr(criteria.getMaxStrokeCount());

        return result;
    }

    private String toIntStr(Integer i) {
        return i == null ? null : i.toString();
    }

    private String toTfInt(boolean b) {
        return b ? "1" : "0";
    }

    @Override
    protected void importItems() {
        // TODO Auto-generated method stub

    }

}
