
package org.nick.wwwjdic.history;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;

import org.nick.wwwjdic.DictionaryEntryDetail;
import org.nick.wwwjdic.KanjiEntryDetail;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicApplication;
import org.nick.wwwjdic.WwwjdicPreferences;
import org.nick.wwwjdic.history.FavoritesItem.FavoriteStatusChangedListener;
import org.nick.wwwjdic.model.WwwjdicEntry;
import org.nick.wwwjdic.utils.LoaderResult;
import org.nick.wwwjdic.utils.MediaScannerWrapper;
import org.nick.wwwjdic.utils.UIUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.content.Loader;
import android.widget.CursorAdapter;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class FavoritesFragment extends HistoryFragmentBase implements
        FavoriteStatusChangedListener {

    private static final String TAG = FavoritesFragment.class.getSimpleName();

    private static final String EXPORT_FILENAME = "wwwjdic/favorites.csv";

    private static final String FAVORITES_EXPORT_FILENAME_BASE = "wwwjdic-favorites";
    private static final String KANJI_CSV_EXPORT_FILENAME_BASE = FAVORITES_EXPORT_FILENAME_BASE
            + "-kanji";
    private static final String DICT_CSV_EXPORT_FILENAME_BASE = FAVORITES_EXPORT_FILENAME_BASE
            + "-dict";
    private static final String CSV_EXPORT_FILENAME_EXT = "csv";
    private static final String ANKI_EXPORT_FILENAME_EXT = "apkg";

    private static final int EXPORT_LOCAL_BACKUP_IDX = 0;
    private static final int EXPORT_LOCAL_EXPORT_IDX = 1;
    private static final int EXPORT_ANKI_IDX = 2;

    public FavoritesFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    protected void setupAdapter() {
        MatrixCursor cursor = new MatrixCursor(
                HistoryDbHelper.FAVORITES_ALL_COLUMNS, 0);
        FavoritesAdapter adapter = new FavoritesAdapter(getActivity(), cursor,
                this);
        setListAdapter(adapter);

        getActivity().setProgressBarIndeterminateVisibility(true);
        // LoaderManager.enableDebugLogging(true);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void deleteAll() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                getActivity().setProgressBarIndeterminateVisibility(true);
            }

            @Override
            protected Void doInBackground(Void... arg0) {
                Cursor c = filterCursor();

                db.beginTransaction();
                try {
                    while (c.moveToNext()) {
                        int id = c.getInt(c.getColumnIndex("_id"));
                        db.deleteFavorite(id);
                    }
                    db.setTransactionSuccessful();

                    return null;
                } finally {
                    db.endTransaction();
                }
            }

            @Override
            protected void onPostExecute(Void v) {
                if (isDetached() || getActivity() == null) {
                    return;
                }

                refresh();
                getActivity().setProgressBarIndeterminateVisibility(false);
            }
        }.execute();
    }

    @Override
    protected int getContentView() {
        return R.layout.favorites_fragment;
    }

    @Override
    protected void delete(int position) {
        delete(getEntry(position));
    }

    private void delete(WwwjdicEntry entry) {
        db.deleteFavorite(entry.getId());
        refresh();

    }

    @Override
    public void onStatusChanged(boolean isFavorite, WwwjdicEntry entry) {
        if (isFavorite) {
            db.addFavorite(entry);
            refresh();
        } else {
            db.deleteFavorite(entry.getId());
        }
    }

    @Override
    protected void lookup(int position) {
        WwwjdicEntry entry = getEntry(position);

        lookup(entry);
    }

    private void lookup(WwwjdicEntry entry) {
        Intent intent = null;
        if (entry.isKanji()) {
            intent = new Intent(getActivity(), KanjiEntryDetail.class);
            intent.putExtra(KanjiEntryDetail.EXTRA_KANJI_ENTRY, entry);
            intent.putExtra(KanjiEntryDetail.EXTRA_IS_FAVORITE, true);
        } else {
            intent = new Intent(getActivity(), DictionaryEntryDetail.class);
            intent.putExtra(DictionaryEntryDetail.EXTRA_DICTIONARY_ENTRY, entry);
            intent.putExtra(DictionaryEntryDetail.EXTRA_IS_FAVORITE, true);
        }

        startActivity(intent);
    }

    private WwwjdicEntry getEntry(int position) {
        Cursor c = (Cursor) getListView().getAdapter().getItem(position);

        return HistoryDbHelper.createWwwjdicEntry(c);
    }

    @Override
    protected void copy(int position) {
        copy(getEntry(position));
    }

    @SuppressWarnings("deprecation")
    private void copy(WwwjdicEntry entry) {
        clipboardManager.setText(entry.getHeadword());

        showCopiedToast(entry.getHeadword());
    }

    @Override
    protected String getImportExportFilename() {
        File extStorage = Environment.getExternalStorageDirectory();

        return extStorage.getAbsolutePath() + "/" + EXPORT_FILENAME;
    }

    @Override
    protected void exportItems() {
        String[] items = getResources().getStringArray(
                R.array.favorites_export_dialog_items);
        boolean singleType = selectedFilter != FILTER_ALL;
        final boolean isKanji = selectedFilter == FILTER_KANJI;
        ExportItemsAdapter adapter = new ExportItemsAdapter(getActivity(),
                items, singleType);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.favorites_export_dialog_title);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (item) {
                    case EXPORT_LOCAL_BACKUP_IDX:
                        FavoritesFragment.super.exportItems();
                        break;
                    case EXPORT_LOCAL_EXPORT_IDX:
                        exportLocalCsv(isKanji);
                        break;
                    case EXPORT_ANKI_IDX:
                        exportToAnkiDeckAsync(isKanji);
                        break;
                    default:
                        // do noting
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void exportToAnkiDeckAsync(boolean isKanji) {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), AnkiExportService.class);
            intent.putExtra(AnkiExportService.EXTRA_FILTER_TYPE, selectedFilter);
            intent.putExtra(AnkiExportService.EXTRA_FILENAME,
                    getAnkiExportFilename());

            String message = getActivity()
                    .getString(R.string.exporting_to_anki);
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();

            getActivity().startService(intent);
        }
    }

    private static class ExportItemsAdapter extends ArrayAdapter<String> {

        private boolean singleType;

        ExportItemsAdapter(Context context, String[] items, boolean singleType) {
            super(context, android.R.layout.select_dialog_item,
                    android.R.id.text1, items);
            this.singleType = singleType;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int idx) {
            switch (idx) {
                case EXPORT_LOCAL_BACKUP_IDX:
                    return true;
                case EXPORT_LOCAL_EXPORT_IDX:
                    return singleType;
                case EXPORT_ANKI_IDX:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View result = super.getView(position, convertView, parent);
            result.setEnabled(isEnabled(position));

            return result;
        }

    }

    private void exportLocalCsv(final boolean isKanji) {
        new AsyncTask<Void, Void, Boolean>() {
            Exception exception;
            int count = 0;
            String exportFilename;

            @Override
            protected void onPreExecute() {
                getActivity().setProgressBarIndeterminateVisibility(true);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    File exportFile = new File(
                            WwwjdicApplication.getWwwjdicDir(),
                            getCsvExportFilename(isKanji));
                    writeBom(exportFile);

                    Writer writer = new FileWriter(exportFile, true);
                    exportFilename = exportFile.getAbsolutePath();
                    count = exportToCsv(exportFile.getAbsolutePath(), writer,
                            false);

                    if (UIUtils.isFroyo()) {
                        MediaScannerWrapper.scanFile(getActivity(),
                                exportFilename);
                    }

                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "error exporting favorites", e);
                    exception = e;

                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (isDetached() || getActivity() == null) {
                    return;
                }

                getActivity().setProgressBarIndeterminateVisibility(false);

                if (result) {
                    String message = getResources().getString(
                            R.string.favorites_exported, exportFilename, count);
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT)
                            .show();
                    notifyExportFinished(NOTIFICATION_ID_FAVORITES_EXPORT_CSV,
                            message, exportFilename);
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

    private int exportToCsv(String exportFile, Writer w, boolean showMessages) {
        CSVWriter writer = null;
        Cursor c = null;
        try {
            c = filterCursor();
            writer = new CSVWriter(w);

            boolean isKanji = selectedFilter == FILTER_KANJI;
            Resources r = getResources();
            String[] header = isKanji ? r
                    .getStringArray(R.array.kanji_csv_headers) : r
                    .getStringArray(R.array.dict_csv_headers);
            writer.writeNext(header);

            int count = 0;
            while (c.moveToNext()) {
                WwwjdicEntry entry = HistoryDbHelper.createWwwjdicEntry(c);
                String separatorChar = WwwjdicPreferences
                        .getMeaningsSeparatorCharacter(getActivity());
                // single space not allowed in resources?
                if ("space".equals(separatorChar)) {
                    separatorChar = " ";
                }
                String[] entryStr = FavoritesEntryParser.toParsedStringArray(
                        entry, separatorChar);
                writer.writeNext(entryStr);
                count++;
            }

            writer.flush();
            writer.close();

            if (showMessages) {
                String message = getResources().getString(
                        R.string.favorites_exported);
                Toast t = Toast.makeText(getActivity(),
                        String.format(message, exportFile, count),
                        Toast.LENGTH_SHORT);
                t.show();
            }

            return count;

        } catch (IOException e) {
            Log.d(TAG, "error exporting to CSV", e);
            if (showMessages) {
                String message = getResources()
                        .getString(R.string.export_error);
                Toast.makeText(getActivity(),
                        String.format(message, e.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }

            return 0;
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

    private String getCsvExportFilename(boolean isKanji) {
        String dateStr = DateFormat.format("yyyyMMdd-kkmmss", new Date())
                .toString();
        return String.format("%s-%s.%s",
                isKanji ? KANJI_CSV_EXPORT_FILENAME_BASE
                        : DICT_CSV_EXPORT_FILENAME_BASE, dateStr,
                CSV_EXPORT_FILENAME_EXT);
    }

    private String getAnkiExportFilename() {
        String dateStr = DateFormat.format("yyyyMMdd-kkmmss", new Date())
                .toString();
        return String.format("%s-%s.%s", FAVORITES_EXPORT_FILENAME_BASE,
                dateStr, ANKI_EXPORT_FILENAME_EXT);
    }

    @Override
    protected void doExport(final String exportFile) {
        new AsyncTask<Void, Void, Boolean>() {
            Exception exception;
            int count = 0;

            @Override
            protected void onPreExecute() {
                getActivity().setProgressBarIndeterminateVisibility(true);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                CSVWriter writer = null;
                Cursor c = null;

                try {
                    c = filterCursor();
                    writer = new CSVWriter(new FileWriter(exportFile));

                    while (c.moveToNext()) {
                        WwwjdicEntry entry = HistoryDbHelper
                                .createWwwjdicEntry(c);
                        long time = c.getLong(c.getColumnIndex("time"));
                        String[] entryStr = FavoritesEntryParser.toStringArray(
                                entry, time);
                        writer.writeNext(entryStr);
                        count++;
                    }

                    writer.flush();
                    writer.close();

                    if (UIUtils.isFroyo()) {
                        MediaScannerWrapper.scanFile(getActivity(), exportFile);
                    }

                    return true;
                } catch (IOException e) {
                    Log.e(TAG, "error exporting to file", e);
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

                getActivity().setProgressBarIndeterminateVisibility(false);

                if (result) {
                    String message = String.format(
                            getResources().getString(
                                    R.string.favorites_exported), exportFile,
                            count);
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT)
                            .show();
                    notifyExportFinished(NOTIFICATION_ID_FAVORITES_EXPORT_CSV,
                            message, exportFile);
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
    protected void doImport(final File importFile, final boolean deleteAfterImport) {
        new AsyncTask<Void, Void, Boolean>() {
            Exception exception;
            int count = 0;

            @Override
            protected void onPreExecute() {
                if (isDetached() || getActivity() == null) {
                    return;
                }

                getActivity().setProgressBarIndeterminateVisibility(true);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                if (isDetached() || getActivity() == null) {
                    return null;
                }

                CSVReader reader = null;

                db.beginTransaction();
                try {
                    db.deleteAllFavorites();

                    reader = new CSVReader(new FileReader(importFile));

                    String[] record = null;
                    while ((record = reader.readNext()) != null) {
                        WwwjdicEntry entry = FavoritesEntryParser
                                .fromStringArray(record);
                        long time = Long
                                .parseLong(record[FavoritesEntryParser.TIME_IDX]);
                        db.addFavorite(entry, time);
                        count++;
                    }
                    db.setTransactionSuccessful();

                    return true;
                } catch (IOException e) {
                    Log.e(TAG, "error importing favorites", e);
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
                    if (deleteAfterImport) {
                        importFile.delete();
                    }
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (isDetached() || getActivity() == null) {
                    return;
                }

                if (result == null) {
                    return;
                }

                getActivity().setProgressBarIndeterminateVisibility(false);

                if (result) {
                    String message = getResources().getString(
                            R.string.favorites_imported);
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
            return db.getFavorites();

        }

        return db.getFavoritesByType(selectedFilter);
    }

    @Override
    protected String[] getFilterTypes() {
        return getResources().getStringArray(R.array.filter_types_favorites);
    }

    @Override
    public Loader<LoaderResult<Cursor>> onCreateLoader(int id, Bundle args) {
        HistoryLoaderBase loader = new FavoritesLoader(getActivity(), db);
        loader.setSelectedFilter(selectedFilter);

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult<Cursor>> loader,
            LoaderResult<Cursor> data) {
        getActivity().setProgressBarIndeterminateVisibility(
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
        getActivity().setProgressBarIndeterminateVisibility(
                false);

        CursorAdapter adapter = (CursorAdapter) getListAdapter();
        adapter.swapCursor(null);
    }

}
