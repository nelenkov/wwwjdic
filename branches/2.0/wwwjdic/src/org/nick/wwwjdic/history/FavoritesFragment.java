package org.nick.wwwjdic.history;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.nick.wwwjdic.DictionaryEntryDetail;
import org.nick.wwwjdic.KanjiEntryDetail;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicApplication;
import org.nick.wwwjdic.WwwjdicPreferences;
import org.nick.wwwjdic.history.FavoritesItem.FavoriteStatusChangedListener;
import org.nick.wwwjdic.model.DictionaryEntry;
import org.nick.wwwjdic.model.KanjiEntry;
import org.nick.wwwjdic.model.WwwjdicEntry;
import org.nick.wwwjdic.utils.Analytics;
import org.nick.wwwjdic.utils.LoaderResult;
import org.nick.wwwjdic.utils.MediaScannerWrapper;
import org.nick.wwwjdic.utils.UIUtils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
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

    private static final String KANJI_CSV_EXPORT_FILENAME_BASE = "wwwjdic-favorites-kanji";
    private static final String DICT_CSV_EXPORT_FILENAME_BASE = "wwwjdic-favorites-dict";
    private static final String CSV_EXPORT_FILENAME_EXT = "csv";

    private static final int EXPORT_LOCAL_BACKUP_IDX = 0;
    private static final int EXPORT_LOCAL_EXPORT_IDX = 1;
    private static final int EXPORT_ANKI_IDX = 2;

    private ProgressDialog progressDialog;

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

        getSherlockActivity()
                .setSupportProgressBarIndeterminateVisibility(true);
        // LoaderManager.enableDebugLogging(true);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void deleteAll() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                getSherlockActivity()
                        .setSupportProgressBarIndeterminateVisibility(true);
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
                getSherlockActivity()
                        .setSupportProgressBarIndeterminateVisibility(
                        false);
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

        Analytics.event("lookupFromFavorites", getActivity());

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
        AnkiExportTask task = new AnkiExportTask();
        task.execute(isKanji);
    }

    private class AnkiExportTask extends AsyncTask<Boolean, Object, Boolean> {

        private Throwable error;
        private String exportFilename;

        AnkiExportTask() {
        }

        @Override
        protected void onPreExecute() {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();

            }
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage(getString(R.string.exporting_to_anki));
            progressDialog.setCancelable(true);
            progressDialog.setButton(ProgressDialog.BUTTON_NEUTRAL,
                    getString(R.string.cancel), new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cancel(true);
                        }
                    });
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            try {
                boolean isKanji = params[0];
                exportFilename = exportToAnkiDeck(isKanji);

                if (UIUtils.isFroyo()) {
                    MediaScannerWrapper.scanFile(getActivity(), exportFilename);
                }

                return true;
            } catch (Exception e) {
                error = e;
                Log.d(TAG, "Error exporting favorites to Anki", e);
                deleteIncompleteFile();

                return false;
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            deleteIncompleteFile();
        }

        private void deleteIncompleteFile() {
            Log.d(TAG, "Anki export cancelled, deleting incomplete files...");
            if (exportFilename == null) {
                return;
            }
            File f = new File(exportFilename);
            boolean success = f.delete();
            if (success) {
                Log.d(TAG, "successfully deleted " + f.getAbsolutePath());
            } else {
                Log.d(TAG, "failed to delet " + f.getAbsolutePath());
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (isDetached() || getActivity() == null) {
                return;
            }

            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            Resources r = getResources();
            String template = result ? r
                    .getString(R.string.anki_export_success) : r
                    .getString(R.string.anki_export_failure);
            String message = result ? String.format(template, exportFilename)
                    : String.format(template, error.getMessage());
            Toast t = Toast.makeText(getActivity(), message, Toast.LENGTH_LONG);
            t.show();
        }
    }

    private String exportToAnkiDeck(boolean isKanji) {
        AnkiGenerator generator = new AnkiGenerator(getActivity());
        String filename = getCsvExportFilename(isKanji).replace(".csv", "")
                + ".anki";
        File exportFile = new File(WwwjdicApplication.getWwwjdicDir(), filename);
        Log.d(TAG,
                "exporting favorites to Anki: " + exportFile.getAbsolutePath());

        int size = 0;
        if (isKanji) {
            List<KanjiEntry> kanjis = new ArrayList<KanjiEntry>();
            Cursor c = null;
            try {
                c = filterCursor();
                while (c.moveToNext()) {
                    WwwjdicEntry entry = HistoryDbHelper.createWwwjdicEntry(c);
                    kanjis.add((KanjiEntry) entry);
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            size = generator.createKanjiAnkiFile(exportFile.getAbsolutePath(),
                    kanjis);
        } else {
            List<DictionaryEntry> words = new ArrayList<DictionaryEntry>();
            Cursor c = null;
            try {
                c = filterCursor();
                while (c.moveToNext()) {
                    WwwjdicEntry entry = HistoryDbHelper.createWwwjdicEntry(c);
                    words.add((DictionaryEntry) entry);
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            size = generator.createDictAnkiFile(exportFile.getAbsolutePath(),
                    words);
        }

        Analytics.event("favoritesAnkiExport", getActivity());
        Log.d(TAG,
                String.format("Exported %d entries to %s", size,
                        exportFile.getAbsolutePath()));

        return exportFile.getAbsolutePath();
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
                return singleType;
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
                getSherlockActivity()
                        .setSupportProgressBarIndeterminateVisibility(true);
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

                    Analytics.event("favoritesLocalCsvExport", getActivity());

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

                getSherlockActivity()
                        .setSupportProgressBarIndeterminateVisibility(
                        false);

                if (result) {
                    String message = getResources().getString(
                            R.string.favorites_exported);
                    Toast t = Toast.makeText(getActivity(),
                            String.format(message, exportFilename, count),
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

    @Override
    protected void doExport(final String exportFile) {
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

                    Analytics.event("favoritesExport", getActivity());

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

                getSherlockActivity()
                        .setSupportProgressBarIndeterminateVisibility(
                        false);

                if (result) {
                    String message = getResources().getString(
                            R.string.favorites_exported);
                    Toast t = Toast.makeText(getActivity(),
                            String.format(message, exportFile, count),
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

                    Analytics.event("favoritesImport", getActivity());

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
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (isDetached() || getActivity() == null) {
                    return;
                }

                getSherlockActivity()
                        .setSupportProgressBarIndeterminateVisibility(
                        false);

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
