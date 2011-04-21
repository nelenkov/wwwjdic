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
import org.nick.wwwjdic.SearchSuggestionProvider;
import org.nick.wwwjdic.utils.Analytics;

import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.SearchRecentSuggestions;
import android.util.Log;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class SearchHistory extends HistoryBase {

    private static final String TAG = SearchHistory.class.getSimpleName();

    private static final String EXPORT_FILENAME = "wwwjdic/search-history.csv";

    protected void setupAdapter() {
        MatrixCursor cursor = new MatrixCursor(
                HistoryDbHelper.HISTORY_ALL_COLUMNS, 0);
        startManagingCursor(cursor);
        SearchHistoryAdapter adapter = new SearchHistoryAdapter(this, cursor);
        setListAdapter(adapter);

        new AsyncTask<Void, Void, Cursor>() {
            @Override
            protected void onPreExecute() {
                getParent().setProgressBarIndeterminateVisibility(true);
            }

            @Override
            protected Cursor doInBackground(Void... arg0) {
                return filterCursor();
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                resetAdapter(cursor);
                getParent().setProgressBarIndeterminateVisibility(false);
            }
        }.execute();
    }

    protected void resetAdapter(Cursor c) {
        startManagingCursor(c);
        SearchHistoryAdapter adapter = new SearchHistoryAdapter(
                SearchHistory.this, c);
        setListAdapter(adapter);
    }

    @Override
    protected void deleteAll() {
        new AsyncTask<Void, Void, Boolean>() {
            Exception exception;

            @Override
            protected void onPreExecute() {
                getParent().setProgressBarIndeterminateVisibility(true);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                // clear search suggestions
                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
                        SearchHistory.this, SearchSuggestionProvider.AUTHORITY,
                        SearchSuggestionProvider.MODE);
                suggestions.clearHistory();

                Cursor c = filterCursor();

                db.beginTransaction();
                try {
                    while (c.moveToNext()) {
                        int id = c.getInt(c.getColumnIndex("_id"));
                        db.deleteHistoryItem(id);
                    }
                    db.setTransactionSuccessful();

                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Error deleting history", e);
                    exception = e;

                    return false;
                } finally {
                    db.endTransaction();
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                refresh();
                getParent().setProgressBarIndeterminateVisibility(false);
            }
        }.execute();
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

        Analytics.event("lookupFromHistory", this);

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
    protected String getImportExportFilename() {
        File extStorage = Environment.getExternalStorageDirectory();

        return extStorage.getAbsolutePath() + "/" + EXPORT_FILENAME;
    }

    @Override
    protected void doExport(final String filename) {
        new AsyncTask<Void, Void, Boolean>() {
            Exception exception;
            int count = 0;

            @Override
            protected void onPreExecute() {
                getParent().setProgressBarIndeterminateVisibility(true);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                CSVWriter writer = null;
                Cursor c = null;
                try {
                    c = filterCursor();

                    writer = new CSVWriter(new FileWriter(filename));

                    while (c.moveToNext()) {
                        long time = c.getLong(c.getColumnIndex("time"));
                        SearchCriteria criteria = HistoryDbHelper
                                .createCriteria(c);
                        String[] criteriaStr = SearchCriteriaParser
                                .toStringArray(criteria, time);
                        writer.writeNext(criteriaStr);
                        count++;
                    }

                    Analytics.event("historyExport", SearchHistory.this);

                    return true;

                } catch (IOException e) {
                    Log.e(TAG, "error exporting history", e);
                    exception = e;

                    return false;
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            Log.w(TAG, "error closing CSV writer", e);
                        }
                    }
                    if (c != null) {
                        c.close();
                    }
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    String message = getResources().getString(
                            R.string.history_exported);
                    Toast t = Toast.makeText(SearchHistory.this,
                            String.format(message, filename, count),
                            Toast.LENGTH_SHORT);
                    t.show();
                } else {
                    String message = getResources().getString(
                            R.string.export_error);
                    String errMessage = exception == null ? "Error" : exception
                            .getMessage();
                    Toast.makeText(SearchHistory.this,
                            String.format(message, errMessage),
                            Toast.LENGTH_SHORT).show();
                }
                getParent().setProgressBarIndeterminateVisibility(false);
            }
        }.execute();
    }

    @Override
    protected void doImport(final String importFile) {
        new AsyncTask<Void, Void, Boolean>() {
            Exception exception;
            int count = 0;

            @Override
            protected void onPreExecute() {
                getParent().setProgressBarIndeterminateVisibility(true);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                CSVReader reader = null;

                db.beginTransaction();
                try {
                    db.deleteAllHistory();

                    reader = openImportFile(importFile);
                    if (reader == null) {
                        return false;
                    }

                    String[] record = null;
                    while ((record = reader.readNext()) != null) {
                        SearchCriteria criteria = SearchCriteriaParser
                                .fromStringArray(record);
                        long time = Long
                                .parseLong(record[SearchCriteriaParser.TIME_IDX]);
                        db.addSearchCriteria(criteria, time);
                        count++;
                    }
                    db.setTransactionSuccessful();

                    Analytics.event("historyImport", SearchHistory.this);

                    return true;
                } catch (IOException e) {
                    Log.e(TAG, "error importing history", e);
                    exception = e;

                    return false;
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            Log.w(TAG, "error closing CSV reader", e);
                        }

                    }
                    db.endTransaction();
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    String message = getResources().getString(
                            R.string.history_imported);
                    Toast t = Toast.makeText(SearchHistory.this,
                            String.format(message, importFile, count),
                            Toast.LENGTH_SHORT);
                    t.show();
                } else {
                    String message = getResources().getString(
                            R.string.import_error);
                    String errMessage = exception == null ? "Error" : exception
                            .getMessage();
                    Toast.makeText(SearchHistory.this,
                            String.format(message, errMessage),
                            Toast.LENGTH_SHORT).show();
                }

                refresh();

                getParent().setProgressBarIndeterminateVisibility(false);
            }
        }.execute();
    }

    @Override
    protected Cursor filterCursor() {
        if (selectedFilter == FILTER_ALL) {
            return db.getHistory();

        }

        return db.getHistoryByType(selectedFilter);
    }

    @Override
    protected String[] getFilterTypes() {
        return getResources().getStringArray(R.array.filter_types_history);
    }

}
