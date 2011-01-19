package org.nick.wwwjdic.history;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.DictionaryEntry;
import org.nick.wwwjdic.DictionaryEntryDetail;
import org.nick.wwwjdic.KanjiEntry;
import org.nick.wwwjdic.KanjiEntryDetail;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicApplication;
import org.nick.wwwjdic.WwwjdicEntry;
import org.nick.wwwjdic.WwwjdicPreferences;
import org.nick.wwwjdic.history.FavoritesItem.FavoriteStatusChangedListener;
import org.nick.wwwjdic.history.gdocs.DocsUrl;
import org.nick.wwwjdic.history.gdocs.Namespace;
import org.nick.wwwjdic.utils.Analytics;
import org.nick.wwwjdic.utils.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.google.api.client.apache.ApacheHttpTransport;
import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.GoogleTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.xml.atom.AtomParser;

public class Favorites extends HistoryBase implements
        FavoriteStatusChangedListener {

    private static final String TAG = Favorites.class.getSimpleName();

    private static final String EXPORT_FILENAME = "wwwjdic/favorites.csv";

    private static final String KANJI_CSV_EXPORT_FILENAME_BASE = "wwwjdic-favorites-kanji";
    private static final String DICT_CSV_EXPORT_FILENAME_BASE = "wwwjdic-favorites-dict";
    private static final String CSV_EXPORT_FILENAME_EXT = "csv";

    // for google docs
    private static final String AUTH_TOKEN_TYPE = "writely";
    private static final String GDATA_VERSION = "3";

    private static final int REQUEST_AUTHENTICATE = 0;

    private static final int ACCOUNTS_DIALOG_ID = 1;

    private static final int EXPORT_LOCAL_BACKUP_IDX = 0;
    private static final int EXPORT_LOCAL_EXPORT_IDX = 1;
    private static final int EXPORT_GDOCS_IDX = 2;
    private static final int EXPORT_ANKI_IDX = 3;

    private static final String FAVORITES_EXPORT_TIP_DIALOG = "tips_favorites_export";

    private static final int ECLAIR_VERSION_CODE = 5;

    private HttpTransport transport;

    private String authToken;

    private ProgressDialog progressDialog;

    public Favorites() {
        if (isPostEclair()) {
            initGdocsTransport();
        }
    }

    private static boolean isPostEclair() {
        final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);

        return sdkVersion >= ECLAIR_VERSION_CODE;
    }

    private void initGdocsTransport() {
        HttpTransport.setLowLevelHttpTransport(ApacheHttpTransport.INSTANCE);
        transport = GoogleTransport.create();
        GoogleHeaders headers = (GoogleHeaders) transport.defaultHeaders;
        headers.setApplicationName(WwwjdicApplication.getUserAgentString());
        headers.gdataVersion = GDATA_VERSION;
        AtomParser parser = new AtomParser();
        parser.namespaceDictionary = Namespace.DICTIONARY;
        transport.addParser(parser);

        // for wire debugging
        // Logger.getLogger("com.google.api.client").setLevel(Level.ALL);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Dialogs.showTipOnce(this, FAVORITES_EXPORT_TIP_DIALOG,
                R.string.tips_favorites_export);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case CONFIRM_DELETE_DIALOG_ID:
            return super.onCreateDialog(id);
        case ACCOUNTS_DIALOG_ID:
            final AccountManagerWrapper manager = AccountManagerWrapper
                    .getInstance(this);
            final String[] accountNames = manager.getGoogleAccounts();
            int size = accountNames.length;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if (size == 0) {
                Toast t = Toast.makeText(this, R.string.no_google_accounts,
                        Toast.LENGTH_LONG);
                t.show();

                return null;
            }

            builder.setTitle(R.string.select_google_account);
            builder.setItems(accountNames,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            gotAccount(manager, accountNames[which]);
                        }
                    });
            return builder.create();
        default:
            // do nothing
        }
        return null;
    }

    private void gotAccount(boolean tokenExpired) {
        String accountName = WwwjdicPreferences.getGoogleAcountName(this);
        AccountManagerWrapper manager = AccountManagerWrapper.getInstance(this);
        String[] accountNames = manager.getGoogleAccounts();
        if (accountName != null) {
            int size = accountNames.length;
            for (int i = 0; i < size; i++) {
                if (accountName.equals(accountNames[i])) {
                    if (tokenExpired) {
                        manager.invalidateAuthToken(authToken);
                    }
                    gotAccount(manager, accountNames[i]);
                    return;
                }
            }
        }

        // handle this here to avoid IAE on 2.0
        // ('Activity#onCreateDialog did not create a dialog for id 1')
        if (accountNames.length != 0) {
            showDialog(ACCOUNTS_DIALOG_ID);
        } else {
            // clean up temporary file
            if (uploadData != null) {
                if (uploadData.localFilename != null) {
                    File f = new File(uploadData.localFilename);
                    f.delete();
                }
                uploadData = null;
            }

            Log.w(TAG, "No suitable Google accounts found");
            Toast t = Toast.makeText(this, R.string.no_google_accounts,
                    Toast.LENGTH_LONG);
            t.show();
        }
    }

    private void gotAccount(final AccountManagerWrapper manager,
            final String accountName) {
        WwwjdicPreferences.setGoogleAccountName(this, accountName);

        new Thread() {

            @Override
            public void run() {
                try {
                    final Bundle bundle = manager.getAuthToken(accountName,
                            AUTH_TOKEN_TYPE);
                    runOnUiThread(new Runnable() {

                        public void run() {
                            try {
                                if (bundle
                                        .containsKey(AccountManagerWrapper.KEY_INTENT)) {
                                    Intent intent = bundle
                                            .getParcelable(AccountManagerWrapper.KEY_INTENT);
                                    int flags = intent.getFlags();
                                    flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
                                    intent.setFlags(flags);
                                    startActivityForResult(intent,
                                            REQUEST_AUTHENTICATE);
                                } else if (bundle
                                        .containsKey(AccountManagerWrapper.KEY_AUTHTOKEN)) {
                                    authenticatedClientLogin(bundle
                                            .getString(AccountManagerWrapper.KEY_AUTHTOKEN));
                                }
                            } catch (Exception e) {
                                handleException(e);
                            }
                        }
                    });
                } catch (Exception e) {
                    handleException(e);
                }
            }
        }.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case REQUEST_AUTHENTICATE:
            if (resultCode == RESULT_OK) {
                gotAccount(false);
            } else {
                showDialog(ACCOUNTS_DIALOG_ID);
            }
            break;
        }
    }

    private void authenticatedClientLogin(String authToken) {
        this.authToken = authToken;
        ((GoogleHeaders) transport.defaultHeaders).setGoogleLogin(authToken);
        authenticated();
    }

    static class UploadData {
        String filename;
        String localFilename;
        String contentType;
        long contentLength;

        UploadData() {
        }
    }

    private UploadData uploadData;

    private class GDocsExportTask extends AsyncTask<Void, Object, Boolean> {

        private Throwable error;
        private boolean tokenExpired = false;

        GDocsExportTask() {
        }

        @Override
        protected void onPreExecute() {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();

            }
            progressDialog = new ProgressDialog(Favorites.this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage(getString(R.string.uploading_to_gdocs));
            progressDialog.setCancelable(true);
            progressDialog.setButton(ProgressDialog.BUTTON_NEUTRAL,
                    getString(R.string.cancel), new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cancel(true);
                        }
                    });
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                HttpRequest request = transport.buildPostRequest();
                request.url = DocsUrl.forDefaultPrivateFull();
                ((GoogleHeaders) request.headers)
                        .setSlugFromFileName(uploadData.filename);
                InputStreamContent content = new InputStreamContent();
                content.inputStream = new FileInputStream(
                        uploadData.localFilename);
                content.type = uploadData.contentType;
                content.length = uploadData.contentLength;
                request.content = content;
                request.execute().ignore();

                deleteTempFile();

                Analytics.event("favoritesGDocsExport", Favorites.this);

                return true;
            } catch (HttpResponseException hre) {
                Log.d(TAG, "Error uploading to Google docs", hre);

                HttpResponse response = hre.response;
                int statusCode = response.statusCode;
                try {
                    response.ignore();
                    Log.e(TAG, response.parseAsString());
                } catch (IOException e) {
                    Log.w(TAG, "error parsing response", e);
                }

                if (statusCode == 401 || statusCode == 403) {
                    tokenExpired = true;
                }

                return false;
            } catch (IOException e) {
                error = e;
                Log.d(TAG, "Error uploading to Google docs", e);
                deleteTempFile();

                return false;
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            deleteTempFile();
        }

        private void deleteTempFile() {
            Log.d(TAG, "deleting temp files...");
            File f = new File(uploadData.localFilename);
            boolean success = f.delete();
            if (success) {
                Log.d(TAG, "successfully deleted " + f.getAbsolutePath());
            } else {
                Log.d(TAG, "failed to delet " + f.getAbsolutePath());
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if (tokenExpired) {
                gotAccount(true);

                return;
            }

            Resources r = getResources();
            String template = result ? r
                    .getString(R.string.gdocs_upload_success) : r
                    .getString(R.string.gdocs_upload_failure);
            String message = result ? String.format(template,
                    uploadData.filename) : String.format(template,
                    error.getMessage());
            Toast t = Toast
                    .makeText(Favorites.this, message, Toast.LENGTH_LONG);
            uploadData = null;
            t.show();
        }
    }

    private void authenticated() {
        if (uploadData != null && uploadData.filename != null) {
            GDocsExportTask task = new GDocsExportTask();
            task.execute(new Void[] {});
        }
    }

    private void handleException(Exception e) {
        Log.e(TAG, e.getMessage(), e);

        if (e instanceof HttpResponseException) {
            HttpResponse response = ((HttpResponseException) e).response;
            int statusCode = response.statusCode;
            try {
                response.ignore();
            } catch (IOException e1) {
                Log.e(TAG, e.getMessage(), e1);
            }
            if (statusCode == 401 || statusCode == 403) {
                gotAccount(true);
                return;
            }

            try {
                Log.e(TAG, response.parseAsString());
            } catch (IOException parseException) {
                Log.w(TAG, e.getMessage(), parseException);
            }
        }
    }

    protected void setupAdapter() {
        MatrixCursor cursor = new MatrixCursor(
                HistoryDbHelper.FAVORITES_ALL_COLUMNS, 0);
        startManagingCursor(cursor);
        FavoritesAdapter adapter = new FavoritesAdapter(Favorites.this, cursor,
                Favorites.this);
        setListAdapter(adapter);

        new AsyncTask<Void, Void, Cursor>() {
            @Override
            protected void onPreExecute() {
                getParent().setProgressBarIndeterminateVisibility(true);
            }

            @Override
            protected Cursor doInBackground(Void... arg0) {
                return filterCursor();
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                resetAdapter(cursor);
                getParent().setProgressBarIndeterminateVisibility(false);
            }
        }.execute();
    }

    protected void resetAdapter(Cursor cursor) {
        startManagingCursor(cursor);
        FavoritesAdapter adapter = new FavoritesAdapter(Favorites.this, cursor,
                Favorites.this);
        setListAdapter(adapter);
    }

    @Override
    protected void deleteAll() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                getParent().setProgressBarIndeterminateVisibility(true);
            }

            @Override
            protected Void doInBackground(Void... arg0) {
                Cursor c = filterCursor();

                db.beginTransaction();
                try {
                    while (c.moveToNext()) {
                        int id = c.getInt(c.getColumnIndex("_id"));
                        db.deleteFavorite(id);
                    }
                    db.setTransactionSuccessful();

                    return null;
                } finally {
                    db.endTransaction();
                }
            }

            @Override
            protected void onPostExecute(Void v) {
                refresh();
                getParent().setProgressBarIndeterminateVisibility(false);
            }
        }.execute();
    }

    @Override
    protected int getContentView() {
        return R.layout.favorites;
    }

    @Override
    protected void deleteCurrentItem() {
        Cursor c = getCursor();
        int idx = c.getColumnIndex("_id");
        int id = c.getInt(idx);
        db.deleteFavorite(id);

        refresh();
    }

    @Override
    public void onStatusChanged(boolean isFavorite, WwwjdicEntry entry) {
        if (isFavorite) {
            db.addFavorite(entry);
            refresh();
        } else {
            db.deleteFavorite(entry.getId());
        }
    }

    @Override
    protected void lookupCurrentItem() {
        WwwjdicEntry entry = getCurrentEntry();

        Intent intent = null;
        if (entry.isKanji()) {
            intent = new Intent(this, KanjiEntryDetail.class);
            intent.putExtra(Constants.KANJI_ENTRY_KEY, entry);
            intent.putExtra(Constants.IS_FAVORITE, true);
        } else {
            intent = new Intent(this, DictionaryEntryDetail.class);
            intent.putExtra(Constants.ENTRY_KEY, entry);
            intent.putExtra(Constants.IS_FAVORITE, true);
        }

        Analytics.event("lookupFromFavorites", this);

        startActivity(intent);
    }

    @Override
    protected void copyCurrentItem() {
        WwwjdicEntry entry = getCurrentEntry();
        clipboardManager.setText(entry.getHeadword());
    }

    private WwwjdicEntry getCurrentEntry() {
        Cursor c = getCursor();
        WwwjdicEntry entry = HistoryDbHelper.createWwwjdicEntry(c);
        return entry;
    }

    @Override
    protected String getImportExportFilename() {
        File extStorage = Environment.getExternalStorageDirectory();

        return extStorage.getAbsolutePath() + "/" + EXPORT_FILENAME;
    }

    @Override
    protected void exportItems() {
        String[] items = getResources().getStringArray(
                R.array.favorites_export_dialog_items);
        boolean singleType = selectedFilter != FILTER_ALL;
        final boolean isKanji = selectedFilter == FILTER_KANJI;
        ExportItemsAdapter adapter = new ExportItemsAdapter(this, items,
                singleType);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.favorites_export_dialog_title);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (item) {
                case EXPORT_LOCAL_BACKUP_IDX:
                    Favorites.super.exportItems();
                    break;
                case EXPORT_LOCAL_EXPORT_IDX:
                    exportLocalCsv(isKanji);
                    break;
                case EXPORT_GDOCS_IDX:
                    exportToGDocs(isKanji);
                    break;
                case EXPORT_ANKI_IDX:
                    exportToAnkiDeckAsync(isKanji);
                    break;
                default:
                    // do noting
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void exportToAnkiDeckAsync(boolean isKanji) {
        AnkiExportTask task = new AnkiExportTask();
        task.execute(isKanji);
    }

    private class AnkiExportTask extends AsyncTask<Boolean, Object, Boolean> {

        private Throwable error;
        private String exportFilename;

        AnkiExportTask() {
        }

        @Override
        protected void onPreExecute() {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();

            }
            progressDialog = new ProgressDialog(Favorites.this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage(getString(R.string.exporting_to_anki));
            progressDialog.setCancelable(true);
            progressDialog.setButton(ProgressDialog.BUTTON_NEUTRAL,
                    getString(R.string.cancel), new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cancel(true);
                        }
                    });
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            try {
                boolean isKanji = params[0];
                exportFilename = exportToAnkiDeck(isKanji);

                return true;
            } catch (Exception e) {
                error = e;
                Log.d(TAG, "Error exporting favorites to Anki", e);
                deleteIncompleteFile();

                return false;
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            deleteIncompleteFile();
        }

        private void deleteIncompleteFile() {
            Log.d(TAG, "Anki export cancelled, deleting incomplete files...");
            if (exportFilename == null) {
                return;
            }
            File f = new File(exportFilename);
            boolean success = f.delete();
            if (success) {
                Log.d(TAG, "successfully deleted " + f.getAbsolutePath());
            } else {
                Log.d(TAG, "failed to delet " + f.getAbsolutePath());
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            Resources r = getResources();
            String template = result ? r
                    .getString(R.string.anki_export_success) : r
                    .getString(R.string.anki_export_failure);
            String message = result ? String.format(template, exportFilename)
                    : String.format(template, error.getMessage());
            Toast t = Toast
                    .makeText(Favorites.this, message, Toast.LENGTH_LONG);
            t.show();
        }
    }

    private String exportToAnkiDeck(boolean isKanji) {
        AnkiGenerator generator = new AnkiGenerator(Favorites.this);
        String filename = getCsvExportFilename(isKanji).replace(".csv", "")
                + ".anki";
        File exportFile = new File(WwwjdicApplication.getWwwjdicDir(), filename);
        Log.d(TAG,
                "exporting favorites to Anki: " + exportFile.getAbsolutePath());

        int size = 0;
        if (isKanji) {
            List<KanjiEntry> kanjis = new ArrayList<KanjiEntry>();
            Cursor c = null;
            try {
                c = filterCursor();
                while (c.moveToNext()) {
                    WwwjdicEntry entry = HistoryDbHelper.createWwwjdicEntry(c);
                    kanjis.add((KanjiEntry) entry);
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            size = generator.createKanjiAnkiFile(exportFile.getAbsolutePath(),
                    kanjis);
        } else {
            List<DictionaryEntry> words = new ArrayList<DictionaryEntry>();
            Cursor c = null;
            try {
                c = filterCursor();
                while (c.moveToNext()) {
                    WwwjdicEntry entry = HistoryDbHelper.createWwwjdicEntry(c);
                    words.add((DictionaryEntry) entry);
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            size = generator.createDictAnkiFile(exportFile.getAbsolutePath(),
                    words);
        }

        Analytics.event("favoritesAnkiExport", this);
        Log.d(TAG,
                String.format("Exported %d entries to %s", size,
                        exportFile.getAbsolutePath()));

        return exportFile.getAbsolutePath();
    }

    private static class ExportItemsAdapter extends ArrayAdapter<String> {

        private boolean singleType;
        private boolean isPostEclair;

        ExportItemsAdapter(Context context, String[] items, boolean singleType) {
            super(context, android.R.layout.select_dialog_item,
                    android.R.id.text1, items);
            this.singleType = singleType;
            this.isPostEclair = isPostEclair();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int idx) {
            switch (idx) {
            case EXPORT_LOCAL_BACKUP_IDX:
                return true;
            case EXPORT_LOCAL_EXPORT_IDX:
                return singleType;
            case EXPORT_GDOCS_IDX:
                return singleType && isPostEclair;
            case EXPORT_ANKI_IDX:
                return singleType;
            default:
                return false;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View result = super.getView(position, convertView, parent);
            result.setEnabled(isEnabled(position));

            return result;
        }

    }

    private void exportLocalCsv(final boolean isKanji) {
        new AsyncTask<Void, Void, Boolean>() {
            Exception exception;
            int count = 0;
            String exportFilename;

            @Override
            protected void onPreExecute() {
                getParent().setProgressBarIndeterminateVisibility(true);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    File exportFile = new File(
                            WwwjdicApplication.getWwwjdicDir(),
                            getCsvExportFilename(isKanji));
                    writeBom(exportFile);

                    Writer writer = new FileWriter(exportFile, true);
                    exportFilename = exportFile.getAbsolutePath();
                    count = exportToCsv(exportFile.getAbsolutePath(), writer,
                            false);

                    Analytics.event("favoritesLocalCsvExport", Favorites.this);

                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "error exporting favorites", e);
                    exception = e;

                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    String message = getResources().getString(
                            R.string.favorites_exported);
                    Toast t = Toast.makeText(Favorites.this,
                            String.format(message, exportFilename, count),
                            Toast.LENGTH_SHORT);
                    t.show();
                } else {
                    String message = getResources().getString(
                            R.string.export_error);
                    String errMessage = exception == null ? "Error" : exception
                            .getMessage();
                    Toast.makeText(Favorites.this,
                            String.format(message, errMessage),
                            Toast.LENGTH_SHORT).show();
                }

                getParent().setProgressBarIndeterminateVisibility(false);
            }
        }.execute();
    }

    private int exportToCsv(String exportFile, Writer w, boolean showMessages) {
        CSVWriter writer = null;
        Cursor c = null;
        try {
            c = filterCursor();
            writer = new CSVWriter(w);

            boolean isKanji = selectedFilter == FILTER_KANJI;
            Resources r = getResources();
            String[] header = isKanji ? r
                    .getStringArray(R.array.kanji_csv_headers) : r
                    .getStringArray(R.array.dict_csv_headers);
            writer.writeNext(header);

            int count = 0;
            while (c.moveToNext()) {
                WwwjdicEntry entry = HistoryDbHelper.createWwwjdicEntry(c);
                String separatorChar = WwwjdicPreferences
                        .getMeaningsSeparatorCharacter(this);
                // single space not allowed in resources?
                if ("space".equals(separatorChar)) {
                    separatorChar = " ";
                }
                String[] entryStr = FavoritesEntryParser.toParsedStringArray(
                        entry, separatorChar);
                writer.writeNext(entryStr);
                count++;
            }

            writer.flush();
            writer.close();

            if (showMessages) {
                String message = getResources().getString(
                        R.string.favorites_exported);
                Toast t = Toast.makeText(Favorites.this,
                        String.format(message, exportFile, count),
                        Toast.LENGTH_SHORT);
                t.show();
            }

            return count;

        } catch (IOException e) {
            Log.d(TAG, "error exporting to CSV", e);
            if (showMessages) {
                String message = getResources()
                        .getString(R.string.export_error);
                Toast.makeText(Favorites.this,
                        String.format(message, e.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }

            return 0;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Log.w(TAG, "error closing CSV writer", e);
                }
            }
            if (c != null) {
                c.close();
            }
        }
    }

    private String getCsvExportFilename(boolean isKanji) {
        String dateStr = DateFormat.format("yyyyMMdd-kkmmss", new Date())
                .toString();
        return String.format("%s-%s.%s",
                isKanji ? KANJI_CSV_EXPORT_FILENAME_BASE
                        : DICT_CSV_EXPORT_FILENAME_BASE, dateStr,
                CSV_EXPORT_FILENAME_EXT);
    }

    private void exportToGDocs(final boolean isKanji) {
        AccountManagerWrapper manager = AccountManagerWrapper.getInstance(this);
        String[] accountNames = manager.getGoogleAccounts();
        if (accountNames.length == 0) {
            Log.w(TAG, "No suitable Google accounts found");
            Toast t = Toast.makeText(this, R.string.no_google_accounts,
                    Toast.LENGTH_LONG);
            t.show();
            return;
        }

        new AsyncTask<Void, Void, Boolean>() {
            Exception exception;

            @Override
            protected void onPreExecute() {
                getParent().setProgressBarIndeterminateVisibility(true);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    Log.d(TAG, "exporting to Google docs...");
                    String filename = getCsvExportFilename(isKanji);
                    File tempFile = File.createTempFile("favorites-gdocs",
                            ".csv", WwwjdicApplication.getWwwjdicDir());
                    tempFile.deleteOnExit();
                    Log.d(TAG, "temp file: " + tempFile.getAbsolutePath());
                    Log.d(TAG, "document filename: " + filename);

                    Writer writer = new FileWriter(tempFile, true);
                    exportToCsv(tempFile.getAbsolutePath(), writer, false);

                    uploadData = new UploadData();
                    uploadData.contentLength = tempFile.length();
                    uploadData.contentType = "text/csv";
                    uploadData.filename = filename;
                    uploadData.localFilename = tempFile.getAbsolutePath();

                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "error creating temporary favorites file", e);
                    exception = e;

                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    gotAccount(false);
                } else {
                    String message = getResources().getString(
                            R.string.export_error);
                    String errMessage = exception == null ? "Error" : exception
                            .getMessage();
                    Toast.makeText(Favorites.this,
                            String.format(message, errMessage),
                            Toast.LENGTH_SHORT).show();
                }
                getParent().setProgressBarIndeterminateVisibility(false);
            }
        }.execute();
    }

    @Override
    protected void doExport(final String exportFile) {
        new AsyncTask<Void, Void, Boolean>() {
            Exception exception;
            int count = 0;

            @Override
            protected void onPreExecute() {
                getParent().setProgressBarIndeterminateVisibility(true);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                CSVWriter writer = null;
                Cursor c = null;

                try {
                    c = filterCursor();
                    writer = new CSVWriter(new FileWriter(exportFile));

                    while (c.moveToNext()) {
                        WwwjdicEntry entry = HistoryDbHelper
                                .createWwwjdicEntry(c);
                        long time = c.getLong(c.getColumnIndex("time"));
                        String[] entryStr = FavoritesEntryParser.toStringArray(
                                entry, time);
                        writer.writeNext(entryStr);
                        count++;
                    }

                    writer.flush();
                    writer.close();

                    Analytics.event("favoritesExport", Favorites.this);

                    return true;
                } catch (IOException e) {
                    Log.e(TAG, "error exporting to file", e);
                    exception = e;

                    return false;
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            Log.w(TAG, "error closing CSV writer", e);
                        }
                    }
                    if (c != null) {
                        c.close();
                    }
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    String message = getResources().getString(
                            R.string.favorites_exported);
                    Toast t = Toast.makeText(Favorites.this,
                            String.format(message, exportFile, count),
                            Toast.LENGTH_SHORT);
                    t.show();
                } else {
                    String message = getResources().getString(
                            R.string.export_error);
                    String errMessage = exception == null ? "Error" : exception
                            .getMessage();
                    Toast.makeText(Favorites.this,
                            String.format(message, errMessage),
                            Toast.LENGTH_SHORT).show();
                }

                getParent().setProgressBarIndeterminateVisibility(false);
            }
        }.execute();
    }

    @Override
    protected void doImport(final String importFile) {
        new AsyncTask<Void, Void, Boolean>() {
            Exception exception;
            int count = 0;

            @Override
            protected void onPreExecute() {
                getParent().setProgressBarIndeterminateVisibility(true);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                CSVReader reader = null;

                db.beginTransaction();
                try {
                    db.deleteAllFavorites();

                    reader = new CSVReader(new FileReader(importFile));

                    String[] record = null;
                    while ((record = reader.readNext()) != null) {
                        WwwjdicEntry entry = FavoritesEntryParser
                                .fromStringArray(record);
                        long time = Long
                                .parseLong(record[FavoritesEntryParser.TIME_IDX]);
                        db.addFavorite(entry, time);
                        count++;
                    }
                    db.setTransactionSuccessful();

                    Analytics.event("favoritesImport", Favorites.this);

                    return true;
                } catch (IOException e) {
                    Log.e(TAG, "error importing favorites", e);
                    exception = e;

                    return false;
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            Log.w(TAG, "error closing CSV reader", e);
                        }
                    }
                    db.endTransaction();
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    String message = getResources().getString(
                            R.string.favorites_imported);
                    Toast t = Toast.makeText(Favorites.this,
                            String.format(message, importFile, count),
                            Toast.LENGTH_SHORT);
                    t.show();
                } else {
                    String message = getResources().getString(
                            R.string.import_error);
                    String errMessage = exception == null ? "Error" : exception
                            .getMessage();
                    Toast.makeText(Favorites.this,
                            String.format(message, errMessage),
                            Toast.LENGTH_SHORT).show();
                }

                refresh();

                getParent().setProgressBarIndeterminateVisibility(false);
            }
        }.execute();
    }

    @Override
    protected Cursor filterCursor() {
        if (selectedFilter == FILTER_ALL) {
            return db.getFavorites();

        }

        return db.getFavoritesByType(selectedFilter);
    }

    @Override
    protected String[] getFilterTypes() {
        return getResources().getStringArray(R.array.filter_types_favorites);
    }

}
