
package org.nick.wwwjdic.history;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicApplication;
import org.nick.wwwjdic.utils.FileUtils;
import org.nick.wwwjdic.utils.LoaderResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.BigTextStyle;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import au.com.bytecode.opencsv.CSVReader;

public abstract class HistoryFragmentBase extends ListFragment
        implements LoaderManager.LoaderCallbacks<LoaderResult<Cursor>>,
        OnItemLongClickListener {

    private static final String TAG = HistoryFragmentBase.class.getSimpleName();

    protected static final int CONFIRM_DELETE_DIALOG_ID = 0;

    protected static final int NOTIFICATION_ID_FAVORITES_EXPORT_BACKUP = 0;
    protected static final int NOTIFICATION_ID_FAVORITES_EXPORT_CSV = 1;
    protected static final int NOTIFICATION_ID_FAVORITES_EXPORT_ANKI = 2;
    protected static final int NOTIFICATION_ID_HISTORY_EXPORT = 3;

    public static final int FILTER_ALL = -1;
    public static final int FILTER_DICT = 0;
    public static final int FILTER_KANJI = 1;
    public static final int FILTER_EXAMPLES = 2;

    protected static final int REQUEST_OPEN_DOCUMENT = 42;

    private static final byte[] UTF8_BOM = {
            (byte) 0xef, (byte) 0xbb,
            (byte) 0xbf
    };

    protected HistoryDbHelper db;

    protected ClipboardManager clipboardManager;

    protected int selectedFilter = -1;

    private ActionMode currentActionMode;

    protected NotificationManager notificationManager;

    protected HistoryFragmentBase() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        super.onCreate(savedInstanceState);

        db = HistoryDbHelper.getInstance(getActivity());

        clipboardManager = (ClipboardManager) getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE);
        notificationManager = (NotificationManager) getActivity()
                .getSystemService(Context.NOTIFICATION_SERVICE);

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // XXX -- uglish, but calling setHasOptionsMenu() any later
        // than this may result in menu shown when restored (e.g., on rotate)
        // Is there a better way?
        // hasOptionsMenu has to be false by default, because we don't
        // want it when shown on the main screen (Wwwjdic.java)
        if (activity instanceof FavoritesAndHistory) {
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

    protected abstract int getContentView();

    protected abstract void setupAdapter();

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        getListView().setItemChecked(position, false);
        lookup(position);
    }

    protected abstract void lookup(int position);

    public static class FilterDialogFragment extends DialogFragment {

        private HistoryFragmentBase historyFragment;

        public FilterDialogFragment() {
        }

        public void setHistoryFragment(HistoryFragmentBase historyFragment) {
            this.historyFragment = historyFragment;
        }

        @Override
        public Dialog onCreateDialog(@NonNull Bundle savedInstanceState) {
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
            builder.setSingleChoiceItems(historyFragment.getFilterTypes(),
                    historyFragment.selectedFilter + 1,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            historyFragment.selectedFilter = item - 1;
                            historyFragment.filter();
                            getActivity().invalidateOptionsMenu();
                            dialog.dismiss();
                        }
                    });

            return builder.create();
        }
    }

    void showFilterDialog() {
        // XXX workaround for support library bug(?)
        if (getFragmentManager() == null) {
            return;
        }

        FilterDialogFragment fd = new FilterDialogFragment();
        fd.setHistoryFragment(this);
        fd.show(getFragmentManager(), "filterDialog");
    }

    protected abstract String[] getFilterTypes();

    protected void importItems() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent openIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            openIntent.addCategory(Intent.CATEGORY_OPENABLE);
            openIntent.setType("*/*");
            // Google drive doesn't seem to recognize CSV files
            openIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                    "*/*"
            });
            // hidden
            openIntent.putExtra("android.content.extra.SHOW_ADVANCED", true);
            startActivityForResult(openIntent, REQUEST_OPEN_DOCUMENT);
        }
        else {
            String importFile = getImportExportFilename();

            confirmOverwriteAndImport(importFile, false);

            showAll();
        }
    }

    void confirmOverwriteAndImport(final String filename, final boolean deleteAfterImport) {
        final File file = new File(filename);
        if (getListAdapter() == null || getListAdapter().isEmpty()) {
            doImport(new File(filename), deleteAfterImport);
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
                                        doImport(file, deleteAfterImport);
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

    protected abstract void doImport(File file, boolean delete);

    protected void exportItems() {
        createWwwjdicDirIfNecessary();

        String exportFile = getImportExportFilename();

        confirmOverwriteAndExport(exportFile);
    }

    protected abstract String getImportExportFilename();

    public static class  ConfirmOverriteDialog extends DialogFragment {

        private HistoryFragmentBase historyFragment;
        private String filename;

        public void setHistoryFragment(HistoryFragmentBase historyFragment) {
            this.historyFragment = historyFragment;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

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
                                    historyFragment.doExport(filename);
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
    }

    protected void confirmOverwriteAndExport(final String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            doExport(filename);
            return;
        }

        ConfirmOverriteDialog cod = new ConfirmOverriteDialog();
        cod.setHistoryFragment(this);
        cod.setFilename(filename);

        cod.show(getFragmentManager(), "overwriteExportDialog");
    }

    protected abstract void doExport(String filename);

    public void filter() {
        if (isDetached() || getActivity() == null) {
            return;
        }

       LoaderManager.getInstance(this).restartLoader(0, null, this);
    }

    protected abstract Cursor filterCursor();

    protected void showAll() {
        selectedFilter = FILTER_ALL;
        filter();
    }

    protected abstract void deleteAll();

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

    @SuppressLint("NewApi")
    public static class ConfirmDeleteDialog extends DialogFragment {

        private HistoryFragmentBase historyFragment;

        public ConfirmDeleteDialog() {
        }

        public static ConfirmDeleteDialog newInstance(
                HistoryFragmentBase historyFragment) {
            ConfirmDeleteDialog result = new ConfirmDeleteDialog();
            result.historyFragment = historyFragment;

            return result;
        }

        @Override
        public Dialog onCreateDialog(@NonNull Bundle savedInstanceState) {
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
                                    historyFragment.getActivity()
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
            LoaderManager.getInstance(this).restartLoader(0, null,this);
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

    protected CSVReader openImportFile(File importFile)
            throws FileNotFoundException {
        if (!importFile.exists()) {
            String message = getResources().getString(R.string.file_not_found);
            Toast.makeText(getActivity(), String.format(message, importFile),
                    Toast.LENGTH_SHORT).show();

            return null;
        }

        return new CSVReader(new FileReader(importFile));
    }

    protected void writeBom(File exportFile) throws IOException {
        OutputStream out = new FileOutputStream(exportFile);
        out.write(UTF8_BOM);
        out.flush();
        out.close();
    }

    protected void showCopiedToast(String headword) {
        String messageTemplate = getResources().getString(
                R.string.copied_to_clipboard);
        Toast.makeText(getActivity(), String.format(messageTemplate, headword),
                Toast.LENGTH_SHORT).show();
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
        if (currentActionMode != null) {
            return false;
        }

        currentActionMode = getActivity().startActionMode(
                new ContextCallback(position));
        getListView().setItemChecked(position, true);

        return true;
    }

    @SuppressLint("NewApi")
    class ContextCallback implements ActionMode.Callback {

        private int position;

        ContextCallback(int position) {
            this.position = position;
        }

        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = getActivity()
                    .getMenuInflater();
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
    }

    protected void notifyExportFinished(int notificationId, String message,
            String filename) {
        notifyExportFinished(notificationId, message, filename,
                "application/text", false);
    }

    protected void notifyExportFinished(int notificationId, String message,
            String filename, String mimeType, boolean open) {
        // clear progress notification
        notificationManager.cancel(notificationId);

        Context appCtx = WwwjdicApplication.getInstance();

        Intent intent = open ? createOpenIntent(filename, mimeType)
                : createShareFileIntent(filename, mimeType);
        // if (mimeType.contains("anki")) {
        // intent.addCategory("com.ankidroid.category.DECK");
        // }
        PendingIntent pendingIntent = PendingIntent.getActivity(appCtx, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String title = getResources().getString(R.string.app_name) + ": "
                + appCtx.getString(R.string.export_finished);

        Builder builder = new NotificationCompat.Builder(appCtx);
        builder.setSmallIcon(R.drawable.ic_stat_export);
        builder.setContentTitle(title);
        builder.setContentText(message);
        BigTextStyle style = new BigTextStyle(builder);
        style.bigText(message);
        style.setBigContentTitle(title);
        builder.setStyle(style);
        builder.setContentIntent(pendingIntent);
        builder.setWhen(System.currentTimeMillis());
        builder.setDefaults(Notification.DEFAULT_LIGHTS);
        builder.setAutoCancel(true);
        builder.setOngoing(false);
        if (open) {
            builder.addAction(android.R.drawable.ic_menu_view,
                    appCtx.getString(R.string.open), pendingIntent);
        } else {
            builder.addAction(android.R.drawable.ic_menu_share,
                    appCtx.getString(R.string.share), pendingIntent);
        }

        notificationManager.notify(notificationId, builder.build());
    }

    private Intent createOpenIntent(String filename, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType( getUriForfile(filename), mimeType);

        return Intent.createChooser(intent, getString(R.string.open));
    }

    private Intent createShareFileIntent(String filename, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        Uri uri = getUriForfile(filename);
        Log.d(TAG, "share URI: " + uri);
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        return Intent.createChooser(intent, (getString(R.string.share)));
    }

    private Uri getUriForfile(String filename) {
        File file = new File(filename);
        Uri uri;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            uri = Uri.fromFile(file);
        } else {
            String authority = getActivity().getApplicationContext().getPackageName() + ".fileprovider";
            uri = FileProvider.getUriForFile(getActivity(), authority, file);
        }
        return uri;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_OPEN_DOCUMENT) {
            Uri uri = data.getData();
            String filePath = uri.getPath();
            if (!isCsvFile(uri)) {
                Toast.makeText(getActivity(), getString(R.string.invalid_backup_file),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            File tempFile = null;
            if (!uri.getScheme().equalsIgnoreCase("file")) {
                try {
                    tempFile = copyToTempFile(uri);
                    filePath = tempFile.getAbsolutePath();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            confirmOverwriteAndImport(filePath, tempFile != null);

            showAll();
        }
    }

    private boolean isCsvFile(Uri uri) {
        Cursor cursor = getActivity().getContentResolver()
                .query(uri, null, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                String displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                return displayName.endsWith("csv") || displayName.endsWith("CSV");
            }
        } finally {
            cursor.close();
        }

        return false;
    }

    private File copyToTempFile(Uri uri) throws IOException {
        File tempFile = File.createTempFile("wwwjdic-import", ".csv");
        FileOutputStream out = new FileOutputStream(tempFile);
        byte[] zipBytes = FileUtils.readFromUri(getActivity(), uri);
        out.write(zipBytes);
        out.flush();
        out.close();

        return tempFile;
    }

}
