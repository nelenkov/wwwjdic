package org.nick.wwwjdic.history;

import org.nick.wwwjdic.DictionaryEntry;
import org.nick.wwwjdic.KanjiEntry;
import org.nick.wwwjdic.SearchCriteria;
import org.nick.wwwjdic.WwwjdicEntry;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HistoryDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 12;

    private static final String DATABASE_NAME = "wwwjdic_history.db";

    private static final String HISTORY_TABLE_NAME = "search_history";

    private static final String FAVORITES_TABLE_NAME = "favorites";

    private static final String[] HISTORY_ALL_COLUMNS = { "_id", "time",
            "query_string", "is_exact_match", "is_kanji_lookup",
            "is_romanized_japanese", "is_common_words_only", "dictionary",
            "kanji_search_type", "min_stroke_count", "max_stroke_count" };

    private static final String HISTORY_TABLE_CREATE = "create table "
            + HISTORY_TABLE_NAME
            + " (_id integer primary key autoincrement, time integer not null, "
            + "query_string text not null, is_exact_match integer, "
            + "is_kanji_lookup integer not null, is_romanized_japanese integer, "
            + "is_common_words_only integer, dictionary text, kanji_search_type text, "
            + "min_stroke_count integer, max_stroke_count integer);";

    private static final String[] FAVORITES_ALL_COLUMNS = { "_id", "time",
            "is_kanji", "heading", "dict_str" };

    private static final String FAVORITES_TABLE_CREATE = "create table "
            + FAVORITES_TABLE_NAME
            + " (_id integer primary key autoincrement, time integer not null, "
            + "is_kanji integer not null, heading text not null, dict_str text not null);";

    public HistoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(HISTORY_TABLE_CREATE);
        db.execSQL(FAVORITES_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists search_history");
        db.execSQL("drop table if exists favorites");
        onCreate(db);
    }

    public void addSearchCriteria(SearchCriteria criteria) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = fromCriteria(criteria);
        db.insertOrThrow(HISTORY_TABLE_NAME, null, values);
    }

    public long addFavorite(WwwjdicEntry entry) {
        SQLiteDatabase db = getWritableDatabase();
        long result = addFavorite(db, entry);

        return result;
    }

    private long addFavorite(SQLiteDatabase db, WwwjdicEntry entry) {
        ContentValues values = fromEntry(entry);

        return db.insertOrThrow(FAVORITES_TABLE_NAME, null, values);
    }

    private ContentValues fromEntry(WwwjdicEntry entry) {
        ContentValues values = new ContentValues();
        values.put("time", System.currentTimeMillis());
        values.put("is_kanji", entry.isKanji() ? 1 : 0);
        values.put("heading", entry.getHeading());
        values.put("dict_str", entry.getDictString());

        return values;
    }

    private ContentValues fromCriteria(SearchCriteria criteria) {
        ContentValues values = new ContentValues();
        values.put("time", System.currentTimeMillis());
        values.put("query_string", criteria.getQueryString());
        values.put("is_exact_match", criteria.isExactMatch());
        values.put("is_kanji_lookup", criteria.isKanjiLookup() ? 1 : 0);
        values.put("is_romanized_japanese", criteria.isRomanizedJapanese());
        values.put("is_common_words_only", criteria.isCommonWordsOnly());
        values.put("dictionary", criteria.getDictionary());
        values.put("kanji_search_type", criteria.getKanjiSearchType());
        values.put("min_stroke_count", criteria.getMinStrokeCount());
        values.put("max_stroke_count", criteria.getMaxStrokeCount());

        return values;
    }

    public Cursor getHistory() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor result = db.query(HISTORY_TABLE_NAME, HISTORY_ALL_COLUMNS, null,
                null, null, null, "time desc");

        return result;
    }

    public Cursor getFavorites() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor result = db.query(FAVORITES_TABLE_NAME, FAVORITES_ALL_COLUMNS,
                null, null, null, null, "time desc");

        return result;
    }

    public Cursor getDictionaryHistory() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor result = db.query(HISTORY_TABLE_NAME, HISTORY_ALL_COLUMNS,
                "is_kanji_lookup = 0", null, null, null, "time desc");

        return result;
    }

    public Cursor getKanjiHistory() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor result = db.query(HISTORY_TABLE_NAME, HISTORY_ALL_COLUMNS,
                "is_kanji_lookup = 1", null, null, null, "time desc");

        return result;
    }

    public static SearchCriteria createCriteria(Cursor cursor) {
        int idx = cursor.getColumnIndex("is_kanji_lookup");
        boolean isKanji = cursor.getInt(idx) == 1;
        idx = cursor.getColumnIndex("query_string");
        String queryString = cursor.getString(idx);
        idx = cursor.getColumnIndex("_id");
        long id = cursor.getLong(idx);

        if (isKanji) {
            idx = cursor.getColumnIndex("kanji_search_type");
            String searchType = cursor.getString(idx);
            idx = cursor.getColumnIndex("min_stroke_count");
            if (cursor.isNull(idx)) {
                SearchCriteria result = SearchCriteria.createForKanji(
                        queryString, searchType);
                result.setId(id);

                return result;
            }

            int minStrokeCount = cursor.getInt(idx);
            idx = cursor.getColumnIndex("max_stroke_count");
            Integer maxStrokeCount = null;
            if (!cursor.isNull(idx)) {
                maxStrokeCount = cursor.getInt(idx);
            }

            SearchCriteria result = SearchCriteria.createWithStrokeCount(
                    queryString, searchType, minStrokeCount, maxStrokeCount);
            result.setId(id);

            return result;
        }

        boolean isExactMatch = cursor.getInt(cursor
                .getColumnIndex("is_exact_match")) == 1;
        boolean isRomanized = cursor.getInt(cursor
                .getColumnIndex("is_romanized_japanese")) == 1;
        boolean isCommonWordsOnly = cursor.getInt(cursor
                .getColumnIndex("is_common_words_only")) == 1;
        String dictionary = cursor.getString(cursor
                .getColumnIndex("dictionary"));

        SearchCriteria result = SearchCriteria.createForDictionary(queryString,
                isExactMatch, isRomanized, isCommonWordsOnly, dictionary);
        result.setId(id);

        return result;

    }

    public static WwwjdicEntry createWwwjdicEntry(Cursor cursor) {
        int idx = cursor.getColumnIndex("is_kanji");
        boolean isKanji = cursor.getInt(idx) == 1;
        idx = cursor.getColumnIndex("dict_str");
        String dictStr = cursor.getString(idx);
        idx = cursor.getColumnIndex("_id");
        long id = cursor.getLong(idx);

        WwwjdicEntry result = null;
        if (isKanji) {
            result = KanjiEntry.parseKanjidic(dictStr);
        } else {
            result = DictionaryEntry.parseEdict(dictStr);
        }
        result.setId(id);

        return result;

    }

    public void deleteHistoryItem(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(HISTORY_TABLE_NAME, "_id = " + id, null);
    }

    public void deleteFavorite(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(FAVORITES_TABLE_NAME, "_id = " + id, null);
    }

    public void deleteAllHistory() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(HISTORY_TABLE_NAME, null, null);
    }

    public void deleteAllFavorites() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(FAVORITES_TABLE_NAME, null, null);
    }

    public Long getFavoriteId(String heading) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(FAVORITES_TABLE_NAME, new String[] { "_id" },
                    "heading = ?", new String[] { heading }, null, null,
                    "time desc");
            int count = cursor.getCount();
            if (count == 0) {
                return null;
            }

            int idx = cursor.getColumnIndex("_id");
            cursor.moveToFirst();

            return cursor.getLong(idx);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
