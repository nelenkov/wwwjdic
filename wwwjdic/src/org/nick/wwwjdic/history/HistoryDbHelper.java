package org.nick.wwwjdic.history;

import java.util.ArrayList;
import java.util.List;

import org.nick.wwwjdic.DictionaryEntry;
import org.nick.wwwjdic.KanjiEntry;
import org.nick.wwwjdic.SearchCriteria;
import org.nick.wwwjdic.WwwjdicEntry;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class HistoryDbHelper extends SQLiteOpenHelper {

    private static final String TAG = HistoryDbHelper.class.getSimpleName();

    private static final String ID = "_id";
    private static final String TIME = "time";

    private static final String FAVORITES_DICT_STR = "dict_str";
    private static final String FAVORITES_HEADWORD = "headword";
    private static final String FAVORITES_IS_KANJI = "is_kanji";

    public static final int FAVORITES_TYPE_DICT = 0;
    public static final int FAVORITES_TYPE_KANJI = 1;

    private static final String HISTORY_MAX_RESULTS = "max_results";
    private static final String HISTORY_MAX_STROKE_COUNT = "max_stroke_count";
    private static final String HISTORY_MIN_STROKE_COUNT = "min_stroke_count";
    private static final String HISTORY_KANJI_SEARCH_TYPE = "kanji_search_type";
    private static final String HISTORY_DICTIONARY = "dictionary";
    private static final String HISTORY_IS_COMMON_WORDS_ONLY = "is_common_words_only";
    private static final String HISTORY_IS_ROMANIZED_JAPANESE = "is_romanized_japanese";
    private static final String HISTORY_IS_EXACT_MATCH = "is_exact_match";
    private static final String HISTORY_QUERY_STRING = "query_string";
    private static final String HISTORY_SEARCH_TYPE = "search_type";

    public static final int HISTORY_SEARCH_TYPE_DICT = 0;
    public static final int HISTORY_SEARCH_TYPE_KANJI = 1;
    public static final int HISTORY_SEARCH_TYPE_EXAMPLES = 2;

    private static final int DATABASE_VERSION = 14;

    private static final String DATABASE_NAME = "wwwjdic_history.db";

    private static final String HISTORY_TABLE_NAME = "search_history";

    private static final String HISTORY_BACKUP_TABLE_NAME = "search_history_backup";

    private static final String FAVORITES_TABLE_NAME = "favorites";

    public static final String[] HISTORY_ALL_COLUMNS = { ID, TIME,
            HISTORY_QUERY_STRING, HISTORY_IS_EXACT_MATCH, HISTORY_SEARCH_TYPE,
            HISTORY_IS_ROMANIZED_JAPANESE, HISTORY_IS_COMMON_WORDS_ONLY,
            HISTORY_DICTIONARY, HISTORY_KANJI_SEARCH_TYPE,
            HISTORY_MIN_STROKE_COUNT, HISTORY_MAX_STROKE_COUNT,
            HISTORY_MAX_RESULTS };

    private static final String HISTORY_BACKUP_TABLE_CREATE = "create table "
            + HISTORY_BACKUP_TABLE_NAME
            + " (_id integer primary key autoincrement, time integer not null, "
            + "query_string text not null, is_exact_match integer, "
            + "is_kanji integer not null, is_romanized_japanese integer, "
            + "is_common_words_only integer, dictionary text, kanji_search_type text, "
            + "min_stroke_count integer, max_stroke_count integer);";

    private static final String HISTORY_TABLE_CREATE = "create table "
            + HISTORY_TABLE_NAME
            + " (_id integer primary key autoincrement, time integer not null, "
            + "query_string text not null, is_exact_match integer, "
            + "search_type integer not null, is_romanized_japanese integer, "
            + "is_common_words_only integer, dictionary text, kanji_search_type text, "
            + "min_stroke_count integer, max_stroke_count integer, max_results integer);";

    public static final String[] FAVORITES_ALL_COLUMNS = { ID, TIME,
            FAVORITES_IS_KANJI, FAVORITES_HEADWORD, FAVORITES_DICT_STR };

    private static final String FAVORITES_TABLE_CREATE = "create table "
            + FAVORITES_TABLE_NAME
            + " (_id integer primary key autoincrement, time integer not null, "
            + "is_kanji integer not null, headword text not null, dict_str text not null);";

    private SQLiteStatement favoritesCountStatement;
    private SQLiteStatement historyCountStatement;

    private Context context;

    private static HistoryDbHelper instance;

    public static HistoryDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new HistoryDbHelper(context);
        }

        return instance;
    }

    private HistoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL(HISTORY_TABLE_CREATE);
            db.execSQL(FAVORITES_TABLE_CREATE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, String.format("upgrading db from version %d to %d",
                oldVersion, newVersion));
        upgradeDbTov14(db);
        Log.d(TAG, "done");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        favoritesCountStatement = db.compileStatement("select count(*) from "
                + FAVORITES_TABLE_NAME + " where is_kanji = ?");
        historyCountStatement = db.compileStatement("select count(*) from "
                + HISTORY_TABLE_NAME + " where search_type = ?");
    }

    private void upgradeDbTov14(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL(HISTORY_BACKUP_TABLE_CREATE);
            db.execSQL("insert into " + HISTORY_BACKUP_TABLE_NAME
                    + " select * from " + HISTORY_TABLE_NAME);
            db.execSQL("drop table " + HISTORY_TABLE_NAME);
            db.execSQL(HISTORY_TABLE_CREATE);
            db.execSQL("insert into " + HISTORY_TABLE_NAME
                    + "(_id, time, query_string, is_exact_match, "
                    + "search_type, is_romanized_japanese, "
                    + "is_common_words_only, dictionary, "
                    + "kanji_search_type, min_stroke_count, max_stroke_count)"
                    + "select * from " + HISTORY_BACKUP_TABLE_NAME);
            db.execSQL("drop table " + HISTORY_BACKUP_TABLE_NAME);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void addSearchCriteria(SearchCriteria criteria) {
        addSearchCriteria(criteria, System.currentTimeMillis());
    }

    public synchronized void addSearchCriteria(SearchCriteria criteria,
            long currentTimeMillis) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = fromCriteria(criteria, currentTimeMillis);
        db.insertOrThrow(HISTORY_TABLE_NAME, null, values);
    }

    public synchronized long addFavorite(WwwjdicEntry entry) {
        return addFavorite(entry, System.currentTimeMillis());
    }

    public synchronized long addFavorite(WwwjdicEntry entry,
            long currentTimeInMillis) {
        SQLiteDatabase db = getWritableDatabase();
        long result = addFavorite(db, entry, currentTimeInMillis);

        return result;
    }

    private long addFavorite(SQLiteDatabase db, WwwjdicEntry entry,
            long currentTimeInMillis) {
        ContentValues values = fromEntry(entry, currentTimeInMillis);

        return db.insertOrThrow(FAVORITES_TABLE_NAME, null, values);
    }

    private ContentValues fromEntry(WwwjdicEntry entry, long currentTimeInMillis) {
        ContentValues values = new ContentValues();
        values.put(TIME, currentTimeInMillis);
        values.put(FAVORITES_IS_KANJI,
                entry.isKanji() ? SearchCriteria.CRITERIA_TYPE_KANJI
                        : SearchCriteria.CRITERIA_TYPE_DICT);
        values.put(FAVORITES_HEADWORD, entry.getHeadword());
        values.put(FAVORITES_DICT_STR, entry.getDictString());

        return values;
    }

    private ContentValues fromCriteria(SearchCriteria criteria,
            long currentTimeMillis) {
        ContentValues values = new ContentValues();
        values.put(TIME, currentTimeMillis);
        values.put(HISTORY_QUERY_STRING, criteria.getQueryString());
        values.put(HISTORY_IS_EXACT_MATCH, criteria.isExactMatch());
        values.put(HISTORY_SEARCH_TYPE, criteria.getType());
        values.put(HISTORY_IS_ROMANIZED_JAPANESE,
                criteria.isRomanizedJapanese());
        values.put(HISTORY_IS_COMMON_WORDS_ONLY, criteria.isCommonWordsOnly());
        values.put(HISTORY_DICTIONARY, criteria.getDictionary());
        values.put(HISTORY_KANJI_SEARCH_TYPE, criteria.getKanjiSearchType());
        values.put(HISTORY_MIN_STROKE_COUNT, criteria.getMinStrokeCount());
        values.put(HISTORY_MAX_STROKE_COUNT, criteria.getMaxStrokeCount());
        values.put(HISTORY_MAX_RESULTS, criteria.getNumMaxResults());

        return values;
    }

    public Cursor getHistory() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor result = db.query(HISTORY_TABLE_NAME, HISTORY_ALL_COLUMNS, null,
                null, null, null, "time desc");

        return result;
    }

    public Cursor getHistoryByType(int type) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor result = db.query(HISTORY_TABLE_NAME, HISTORY_ALL_COLUMNS,
                "search_type = ?", new String[] { Integer.toString(type) },
                null, null, "time desc");

        return result;
    }

    public long getHistoryCountByType(int type) {
        SQLiteDatabase db = getReadableDatabase();
        if (historyCountStatement == null) {
            historyCountStatement = db.compileStatement("select count(*) from "
                    + HISTORY_TABLE_NAME + " where search_type = ?");
        }
        historyCountStatement.bindString(1, Integer.toString(type));

        return historyCountStatement.simpleQueryForLong();
    }

    public long getDictHistoryCount() {
        return getHistoryCountByType(HISTORY_SEARCH_TYPE_DICT);
    }

    public long getKanjiHistoryCount() {
        return getHistoryCountByType(HISTORY_SEARCH_TYPE_KANJI);
    }

    public long getExamplesHistoryCount() {
        return getHistoryCountByType(HISTORY_SEARCH_TYPE_EXAMPLES);
    }

    public long getFavoritesCountByType(int type) {
        SQLiteDatabase db = getReadableDatabase();
        if (favoritesCountStatement == null) {
            favoritesCountStatement = db
                    .compileStatement("select count(*) from "
                            + FAVORITES_TABLE_NAME + " where is_kanji = ?");
        }
        favoritesCountStatement.bindString(1, Integer.toString(type));

        return favoritesCountStatement.simpleQueryForLong();
    }

    public long getDictFavoritesCount() {
        return getFavoritesCountByType(FAVORITES_TYPE_DICT);
    }

    public long getKanjiFavoritesCount() {
        return getFavoritesCountByType(FAVORITES_TYPE_KANJI);
    }

    public List<String> getRecentHistoryByType(int type, int top) {
        List<String> result = new ArrayList<String>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor c = null;
        try {
            c = db.query(HISTORY_TABLE_NAME, new String[] {
                    HISTORY_QUERY_STRING, HISTORY_KANJI_SEARCH_TYPE },
                    "search_type = ?", new String[] { Integer.toString(type) },
                    null, null, "time desc", Integer.toString(top));
            while (c.moveToNext()) {
                String searchQuery = c.getString(c
                        .getColumnIndex(HISTORY_QUERY_STRING));
                String historyStr = searchQuery;
                if (!c.isNull(c.getColumnIndex(HISTORY_KANJI_SEARCH_TYPE))) {
                    String kanjiSearchType = c.getString(c
                            .getColumnIndex(HISTORY_KANJI_SEARCH_TYPE));
                    String kanjiSearchName = HistoryUtils
                            .lookupKanjiSearchName(kanjiSearchType,
                                    searchQuery, context);
                    historyStr = String.format("%s(%s)", searchQuery,
                            kanjiSearchName);
                }

                result.add(historyStr);
            }

            return result;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public List<String> getRecentDictHistory(int top) {
        return getRecentHistoryByType(HISTORY_SEARCH_TYPE_DICT, top);
    }

    public List<String> getRecentKanjiHistory(int top) {
        return getRecentHistoryByType(HISTORY_SEARCH_TYPE_KANJI, top);
    }

    public List<String> getRecentExamplesHistory(int top) {
        return getRecentHistoryByType(HISTORY_SEARCH_TYPE_EXAMPLES, top);
    }

    public Cursor getFavorites() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor result = db.query(FAVORITES_TABLE_NAME, FAVORITES_ALL_COLUMNS,
                null, null, null, null, "time desc");

        return result;
    }

    public List<String> getRecentFavoritesByType(int type, int top) {
        List<String> result = new ArrayList<String>();

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(FAVORITES_TABLE_NAME,
                    new String[] { FAVORITES_HEADWORD }, "is_kanji = ?",
                    new String[] { Integer.toString(type) }, null, null,
                    "time desc", Integer.toString(top));
            while (c.moveToNext()) {
                String searchQuery = c.getString(c
                        .getColumnIndex(FAVORITES_HEADWORD));
                result.add(searchQuery);
            }

            return result;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public List<String> getRecentDictFavorites(int top) {
        return getRecentFavoritesByType(FAVORITES_TYPE_DICT, top);
    }

    public List<String> getRecentKanjiFavorites(int top) {
        return getRecentFavoritesByType(FAVORITES_TYPE_KANJI, top);
    }

    public Cursor getFavoritesByType(int type) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor result = db.query(FAVORITES_TABLE_NAME, FAVORITES_ALL_COLUMNS,
                "is_kanji = ?", new String[] { Integer.toString(type) }, null,
                null, "time desc");

        return result;
    }

    public static SearchCriteria createCriteria(Cursor cursor) {
        int idx = cursor.getColumnIndex(HISTORY_SEARCH_TYPE);
        int type = cursor.getInt(idx);
        idx = cursor.getColumnIndex(HISTORY_QUERY_STRING);
        String queryString = cursor.getString(idx);
        idx = cursor.getColumnIndex(ID);
        long id = cursor.getLong(idx);

        switch (type) {
        case SearchCriteria.CRITERIA_TYPE_KANJI:
            idx = cursor.getColumnIndex(HISTORY_KANJI_SEARCH_TYPE);
            String searchType = cursor.getString(idx);
            int minStrokexIdx = cursor.getColumnIndex(HISTORY_MIN_STROKE_COUNT);
            int maxStrokesIdx = cursor.getColumnIndex(HISTORY_MAX_STROKE_COUNT);
            if (cursor.isNull(minStrokexIdx) && cursor.isNull(maxStrokesIdx)) {
                SearchCriteria result = SearchCriteria.createForKanji(
                        queryString, searchType);
                result.setId(id);

                return result;
            }

            Integer minStrokeCount = null;
            if (!cursor.isNull(minStrokexIdx)) {
                minStrokeCount = cursor.getInt(minStrokexIdx);
            }
            Integer maxStrokeCount = null;
            if (!cursor.isNull(maxStrokesIdx)) {
                maxStrokeCount = cursor.getInt(maxStrokesIdx);
            }

            SearchCriteria result = SearchCriteria.createWithStrokeCount(
                    queryString, searchType, minStrokeCount, maxStrokeCount);
            result.setId(id);

            return result;
        case SearchCriteria.CRITERIA_TYPE_DICT:
            boolean isExactMatch = cursor.getInt(cursor
                    .getColumnIndex(HISTORY_IS_EXACT_MATCH)) == 1;
            boolean isRomanized = cursor.getInt(cursor
                    .getColumnIndex(HISTORY_IS_ROMANIZED_JAPANESE)) == 1;
            boolean isCommonWordsOnly = cursor.getInt(cursor
                    .getColumnIndex(HISTORY_IS_COMMON_WORDS_ONLY)) == 1;
            String dictionary = cursor.getString(cursor
                    .getColumnIndex(HISTORY_DICTIONARY));

            result = SearchCriteria.createForDictionary(queryString,
                    isExactMatch, isRomanized, isCommonWordsOnly, dictionary);
            result.setId(id);

            return result;
        case SearchCriteria.CRITERIA_TYPE_EXAMPLES:
            int numMaxResults = cursor.getInt(cursor
                    .getColumnIndex(HISTORY_MAX_RESULTS));
            isExactMatch = cursor.getInt(cursor
                    .getColumnIndex(HISTORY_IS_EXACT_MATCH)) == 1;
            result = SearchCriteria.createForExampleSearch(queryString,
                    isExactMatch, numMaxResults);
            result.setId(id);

            return result;
        default:
            // should never happen...
            throw new IllegalStateException("Unknown criteria type " + type);
        }

    }

    public static WwwjdicEntry createWwwjdicEntry(Cursor cursor) {
        int idx = cursor.getColumnIndex(FAVORITES_IS_KANJI);
        boolean isKanji = cursor.getInt(idx) == SearchCriteria.CRITERIA_TYPE_KANJI;
        idx = cursor.getColumnIndex(FAVORITES_DICT_STR);
        String dictStr = cursor.getString(idx);
        idx = cursor.getColumnIndex(ID);
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

    public synchronized void deleteHistoryItem(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(HISTORY_TABLE_NAME, "_id = " + id, null);
    }

    public synchronized void deleteFavorite(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(FAVORITES_TABLE_NAME, "_id = " + id, null);
    }

    public synchronized void deleteAllHistory() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(HISTORY_TABLE_NAME, null, null);
    }

    public synchronized void deleteAllFavorites() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(FAVORITES_TABLE_NAME, null, null);
    }

    public Long getFavoriteId(String headword) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(FAVORITES_TABLE_NAME, new String[] { ID },
                    "headword = ?", new String[] { headword }, null, null,
                    "time desc");
            int count = cursor.getCount();
            if (count == 0) {
                return null;
            }

            int idx = cursor.getColumnIndex(ID);
            cursor.moveToFirst();

            return cursor.getLong(idx);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void beginTransaction() {
        getWritableDatabase().beginTransaction();
    }

    public void setTransactionSuccessful() {
        getWritableDatabase().setTransactionSuccessful();
    }

    public void endTransaction() {
        getWritableDatabase().endTransaction();
    }
}
