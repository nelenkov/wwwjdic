package org.nick.wwwjdic.history;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;

import org.nick.wwwjdic.Analytics;
import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.DictionaryEntryDetail;
import org.nick.wwwjdic.KanjiEntryDetail;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicApplication;
import org.nick.wwwjdic.WwwjdicEntry;
import org.nick.wwwjdic.history.FavoritesItem.FavoriteStatusChangedListener;
import org.nick.wwwjdic.history.gdocs.DocsUrl;
import org.nick.wwwjdic.history.gdocs.Namespace;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

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

    private static final String PREF_ACCOUNT_NAME_KEY = "pref_account_name";

    private static final int ACCOUNTS_DIALOG_ID = 1;

    private static final int EXPORT_LOCAL_BACKUP_IDX = 0;
    private static final int EXPORT_LOCAL_EXPORT_IDX = 1;
    private static final int EXPORT_GDOCS_IDX = 2;
    private static final int EXPORT_ANKI_IDX = 3;

    private HttpTransport transport;

    private String authToken;

    private ProgressDialog progressDialog;

    public Favorites() {
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
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case CONFIRM_DELETE_DIALOG_ID:
            return super.onCreateDialog(id);
        case ACCOUNTS_DIALOG_ID:
            final AccountManager manager = AccountManager.get(this);
            final Account[] accounts = manager.getAccountsByType("com.google");
            final int size = accounts.length;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if (size == 0) {
                Toast t = Toast.makeText(this, R.string.no_google_accounts,
                        Toast.LENGTH_LONG);
                t.show();

                return null;
            }
            builder.setTitle(R.string.select_google_account);

            String[] names = new String[size];
            for (int i = 0; i < size; i++) {
                names[i] = accounts[i].name;
            }
            builder.setItems(names, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    gotAccount(manager, accounts[which]);
                }
            });
            return builder.create();
        default:
            // do nothing
        }
        return null;
    }

    private void gotAccount(boolean tokenExpired) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(this);

        String accountName = settings.getString(PREF_ACCOUNT_NAME_KEY, null);
        if (accountName != null) {
            AccountManager manager = AccountManager.get(this);
            Account[] accounts = manager.getAccountsByType("com.google");
            int size = accounts.length;
            for (int i = 0; i < size; i++) {
                Account account = accounts[i];
                if (accountName.equals(account.name)) {
                    if (tokenExpired) {
                        manager.invalidateAuthToken("com.google",
                                this.authToken);
                    }
                    gotAccount(manager, account);
                    return;
                }
            }
        }
        showDialog(ACCOUNTS_DIALOG_ID);
    }

    private void gotAccount(final AccountManager manager, final Account account) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_ACCOUNT_NAME_KEY, account.name);
        editor.commit();
        new Thread() {

            @Override
            public void run() {
                try {
                    final Bundle bundle = manager.getAuthToken(account,
                            AUTH_TOKEN_TYPE, true, null, null).getResult();
                    runOnUiThread(new Runnable() {

                        public void run() {
                            try {
                                if (bundle
                                        .containsKey(AccountManager.KEY_INTENT)) {
                                    Intent intent = bundle
                                            .getParcelable(AccountManager.KEY_INTENT);
                                    int flags = intent.getFlags();
                                    flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
                                    intent.setFlags(flags);
                                    startActivityForResult(intent,
                                            REQUEST_AUTHENTICATE);
                                } else if (bundle
                                        .containsKey(AccountManager.KEY_AUTHTOKEN)) {
                                    authenticatedClientLogin(bundle
                                            .getString(AccountManager.KEY_AUTHTOKEN));
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
            Log.d(TAG, "Google docs upload cancelled, deleting temp files...");
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
                    uploadData.filename) : String.format(template, error
                    .getMessage());
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
        Cursor cursor = filterCursor();
        startManagingCursor(cursor);
        FavoritesAdapter adapter = new FavoritesAdapter(this, cursor, this);
        setListAdapter(adapter);
    }

    @Override
    protected void deleteAll() {
        Cursor c = filterCursor();

        db.beginTransaction();
        try {
            while (c.moveToNext()) {
                int id = c.getInt(c.getColumnIndex("_id"));
                db.deleteFavorite(id);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        refresh();
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
                    throw new IllegalArgumentException("Not implemented");
                default:
                    // do noting
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private static class ExportItemsAdapter extends ArrayAdapter<String> {

        private boolean singleType;
        private boolean isPostEclair;

        ExportItemsAdapter(Context context, String[] items, boolean singleType) {
            super(context, android.R.layout.select_dialog_item,
                    android.R.id.text1, items);
            this.singleType = singleType;
            final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
            this.isPostEclair = sdkVersion >= Build.VERSION_CODES.ECLAIR;

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

    private void exportLocalCsv(boolean isKanji) {
        try {
            File exportFile = new File(WwwjdicApplication.getWwwjdicDir(),
                    getCsvExportFilename(isKanji));
            Writer writer = new FileWriter(exportFile);
            exportToCsv(exportFile.getAbsolutePath(), writer, true);
        } catch (IOException e) {
            String message = getResources().getString(R.string.export_error);
            Toast.makeText(Favorites.this,
                    String.format(message, e.getMessage()), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void exportToCsv(String exportFile, Writer w, boolean showMessages) {
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
                String[] entryStr = FavoritesEntryParser
                        .toParsedStringArray(entry);
                writer.writeNext(entryStr);
                count++;
            }

            writer.flush();
            writer.close();

            Analytics.event("favoritesLocalCsvExport", this);

            if (showMessages) {
                String message = getResources().getString(
                        R.string.favorites_exported);
                Toast t = Toast.makeText(Favorites.this, String.format(message,
                        exportFile, count), Toast.LENGTH_SHORT);
                t.show();
            }

        } catch (IOException e) {
            Log.d(TAG, "error exporting to CSV", e);
            if (showMessages) {
                String message = getResources()
                        .getString(R.string.export_error);
                Toast.makeText(Favorites.this,
                        String.format(message, e.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
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

    private void exportToGDocs(boolean isKanji) {
        try {
            Log.d(TAG, "exporting to Google docs...");
            String filename = getCsvExportFilename(isKanji);
            File f = File.createTempFile("favorites-gdocs", ".csv",
                    WwwjdicApplication.getWwwjdicDir());
            f.deleteOnExit();
            Log.d(TAG, "temp file: " + f.getAbsolutePath());
            Log.d(TAG, "document filename: " + filename);
            Writer writer = new FileWriter(f);
            exportToCsv(f.getAbsolutePath(), writer, false);

            uploadData = new UploadData();
            uploadData.contentLength = f.length();
            uploadData.contentType = "text/csv";
            uploadData.filename = filename;
            uploadData.localFilename = f.getAbsolutePath();

            gotAccount(false);
        } catch (IOException e) {
            String message = getResources().getString(R.string.export_error);
            Toast.makeText(Favorites.this,
                    String.format(message, e.getMessage()), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    protected void doExport(final String exportFile) {
        CSVWriter writer = null;
        Cursor c = null;

        try {
            c = filterCursor();
            writer = new CSVWriter(new FileWriter(exportFile));

            int count = 0;
            while (c.moveToNext()) {
                WwwjdicEntry entry = HistoryDbHelper.createWwwjdicEntry(c);
                long time = c.getLong(c.getColumnIndex("time"));
                String[] entryStr = FavoritesEntryParser.toStringArray(entry,
                        time);
                writer.writeNext(entryStr);
                count++;
            }

            writer.flush();
            writer.close();

            Analytics.event("favoritesExport", this);

            String message = getResources().getString(
                    R.string.favorites_exported);
            Toast t = Toast.makeText(Favorites.this, String.format(message,
                    exportFile, count), Toast.LENGTH_SHORT);
            t.show();

        } catch (IOException e) {
            String message = getResources().getString(R.string.export_error);
            Toast.makeText(Favorites.this,
                    String.format(message, e.getMessage()), Toast.LENGTH_SHORT)
                    .show();
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
    protected void doImport(String importFile) {
        CSVReader reader = null;

        db.beginTransaction();
        try {
            db.deleteAllFavorites();

            reader = new CSVReader(new FileReader(importFile));
            if (reader == null) {
                return;
            }

            String[] record = null;
            int count = 0;
            while ((record = reader.readNext()) != null) {
                WwwjdicEntry entry = FavoritesEntryParser
                        .fromStringArray(record);
                long time = Long
                        .parseLong(record[FavoritesEntryParser.TIME_IDX]);
                db.addFavorite(entry, time);
                count++;
            }
            db.setTransactionSuccessful();

            refresh();

            Analytics.event("favoritesImport", this);

            String message = getResources().getString(
                    R.string.favorites_imported);
            Toast t = Toast.makeText(this, String.format(message, importFile,
                    count), Toast.LENGTH_SHORT);
            t.show();
        } catch (IOException e) {
            Log.e(TAG, "error importing favorites", e);
            String message = getResources().getString(R.string.import_error);
            Toast.makeText(this, String.format(message, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
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
