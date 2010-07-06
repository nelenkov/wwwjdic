package org.nick.wwwjdic.history;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.DictionaryResultListView;
import org.nick.wwwjdic.ExamplesResultListView;
import org.nick.wwwjdic.KanjiResultListView;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.SearchCriteria;
import org.nick.wwwjdic.StringUtils;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;
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
                    + "/search-history.csv"));

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
        Toast t = Toast.makeText(this, "Search history exported",
                Toast.LENGTH_SHORT);
        t.show();
    }

    private String[] toStringArr(SearchCriteria criteria) {
        String[] result = new String[11];
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
        result[10] = criteria.getNumMaxResults() == null ? null : Integer
                .toString(criteria.getNumMaxResults());

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
        CSVReader reader = null;

        SQLiteDatabase s = db.getWritableDatabase();
        s.beginTransaction();
        try {
            File extStorage = Environment.getExternalStorageDirectory();
            reader = new CSVReader(new FileReader(extStorage.getAbsolutePath()
                    + "/search-history.csv"));

            String[] record = null;
            while ((record = reader.readNext()) != null) {
                SearchCriteria criteria = createCriteria(record);
                db.addSearchCriteria(criteria);

            }
            s.setTransactionSuccessful();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.w(TAG, "error closing CSV reader", e);
                }

            }
            s.endTransaction();
        }

        refresh();
        Toast t = Toast.makeText(this, "Search history imported",
                Toast.LENGTH_SHORT);
        t.show();

    }

    private SearchCriteria createCriteria(String[] record) {
        SearchCriteria result = null;

        int type = Integer.parseInt(record[0]);
        switch (type) {
        case SearchCriteria.CRITERIA_TYPE_DICT:
            result = SearchCriteria.createForDictionary(record[1],
                    parseTfStr(record[2]), parseTfStr(record[4]),
                    parseTfStr(record[5]), record[6]);
            break;
        case SearchCriteria.CRITERIA_TYPE_KANJI:
            Integer minStrokes = StringUtils.isEmpty(record[8]) ? null
                    : Integer.parseInt(record[8]);
            Integer maxStrokes = StringUtils.isEmpty(record[9]) ? null
                    : Integer.parseInt(record[9]);
            result = SearchCriteria.createWithStrokeCount(record[1], record[7],
                    minStrokes, maxStrokes);
            break;
        case SearchCriteria.CRITERIA_TYPE_EXAMPLES:
            result = SearchCriteria.createForExampleSearch(record[1],
                    parseTfStr(record[2]), Integer.parseInt(record[10]));
            break;
        default:
            throw new IllegalArgumentException("Unknown criteria type: " + type);
        }

        return result;
    }

    private boolean parseTfStr(String str) {
        if ("1".equals(str)) {
            return true;
        }

        return false;
    }

}
