package org.nick.wwwjdic.history;

import android.content.Context;
import android.database.Cursor;

public class SearchHistoryLoader extends HistoryLoaderBase {

    public SearchHistoryLoader(Context context, HistoryDbHelper db) {
        super(context, db);
    }

    @Override
    public Cursor load() {
        if (selectedFilter == FILTER_ALL) {
            lastCursor = db.getHistory();

        } else {
            lastCursor = db.getHistoryByType(selectedFilter);
        }

        return lastCursor;
    }

}
