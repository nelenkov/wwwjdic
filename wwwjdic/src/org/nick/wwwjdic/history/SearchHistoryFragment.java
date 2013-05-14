package org.nick.wwwjdic.history;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.nick.wwwjdic.DictionaryResultList;
import org.nick.wwwjdic.ExamplesResultList;
import org.nick.wwwjdic.KanjiResultList;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.SearchSuggestionProvider;
import org.nick.wwwjdic.Wwwjdic;
import org.nick.wwwjdic.model.SearchCriteria;
import org.nick.wwwjdic.utils.LoaderResult;
import org.nick.wwwjdic.utils.MediaScannerWrapper;
import org.nick.wwwjdic.utils.UIUtils;

import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.SearchRecentSuggestions;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class SearchHistoryFragment extends HistoryFragmentBase {

    private static final String TAG = SearchHistoryFragment.class
            .getSimpleName();

    private static final String EXPORT_FILENAME = "wwwjdic/search-history.csv";

    protected void setupAdapter() {
        MatrixCursor cursor = new MatrixCursor(
                HistoryDbHelper.HISTORY_ALL_COLUMNS, 0);

        SearchHistoryAdapter adapter = new SearchHistoryAdapter(getActivity(),
                cursor);
        setListAdapter(adapter);

        getSherlockActivity()
                .setSupportProgressBarIndeterminateVisibility(true);
        getLoaderManager().initLoader(1, null, this);
    }

    @Override
    protected void deleteAll() {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected void onPreExecute() {
                getSherlockActivity()
                        .setSupportProgressBarIndeterminateVisibility(true);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                // clear search suggestions
                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
                        getActivity(), SearchSuggestionProvider.AUTHORITY,
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

                    return false;
                } finally {
                    db.endTransaction();
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (isDetached() || getActivity() == null) {
                    return;
                }

                refresh();
                getSherlockActivity()
                        .setSupportProgressBarIndeterminateVisibility(false);
            }
        }.execute();
    }

    @Override
    protected int getContentView() {
        return R.layout.search_history_fragment;
    }

    private void lookup(SearchCriteria criteria) {
        Intent intent = null;
        switch (criteria.getType()) {
        case SearchCriteria.CRITERIA_TYPE_DICT:
            intent = new Intent(getActivity(), DictionaryResultList.class);
            break;
        case SearchCriteria.CRITERIA_TYPE_KANJI:
            intent = new Intent(getActivity(), KanjiResultList.class);
            break;
        case SearchCriteria.CRITERIA_TYPE_EXAMPLES:
            intent = new Intent(getActivity(), ExamplesResultList.class);
            break;
        default:
            // do nothing?
        }

        intent.putExtra(Wwwjdic.EXTRA_CRITERIA, criteria);

        startActivity(intent);
    }

    @Override
    protected void lookup(int position) {
        lookup(getCriteria(position));
    }

    private SearchCriteria getCriteria(int position) {
        Cursor c = (Cursor) getListView().getAdapter().getItem(position);

        return HistoryDbHelper.createCriteria(c);
    }

    @Override
    protected void delete(int position) {
        SearchCriteria criteria = getCriteria(position);
        db.deleteHistoryItem(criteria.getId());

        refresh();
    }

    @SuppressWarnings("deprecation")
    private void copy(SearchCriteria criteria) {
        clipboardManager.setText(criteria.getQueryString());

        showCopiedToast(criteria.getQueryString());
    }

    @Override
    protected void copy(int position) {
        copy(getCriteria(position));
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
                getSherlockActivity()
                        .setSupportProgressBarIndeterminateVisibility(true);
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

                    if (UIUtils.isFroyo()) {
                        MediaScannerWrapper.scanFile(getActivity(), filename);
                    }

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
                if (isDetached() || getActivity() == null) {
                    return;
                }

                getSherlockActivity()
                        .setSupportProgressBarIndeterminateVisibility(false);

                if (result) {
                    String message = getResources().getString(
                            R.string.history_exported);
                    Toast t = Toast.makeText(getActivity(),
                            String.format(message, filename, count),
                            Toast.LENGTH_SHORT);
                    t.show();
                } else {
                    String message = getResources().getString(
                            R.string.export_error);
                    String errMessage = exception == null ? "Error" : exception
                            .getMessage();
                    Toast.makeText(getActivity(),
                            String.format(message, errMessage),
                            Toast.LENGTH_SHORT).show();
                }
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
                getSherlockActivity()
                        .setSupportProgressBarIndeterminateVisibility(true);
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
                if (isDetached() || getActivity() == null) {
                    return;
                }

                getSherlockActivity()
                        .setSupportProgressBarIndeterminateVisibility(false);

                if (result) {
                    String message = getResources().getString(
                            R.string.history_imported);
                    Toast t = Toast.makeText(getActivity(),
                            String.format(message, importFile, count),
                            Toast.LENGTH_SHORT);
                    t.show();
                } else {
                    String message = getResources().getString(
                            R.string.import_error);
                    String errMessage = exception == null ? "Error" : exception
                            .getMessage();
                    Toast.makeText(getActivity(),
                            String.format(message, errMessage),
                            Toast.LENGTH_SHORT).show();
                }

                refresh();
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

    @Override
    public Loader<LoaderResult<Cursor>> onCreateLoader(int id, Bundle args) {
        SearchHistoryLoader loader = new SearchHistoryLoader(getActivity(), db);
        loader.setSelectedFilter(selectedFilter);

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult<Cursor>> loader,
            LoaderResult<Cursor> data) {
        getSherlockActivity().setSupportProgressBarIndeterminateVisibility(
                false);

        CursorAdapter adapter = (CursorAdapter) getListAdapter();
        adapter.swapCursor(data.getData());

        // The list should now be shown.
        // if (isResumed()) {
        // setListShown(true);
        // } else {
        // setListShownNoAnimation(true);
        // }
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult<Cursor>> loader) {
        getSherlockActivity().setSupportProgressBarIndeterminateVisibility(
                false);

        CursorAdapter adapter = (CursorAdapter) getListAdapter();
        adapter.swapCursor(null);
    }

}
