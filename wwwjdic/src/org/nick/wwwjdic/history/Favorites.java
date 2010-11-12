package org.nick.wwwjdic.history;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.nick.wwwjdic.Analytics;
import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.DictionaryEntryDetail;
import org.nick.wwwjdic.KanjiEntryDetail;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicApplication;
import org.nick.wwwjdic.WwwjdicEntry;
import org.nick.wwwjdic.history.FavoritesItem.FavoriteStatusChangedListener;
import org.nick.wwwjdic.history.gdocs.DocsUrl;
import org.nick.wwwjdic.history.gdocs.DocumentListEntry;
import org.nick.wwwjdic.history.gdocs.DocumentListFeed;
import org.nick.wwwjdic.history.gdocs.Link;
import org.nick.wwwjdic.history.gdocs.Namespace;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Images;
import android.util.Log;
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

    //
    private static final String AUTH_TOKEN_TYPE = "writely";

    private static final int REQUEST_AUTHENTICATE = 0;

    private static final String PREF = "MyPrefs";

    private static final int DIALOG_ACCOUNTS = 0;

    //

    private static HttpTransport transport;

    private String postLink;

    private String authToken;

    private final List<DocumentListEntry> documents = new ArrayList<DocumentListEntry>();

    public Favorites() {
        transport = GoogleTransport.create();
        GoogleHeaders headers = (GoogleHeaders) transport.defaultHeaders;
        headers.setApplicationName(WwwjdicApplication.getUserAgentString());
        headers.gdataVersion = "3";
        AtomParser parser = new AtomParser();
        parser.namespaceDictionary = Namespace.DICTIONARY;
        transport.addParser(parser);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_ACCOUNTS:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select a Google account");
            final AccountManager manager = AccountManager.get(this);
            final Account[] accounts = manager.getAccountsByType("com.google");
            final int size = accounts.length;
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
        }
        return null;
    }

    private void gotAccount(boolean tokenExpired) {
        SharedPreferences settings = getSharedPreferences(PREF, 0);
        String accountName = settings.getString("accountName", null);
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
        showDialog(DIALOG_ACCOUNTS);
    }

    private void gotAccount(final AccountManager manager, final Account account) {
        SharedPreferences settings = getSharedPreferences(PREF, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("accountName", account.name);
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
                showDialog(DIALOG_ACCOUNTS);
            }
            break;
        }
    }

    private void authenticatedClientLogin(String authToken) {
        this.authToken = authToken;
        ((GoogleHeaders) transport.defaultHeaders).setGoogleLogin(authToken);
        authenticated();
    }

    static class SendData {
        String fileName;
        Uri uri;
        String contentType;
        long contentLength;

        SendData() {
        }

        SendData(Intent intent, ContentResolver contentResolver) {
            Bundle extras = intent.getExtras();
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                Uri uri = this.uri = (Uri) extras
                        .getParcelable(Intent.EXTRA_STREAM);
                String scheme = uri.getScheme();
                if (scheme.equals("content")) {
                    Cursor cursor = contentResolver.query(uri, null, null,
                            null, null);
                    cursor.moveToFirst();
                    this.fileName = cursor.getString(cursor
                            .getColumnIndexOrThrow(Images.Media.DISPLAY_NAME));
                    this.contentType = intent.getType();
                    this.contentLength = cursor.getLong(cursor
                            .getColumnIndexOrThrow(Images.Media.SIZE));
                }
            }
        }
    }

    static SendData sendData;

    private void authenticated() {
        if (sendData != null) {
            try {
                if (sendData.fileName != null) {
                    boolean success = false;
                    try {
                        HttpRequest request = transport.buildPostRequest();
                        request.url = DocsUrl.forDefaultPrivateFull();
                        ((GoogleHeaders) request.headers)
                                .setSlugFromFileName(sendData.fileName);
                        InputStreamContent content = new InputStreamContent();
                        content.inputStream = new FileInputStream(
                                sendData.fileName);
                        // getContentResolver().openInputStream(sendData.uri);
                        content.type = sendData.contentType;
                        content.length = sendData.contentLength;
                        request.content = content;
                        request.execute().ignore();
                        success = true;
                    } catch (IOException e) {
                        handleException(e);
                    }
                    String message = success ? "OK" : "ERROR";
                    Toast t = Toast.makeText(this, "upload: " + message,
                            Toast.LENGTH_SHORT);
                    t.show();
                }
            } finally {
                sendData = null;
            }
        } else {
            executeRefreshTitles();
        }
    }

    private void setLogging(boolean logging) {
        Logger.getLogger("com.google.api.client").setLevel(Level.ALL);
        SharedPreferences settings = getSharedPreferences(PREF, 0);
        boolean currentSetting = settings.getBoolean("logging", false);
        if (currentSetting != logging) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("logging", logging);
            editor.commit();
        }
    }

    private void executeRefreshTitles() {
        String[] documentTitles;
        List<DocumentListEntry> albums = this.documents;
        albums.clear();
        try {
            DocsUrl url = DocsUrl.forDefaultPrivateFull();
            // page through results
            DocumentListFeed userFeed = DocumentListFeed.executeGet(transport,
                    url);
            documentTitles = new String[userFeed.docs.size()];
            List<Link> links = userFeed.links;
            for (int i = 0; i < userFeed.docs.size(); i++) {
                documentTitles[i] = userFeed.docs.get(i).title;
                System.out.println(userFeed.docs.get(i).title);
            }
        } catch (IOException e) {
            handleException(e);
            documentTitles = new String[] { e.getMessage() };
            albums.clear();
        }
        setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, documentTitles));
    }

    private void handleException(Exception e) {
        e.printStackTrace();
        SharedPreferences settings = getSharedPreferences(PREF, 0);
        boolean log = true;// settings.getBoolean("logging", true);
        if (e instanceof HttpResponseException) {
            HttpResponse response = ((HttpResponseException) e).response;
            int statusCode = response.statusCode;
            try {
                response.ignore();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            if (statusCode == 401 || statusCode == 403) {
                gotAccount(true);
                return;
            }
            if (log) {
                try {
                    Log.e(TAG, response.parseAsString());
                } catch (IOException parseException) {
                    parseException.printStackTrace();
                }
            }
        }
        if (log) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    //

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
    protected void doExport(final String exportFile) {
        CSVWriter writer = null;

        try {
            final Cursor c = filterCursor();

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
            //
            File f = new File(exportFile);
            sendData = new SendData();
            sendData.contentLength = f.length();
            sendData.contentType = "text/csv";
            sendData.fileName = exportFile;

            gotAccount(false);

            Analytics.event("favoritesExport", this);

            String message = getResources().getString(
                    R.string.favorites_exported);
            Toast t = Toast.makeText(Favorites.this, String.format(message,
                    exportFile, count), Toast.LENGTH_SHORT);
            t.show();

            // } catch (IOException e) {
        } catch (Exception e) {
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
