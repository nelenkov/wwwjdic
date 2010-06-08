package org.nick.wwwjdic.history;

import org.nick.wwwjdic.SearchCriteria;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HistoryDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "wwwjdic_history.db";
    private static final String HISTORY_TABLE_NAME = "search_history";

    private static final String[] ALL_COLUMNS = { "_id", "time",
            "query_string", "is_exact_match", "is_kanji_lookup",
            "is_romanized_japanese", "is_common_words_only", "dictionary",
            "kanji_search_type", "min_stroke_count", "max_stroke_count",
            "is_favorite" };

    private static final String HISTORY_TABLE_CREATE = "create table "
            + HISTORY_TABLE_NAME
            + " (_id integer primary key autoincrement, time integer not null, "
            + "query_string text not null, is_exact_match integer, "
            + "is_kanji_lookup integer not null, is_romanized_japanese integer, "
            + "is_common_words_only integer, dictionary text, kanji_search_type text, "
            + "min_stroke_count integer, max_stroke_count integer, "
            + "is_favorite integer not null);";

    public HistoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(HISTORY_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists search_history");
        onCreate(db);
    }

    public void addSearchCriteria(SearchCriteria criteria) {
        SQLiteDatabase db = getWritableDatabase();
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
        values.put("is_favorite", 0);

        db.insertOrThrow(HISTORY_TABLE_NAME, null, values);
    }

    public Cursor getHistory() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor result = db.query(HISTORY_TABLE_NAME, ALL_COLUMNS,
                "is_favorite = 0", null, null, null, "time desc");

        return result;
    }

    public Cursor getFavorites() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor result = db.query(HISTORY_TABLE_NAME, ALL_COLUMNS,
                "is_favorite = 1", null, null, null, "time desc");

        return result;
    }

    public Cursor getDictionaryHistory() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor result = db.query(HISTORY_TABLE_NAME, ALL_COLUMNS,
                "is_kanji_lookup = 0", null, null, null, "time desc");

        return result;
    }

    public Cursor getKanjiHistory() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor result = db.query(HISTORY_TABLE_NAME, ALL_COLUMNS,
                "is_kanji_lookup = 1", null, null, null, "time desc");

        return result;
    }

    public static SearchCriteria createCriteria(Cursor cursor) {
        int idx = cursor.getColumnIndex("is_kanji_lookup");
        boolean isKanji = cursor.getInt(idx) == 1;
        idx = cursor.getColumnIndex("query_string");
        String queryString = cursor.getString(idx);
        idx = cursor.getColumnIndex("_id");
        int id = cursor.getInt(idx);
        idx = cursor.getColumnIndex("is_favorite");
        boolean isFavorite = cursor.getInt(idx) == 1;

        if (isKanji) {
            idx = cursor.getColumnIndex("kanji_search_type");
            String searchType = cursor.getString(idx);
            idx = cursor.getColumnIndex("min_stroke_count");
            if (cursor.isNull(idx)) {
                SearchCriteria result = SearchCriteria.createForKanji(
                        queryString, searchType);
                result.setId(id);
                result.setFavorite(isFavorite);

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
            result.setFavorite(isFavorite);

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
        result.setFavorite(isFavorite);

        return result;

    }

    public void toggleFavorite(int id, boolean isFavorite) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_favorite", isFavorite ? 1 : 0);
        db.update(HISTORY_TABLE_NAME, values, "_id = " + id, null);
    }

    public void deleteHistoryItem(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(HISTORY_TABLE_NAME, "_id = " + id, null);
    }

    public void deleteAllHistory() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(HISTORY_TABLE_NAME, "is_favorite = 0", null);
    }

    public void deleteAllFavorites() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(HISTORY_TABLE_NAME, "is_favorite = 1", null);
    }
}
