package org.nick.wwwjdic.history;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

import org.nick.wwwjdic.R;
import org.nick.wwwjdic.Wwwjdic;
import org.nick.wwwjdic.utils.Analytics;
import org.nick.wwwjdic.utils.LoaderResult;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SupportActivity;
import android.support.v4.view.ActionMode;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.widget.CursorAdapter;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;

@SuppressWarnings("deprecation")
public abstract class HistoryFragmentBase extends ListFragment implements
        LoaderManager.LoaderCallbacks<LoaderResult<Cursor>>,
        OnItemLongClickListener {

    private static final String TAG = HistoryFragmentBase.class.getSimpleName();

    protected static final int CONFIRM_DELETE_DIALOG_ID = 0;

    public static final int FILTER_ALL = -1;
    public static final int FILTER_DICT = 0;
    public static final int FILTER_KANJI = 1;
    public static final int FILTER_EXAMPLES = 2;

    private static final byte[] UTF8_BOM = { (byte) 0xef, (byte) 0xbb,
            (byte) 0xbf };

    private static final boolean IS_HONEYCOMB = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

    protected HistoryDbHelper db;

    protected ClipboardManager clipboardManager;

    protected int selectedFilter = -1;

    private ActionMode currentActionMode;

    protected HistoryFragmentBase() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = HistoryDbHelper.getInstance(getActivity());

        clipboardManager = (ClipboardManager) getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE);

        getListView().setOnCreateContextMenuListener(this);

        if (getArguments() != null && savedInstanceState == null) {
            selectedFilter = getArguments().getInt(
                    FavoritesAndHistory.EXTRA_FILTER_TYPE, FILTER_ALL);
        }

        if (savedInstanceState != null) {
            selectedFilter = savedInstanceState.getInt(
                    FavoritesAndHistory.EXTRA_FILTER_TYPE, FILTER_ALL);
        }

        setupAdapter();

        getListView().setOnItemLongClickListener(this);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(getContentView(), container, false);

        return v;
    }

    @Override
    public void onAttach(SupportActivity activity) {
        super.onAttach(activity);
        // XXX -- uglish, but calling setHasOptionsMenu() any later 
        // than this may result in menu shown when restored (e.g., on rotate)
        // Is there a better way?
        // hasOptionsMenu has to be false by default, because we don't 
        // want it when shown on the main screen (Wwwjdic.java)
        if (activity.asActivity() instanceof FavoritesAndHistory) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(FavoritesAndHistory.EXTRA_FILTER_TYPE, selectedFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();

        Analytics.startSession(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();

        Analytics.endSession(getActivity());
    }

    protected abstract int getContentView();

    protected abstract void setupAdapter();

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        getListView().setItemChecked(position, false);
        lookup(position);
    }

    protected abstract void lookup(int position);

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.history_favorites, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        ListAdapter adapter = getListAdapter();
        final boolean hasItems = adapter == null ? false
                : adapter.getCount() > 0;
        File backupFile = new File(getImportExportFilename());

        menu.findItem(R.id.menu_import).setEnabled(backupFile.exists());
        menu.findItem(R.id.menu_export).setEnabled(hasItems);
        menu.findItem(R.id.menu_delete).setEnabled(hasItems);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(getActivity(), Wwwjdic.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_import) {
            importItems();
            getSupportActivity().invalidateOptionsMenu();
        } else if (item.getItemId() == R.id.menu_export) {
            exportItems();
            getSupportActivity().invalidateOptionsMenu();
        } else if (item.getItemId() == R.id.menu_filter) {
            showFilterDialog();
        } else if (item.getItemId() == R.id.menu_delete) {
            DialogFragment confirmDeleteDialog = ConfirmDeleteDialog
                    .newInstance(this);
            confirmDeleteDialog.show(getFragmentManager(),
                    "confirmDeleteDialog");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showFilterDialog() {
        new DialogFragment() {

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        getActivity());
                builder.setTitle(R.string.select_filter_type);
                builder.setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                dialog.dismiss();

                            }
                        });
                builder.setSingleChoiceItems(getFilterTypes(),
                        selectedFilter + 1,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                selectedFilter = item - 1;
                                filter();
                                getSupportActivity().invalidateOptionsMenu();
                                dialog.dismiss();
                            }
                        });

                return builder.create();
            }
        }.show(getFragmentManager(), "filterDialog");
    }

    protected abstract String[] getFilterTypes();

    protected void importItems() {
        String importFile = getImportExportFilename();

        confirmOverwriteAndImport(importFile);

        showAll();
    }

    private void confirmOverwriteAndImport(final String filename) {
        if (getListAdapter().isEmpty()) {
            doImport(filename);
            return;
        }

        new DialogFragment() {

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        getActivity());
                builder.setMessage(R.string.import_and_overwrite)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int id) {
                                        doImport(filename);
                                    }
                                })
                        .setNegativeButton(R.string.no,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int id) {
                                        dialog.cancel();
                                    }
                                });
                return builder.create();
            }
        }.show(getFragmentManager(), "overwriteImportDialog");
    }

    protected abstract void doImport(String filename);

    protected void exportItems() {
        createWwwjdicDirIfNecessary();

        String exportFile = getImportExportFilename();

        confirmOverwriteAndExport(exportFile);
    }

    protected abstract String getImportExportFilename();

    protected void confirmOverwriteAndExport(final String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            doExport(filename);
            return;
        }

        new DialogFragment() {

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        getActivity());
                String message = getResources().getString(
                        R.string.overwrite_file);
                builder.setMessage(String.format(message, filename))
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int id) {
                                        doExport(filename);
                                    }
                                })
                        .setNegativeButton(R.string.no,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int id) {
                                        dialog.cancel();
                                    }
                                });
                return builder.create();
            }
        }.show(getFragmentManager(), "overwriteExportDialog");
    }

    protected abstract void doExport(String filename);

    public void filter() {
        if (isDetached() || getActivity() == null) {
            return;
        }

        getLoaderManager().restartLoader(0, null, this);
    }

    protected abstract Cursor filterCursor();

    protected void showAll() {
        selectedFilter = FILTER_ALL;
        filter();
    }

    protected abstract void deleteAll();

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        if (IS_HONEYCOMB) {
            // we use action modes on HC
            return;
        }

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

        MenuInflater inflater = getSupportActivity().getMenuInflater();
        inflater.inflate(R.menu.history_favorites_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        int position = info.position;
        if (item.getItemId() == R.id.menu_context_history_lookup) {
            lookup(position);
            return true;
        } else if (item.getItemId() == R.id.menu_context_history_copy) {
            copy(position);
            return true;
        } else if (item.getItemId() == R.id.menu_context_history_delete) {
            delete(position);
            return true;
        }

        return false;
    }

    protected abstract void copy(int position);

    protected abstract void delete(int position);

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

    static class ConfirmDeleteDialog extends DialogFragment {

        private HistoryFragmentBase historyFragment;

        ConfirmDeleteDialog(HistoryFragmentBase historyFragment) {
            this.historyFragment = historyFragment;
        }

        public static ConfirmDeleteDialog newInstance(
                HistoryFragmentBase historyFragment) {
            return new ConfirmDeleteDialog(historyFragment);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return createConfirmDeleteDialog();
        }

        private Dialog createConfirmDeleteDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.delete_all_iteims)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    historyFragment.deleteAll();
                                    historyFragment.getSupportActivity()
                                            .invalidateOptionsMenu();
                                }
                            })
                    .setNegativeButton(R.string.no,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    dialog.cancel();
                                }
                            });
            AlertDialog dialog = builder.create();

            return dialog;
        }
    }

    public void refresh() {
        if (!isDetached() && getActivity() != null) {
            getLoaderManager().restartLoader(0, null, this);
        }
    }

    protected void createWwwjdicDirIfNecessary() {
        File sdDir = Environment.getExternalStorageDirectory();
        File wwwjdicDir = new File(sdDir.getAbsolutePath() + "/wwwjdic");
        if (!wwwjdicDir.exists()) {
            wwwjdicDir.mkdir();
        }

        if (!wwwjdicDir.canWrite()) {
            return;
        }
    }

    protected CSVReader openImportFile(String importFile)
            throws FileNotFoundException {
        File file = new File(importFile);
        if (!file.exists()) {
            String message = getResources().getString(R.string.file_not_found);
            Toast.makeText(getActivity(), String.format(message, importFile),
                    Toast.LENGTH_SHORT).show();

            return null;
        }

        return new CSVReader(new FileReader(importFile));
    }

    protected void writeBom(File exportFile) throws FileNotFoundException,
            IOException {
        OutputStream out = new FileOutputStream(exportFile);
        out.write(UTF8_BOM);
        out.flush();
        out.close();
    }

    protected void showCopiedToast(String headword) {
        String messageTemplate = getResources().getString(
                R.string.copied_to_clipboard);
        Toast t = Toast.makeText(getActivity(),
                String.format(messageTemplate, headword), Toast.LENGTH_SHORT);
        t.show();
    }

    public int getSelectedFilter() {
        return selectedFilter;
    }

    public void setSelectedFilter(int selectedFilter) {
        this.selectedFilter = selectedFilter;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        if (!IS_HONEYCOMB) {
            // not supported on pre-HC
            return false;
        }

        if (currentActionMode != null) {
            return false;
        }

        getListView().setItemChecked(position, true);
        currentActionMode = getSupportActivity().startActionMode(
                new ContextCallback(position));

        return true;
    }

    class ContextCallback implements ActionMode.Callback {

        private int position;

        ContextCallback(int position) {
            this.position = position;
        }

        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.history_favorites_context, menu);
            return true;
        }

        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        public boolean onActionItemClicked(ActionMode actionMode,
                MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.menu_context_history_lookup) {
                lookup(position);
                actionMode.finish();
                return true;
            } else if (menuItem.getItemId() == R.id.menu_context_history_copy) {
                copy(position);
                actionMode.finish();
                return true;
            } else if (menuItem.getItemId() == R.id.menu_context_history_delete) {
                delete(position);
                actionMode.finish();
                return true;
            }
            return false;
        }

        public void onDestroyActionMode(ActionMode actionMode) {
            getListView().setItemChecked(position, false);
            currentActionMode = null;
        }
    };

}
