package org.nick.wwwjdic.history;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.DictionaryEntryDetail;
import org.nick.wwwjdic.KanjiEntryDetail;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicEntry;
import org.nick.wwwjdic.history.FavoritesItem.FavoriteStatusChangedListener;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class Favorites extends HistoryBase implements
        FavoriteStatusChangedListener {

    private static final String TAG = Favorites.class.getSimpleName();

    protected void setupAdapter() {
        Cursor cursor = db.getFavorites();
        startManagingCursor(cursor);
        FavoritesAdapter adapter = new FavoritesAdapter(this, cursor, this);
        setListAdapter(adapter);
    }

    @Override
    protected void deleteAll() {
        db.deleteAllFavorites();
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
    protected void exportItems() {
        CSVWriter writer = null;

        try {
            Cursor c = db.getFavorites();
            File extStorage = Environment.getExternalStorageDirectory();
            String exportFile = extStorage.getAbsolutePath() + "/favorites.csv";
            writer = new CSVWriter(new FileWriter(exportFile));

            while (c.moveToNext()) {
                WwwjdicEntry entry = HistoryDbHelper.createWwwjdicEntry(c);
                long time = c.getLong(c.getColumnIndex("time"));
                String[] entryStr = FavoritesEntryParser.toStringArray(entry,
                        time);
                writer.writeNext(entryStr);

            }

            String message = getResources().getString(
                    R.string.favorites_exported);
            Toast t = Toast.makeText(this, String.format(message, exportFile),
                    Toast.LENGTH_SHORT);
            t.show();
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
    }

    @Override
    protected void importItems() {
        CSVReader reader = null;

        SQLiteDatabase s = db.getWritableDatabase();
        s.beginTransaction();
        try {
            File extStorage = Environment.getExternalStorageDirectory();
            String importFile = extStorage.getAbsolutePath() + "/favorites.csv";
            reader = new CSVReader(new FileReader(importFile));

            String[] record = null;
            while ((record = reader.readNext()) != null) {
                WwwjdicEntry entry = FavoritesEntryParser
                        .fromStringArray(record);
                long time = Long.parseLong(record[3]);
                db.addFavorite(entry, time);

            }
            s.setTransactionSuccessful();

            refresh();
            String message = getResources().getString(
                    R.string.favorites_imported);
            Toast t = Toast.makeText(this, String.format(message, importFile),
                    Toast.LENGTH_SHORT);
            t.show();
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
    }

}
