package org.nick.wwwjdic.history;

import org.nick.wwwjdic.utils.LoaderBase;
import org.nick.wwwjdic.utils.LoaderResult;

import android.content.Context;
import android.database.Cursor;

public abstract class HistoryLoaderBase extends LoaderBase<Cursor> {

    public static final int FILTER_ALL = -1;
    public static final int FILTER_DICT = 0;
    public static final int FILTER_KANJI = 1;
    public static final int FILTER_EXAMPLES = 2;

    protected HistoryDbHelper db;
    protected int selectedFilter = FILTER_ALL;

    protected Cursor lastCursor;

    protected HistoryLoaderBase(Context context, HistoryDbHelper db) {
        super(context);
        this.db = db;
    }

    @Override
    protected void releaseResult(LoaderResult<Cursor> result) {
        Cursor cursor = result.getData();
        if (cursor != null) {
            cursor.close();
        }
    }

    @Override
    protected boolean isActive(LoaderResult<Cursor> result) {
        Cursor cursor = result.getData();
        if (cursor == null) {
            return false;
        }

        return !cursor.isClosed();
    }

    public int getSelectedFilter() {
        return selectedFilter;
    }

    public void setSelectedFilter(int selectedFilter) {
        this.selectedFilter = selectedFilter;
    }
}
