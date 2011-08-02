package org.nick.wwwjdic.history;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;

public abstract class HistoryLoaderBase extends AsyncTaskLoader<Cursor> {

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
    public void deliverResult(Cursor cursor) {
        if (isReset()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        Cursor oldCursor = lastCursor;
        lastCursor = cursor;

        if (isStarted()) {
            super.deliverResult(cursor);
        }

        if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed()) {
            oldCursor.close();
        }
    }

    @Override
    protected void onStartLoading() {
        if (lastCursor != null) {
            deliverResult(lastCursor);
        }

        if (takeContentChanged() || lastCursor == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void onCanceled(Cursor cursor) {
        super.onCanceled(cursor);

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();

        onStopLoading();

        if (lastCursor != null && !lastCursor.isClosed()) {
            lastCursor.close();
        }
        lastCursor = null;
    }


    public int getSelectedFilter() {
        return selectedFilter;
    }

    public void setSelectedFilter(int selectedFilter) {
        this.selectedFilter = selectedFilter;
    }
}
