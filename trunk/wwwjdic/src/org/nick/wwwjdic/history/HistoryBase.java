package org.nick.wwwjdic.history;

import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.DictionaryResultListView;
import org.nick.wwwjdic.KanjiResultListView;
import org.nick.wwwjdic.SearchCriteria;
import org.nick.wwwjdic.history.HistoryItem.FavoriteStatusChangedListener;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;

public abstract class HistoryBase extends ListActivity implements
        FavoriteStatusChangedListener {

    private static final String TAG = HistoryBase.class.getSimpleName();

    private static final int MENU_ITEM_DELETE_ALL = 0;
    private static final int MENU_ITEM_LOOKUP = 1;
    private static final int MENU_ITEM_DELETE = 2;

    protected HistoryDbHelper db;

    protected HistoryBase() {
        db = new HistoryDbHelper(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getContentView());
        getListView().setOnCreateContextMenuListener(this);

        setupAdapter();
    }

    protected abstract int getContentView();

    protected abstract void setupAdapter();

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        lookupCurrentItem();
    }

    private void lookupCurrentItem() {
        Cursor c = getCursor();

        SearchCriteria criteria = HistoryDbHelper.createCriteria(c);

        Intent intent = null;
        if (criteria.isKanjiLookup()) {
            intent = new Intent(this, KanjiResultListView.class);
        } else {
            intent = new Intent(this, DictionaryResultListView.class);
        }
        intent.putExtra(Constants.CRITERIA_KEY, criteria);

        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_ITEM_DELETE_ALL, 0, "Delete All").setIcon(
                android.R.drawable.ic_menu_delete);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final boolean hasItems = getListAdapter().getCount() > 0;

        menu.getItem(0).setEnabled(hasItems);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_DELETE_ALL:
            deleteAll();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected abstract void deleteAll();

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            return;
        }

        menu.setHeaderTitle(cursor.getString(cursor
                .getColumnIndex("query_string")));

        menu.add(0, MENU_ITEM_LOOKUP, 0, "Look up");
        menu.add(0, MENU_ITEM_DELETE, 1, "Delete");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_LOOKUP:
            lookupCurrentItem();
            return true;
        case MENU_ITEM_DELETE: {
            deleteCurrentItem();
            return true;
        }
        }
        return false;
    }

    protected abstract void deleteCurrentItem();

    protected Cursor getCursor() {
        CursorAdapter adapter = (CursorAdapter) getListAdapter();
        Cursor c = adapter.getCursor();
        return c;
    }

    @Override
    public void onStatusChanged(boolean isFavorite, int id) {
        db.toggleFavorite(id, isFavorite);
    }

    public abstract void refresh();

}
