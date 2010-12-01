package org.nick.wwwjdic.history;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.nick.wwwjdic.Analytics;
import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.DictionaryEntryDetail;
import org.nick.wwwjdic.KanjiEntryDetail;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicEntry;
import org.nick.wwwjdic.history.FavoritesItem.FavoriteStatusChangedListener;

import android.content.Intent;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class Favorites extends HistoryBase implements
        FavoriteStatusChangedListener {

    private static final String TAG = Favorites.class.getSimpleName();

    private static final String EXPORT_FILENAME = "wwwjdic/favorites.csv";

    protected void setupAdapter() {
        Cursor cursor = filterCursor();
        startManagingCursor(cursor);
        FavoritesAdapter adapter = new FavoritesAdapter(this, cursor, this);
        setListAdapter(adapter);
    }

    @Override
    protected void deleteAll() {
        Cursor c = filterCursor();

        db.beginTransaction();
        try {
            while (c.moveToNext()) {
                int id = c.getInt(c.getColumnIndex("_id"));
                db.deleteFavorite(id);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        refresh();
    }

    @Override
    protected int getContentView() {
        return R.layout.favorites;
    }

    @Override
    protected void deleteCurrentItem() {
        Cursor c = getCursor();
        int idx = c.getColumnIndex("_id");
        int id = c.getInt(idx);
        db.deleteFavorite(id);

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
    protected void lookupCurrentItem() {
        WwwjdicEntry entry = getCurrentEntry();

        Intent intent = null;
        if (entry.isKanji()) {
            intent = new Intent(this, KanjiEntryDetail.class);
            intent.putExtra(Constants.KANJI_ENTRY_KEY, entry);
            intent.putExtra(Constants.IS_FAVORITE, true);
        } else {
            intent = new Intent(this, DictionaryEntryDetail.class);
            intent.putExtra(Constants.ENTRY_KEY, entry);
            intent.putExtra(Constants.IS_FAVORITE, true);
        }

        Analytics.event("lookupFromFavorites", this);

        startActivity(intent);
    }

    @Override
    protected void copyCurrentItem() {
        WwwjdicEntry entry = getCurrentEntry();
        clipboardManager.setText(entry.getHeadword());
    }

    private WwwjdicEntry getCurrentEntry() {
        Cursor c = getCursor();
        WwwjdicEntry entry = HistoryDbHelper.createWwwjdicEntry(c);
        return entry;
    }

    @Override
    protected String getImportExportFilename() {
        File extStorage = Environment.getExternalStorageDirectory();

        return extStorage.getAbsolutePath() + "/" + EXPORT_FILENAME;
    }

    @Override
    protected void doExport(final String exportFile) {
        CSVWriter writer = null;

        try {
            final Cursor c = filterCursor();

            writer = new CSVWriter(new FileWriter(exportFile));

            int count = 0;
            while (c.moveToNext()) {
                WwwjdicEntry entry = HistoryDbHelper.createWwwjdicEntry(c);
                long time = c.getLong(c.getColumnIndex("time"));
                String[] entryStr = FavoritesEntryParser.toStringArray(entry,
                        time);
                writer.writeNext(entryStr);
                count++;
            }

            Analytics.event("favoritesExport", this);

            String message = getResources().getString(
                    R.string.favorites_exported);
            Toast t = Toast.makeText(Favorites.this, String.format(message,
                    exportFile, count), Toast.LENGTH_SHORT);
            t.show();

        } catch (IOException e) {
            String message = getResources().getString(R.string.export_error);
            Toast.makeText(Favorites.this,
                    String.format(message, e.getMessage()), Toast.LENGTH_SHORT)
                    .show();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Log.w(TAG, "error closing CSV writer", e);
                }
            }
        }
    }

    @Override
    protected void doImport(String importFile) {
        CSVReader reader = null;

        db.beginTransaction();
        try {
            db.deleteAllFavorites();

            reader = new CSVReader(new FileReader(importFile));
            if (reader == null) {
                return;
            }

            String[] record = null;
            int count = 0;
            while ((record = reader.readNext()) != null) {
                WwwjdicEntry entry = FavoritesEntryParser
                        .fromStringArray(record);
                long time = Long
                        .parseLong(record[FavoritesEntryParser.TIME_IDX]);
                db.addFavorite(entry, time);
                count++;
            }
            db.setTransactionSuccessful();

            refresh();

            Analytics.event("favoritesImport", this);

            String message = getResources().getString(
                    R.string.favorites_imported);
            Toast t = Toast.makeText(this, String.format(message, importFile,
                    count), Toast.LENGTH_SHORT);
            t.show();
        } catch (IOException e) {
            Log.e(TAG, "error importing favorites", e);
            String message = getResources().getString(R.string.import_error);
            Toast.makeText(this, String.format(message, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
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

}
