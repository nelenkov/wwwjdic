package org.nick.wwwjdic.history;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;

public class HistoryLoader extends AsyncTaskLoader<Cursor> {

    public static final int FILTER_ALL = -1;
    public static final int FILTER_DICT = 0;
    public static final int FILTER_KANJI = 1;
    public static final int FILTER_EXAMPLES = 2;

    private HistoryDbHelper db;
    private int selectedFilter = FILTER_ALL;

    private Cursor lastCursor;

    public HistoryLoader(Context context, HistoryDbHelper db) {
        super(context);
        this.db = db;
    }

    @Override
    public Cursor loadInBackground() {
        if (selectedFilter == FILTER_ALL) {
            lastCursor = db.getHistory();

        } else {
            lastCursor = db.getHistoryByType(selectedFilter);
        }

        return lastCursor;
    }

    @Override
    protected void onStartLoading() {
        if (lastCursor != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(lastCursor);
        }


        if (lastCursor == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    public int getSelectedFilter() {
        return selectedFilter;
    }

    public void setSelectedFilter(int selectedFilter) {
        this.selectedFilter = selectedFilter;
    }

}
