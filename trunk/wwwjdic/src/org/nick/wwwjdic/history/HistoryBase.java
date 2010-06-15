package org.nick.wwwjdic.history;

import org.nick.wwwjdic.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;

public abstract class HistoryBase extends ListActivity {

    private static final String TAG = HistoryBase.class.getSimpleName();

    private static final int MENU_ITEM_DELETE_ALL = 0;
    private static final int MENU_ITEM_LOOKUP = 1;
    private static final int MENU_ITEM_COPY = 2;
    private static final int MENU_ITEM_DELETE = 3;

    private static final int CONFIRM_DELETE_DIALOG_ID = 0;

    protected HistoryDbHelper db;

    protected ClipboardManager clipboardManager;

    protected HistoryBase() {
        db = new HistoryDbHelper(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        setContentView(getContentView());
        getListView().setOnCreateContextMenuListener(this);

        setupAdapter();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        db.close();
    }

    protected abstract int getContentView();

    protected abstract void setupAdapter();

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        lookupCurrentItem();
    }

    protected abstract void lookupCurrentItem();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_ITEM_DELETE_ALL, 0, R.string.delete_all).setIcon(
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
            showDialog(CONFIRM_DELETE_DIALOG_ID);

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

        // menu.setHeaderTitle(cursor.getString(cursor
        // .getColumnIndex("query_string")));

        menu.add(0, MENU_ITEM_LOOKUP, 0, R.string.look_up);
        menu.add(0, MENU_ITEM_COPY, 1, R.string.copy);
        menu.add(0, MENU_ITEM_DELETE, 2, R.string.delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_LOOKUP:
            lookupCurrentItem();
            return true;
        case MENU_ITEM_COPY:
            copyCurrentItem();
            return true;
        case MENU_ITEM_DELETE: {
            deleteCurrentItem();
            return true;
        }
        }
        return false;
    }

    protected abstract void copyCurrentItem();

    protected abstract void deleteCurrentItem();

    protected Cursor getCursor() {
        CursorAdapter adapter = (CursorAdapter) getListAdapter();
        Cursor c = adapter.getCursor();
        return c;
    }

    @Override
    public void onResume() {
        super.onResume();

        refresh();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;

        switch (id) {
        case 0:
            dialog = createConfirmDeleteDialog();
            break;
        default:
            dialog = null;
        }

        return dialog;
    }

    private Dialog createConfirmDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_all_iteims).setCancelable(false)
                .setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                deleteAll();
                            }
                        }).setNegativeButton(R.string.no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog dialog = builder.create();

        return dialog;
    }

    protected void refresh() {
        Cursor cursor = getCursor();
        cursor.requery();
        CursorAdapter adapter = (CursorAdapter) getListAdapter();
        adapter.notifyDataSetChanged();
    }

}
