package org.nick.wwwjdic.history;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.util.Log;

import org.json.JSONException;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicApplication;
import org.nick.wwwjdic.model.WwwjdicEntry;
import org.nick.wwwjdic.utils.ActivityUtils;
import org.nick.wwwjdic.utils.MediaScannerWrapper;
import org.nick.wwwjdic.utils.UIUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

@SuppressLint("InlinedApi")
public class AnkiExportService extends IntentService {

    private static final String TAG = AnkiExportService.class.getSimpleName();

    public static final int EXPORT_STARTED_NOTIFICATION_ID = 1;
    public static final int EXPORT_DONE_NOTIFICATION_ID = 2;

    public static final String EXTRA_FILTER_TYPE = "filterType";
    public static final String EXTRA_FILENAME = "filename";

    private static final String ANKI_MIME_TYPE = "application/vnd.anki";

    private int selectedFilter;
    private String exportFilename;

    private Exception error;

    private NotificationManager notificationManager;

    private HistoryDbHelper db;

    public AnkiExportService() {
        super("AnkiExportService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        db = HistoryDbHelper.getInstance(this.getApplicationContext());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        selectedFilter = intent.getIntExtra(EXTRA_FILTER_TYPE,
                HistoryFragmentBase.FILTER_ALL);
        Log.d(TAG, "selectedFilter: " + selectedFilter);

        exportFilename = intent.getStringExtra(EXTRA_FILENAME);
        Log.d(TAG, "anki export filename: " + exportFilename);

        boolean success = exportEntries();
        Resources r = getResources();
        String template = success ? r.getString(R.string.anki_export_success)
                : r.getString(R.string.anki_export_failure);
        String message = success ? String.format(template, exportFilename)
                : String.format(template, error.getMessage());
        notifyExportFinished(message, exportFilename);
    }

    private boolean exportEntries() {
        String message = getApplicationContext().getString(
                R.string.exporting_to_anki);
        showForegroundNotification(message);

        try {
            exportFilename = exportToAnkiDeck();

            if (UIUtils.isFroyo()) {
                MediaScannerWrapper.scanFile(this, exportFilename);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error exporting Anki deck: " + e.getMessage(), e);
            error = e;

            return false;
        }
    }

    private String exportToAnkiDeck() throws IOException, JSONException {
        AnkiGenerator generator = new AnkiGenerator(this);
        File exportFile = new File(WwwjdicApplication.getWwwjdicDir(),
                exportFilename);
        Log.d(TAG,
                "exporting favorites to Anki: " + exportFile.getAbsolutePath());

        List<WwwjdicEntry> entries = new ArrayList<>();
        try (Cursor c = filterCursor()) {
            while (c.moveToNext()) {
                WwwjdicEntry entry = HistoryDbHelper.createWwwjdicEntry(c);
                entries.add(entry);
            }
        }

        int size = generator.createAnkiFile(exportFile.getAbsolutePath(),
                entries);

        Log.d(TAG,
                String.format("Exported %d entries to %s", size,
                        exportFile.getAbsolutePath()));

        return exportFile.getAbsolutePath();
    }

    private Cursor filterCursor() {
        if (selectedFilter == HistoryFragmentBase.FILTER_ALL) {
            return db.getFavorites();

        }

        return db.getFavoritesByType(selectedFilter);
    }

    private void notifyExportFinished(String message, String filename) {
        // clear progress notification
        notificationManager.cancel(EXPORT_STARTED_NOTIFICATION_ID);

        Context appCtx = WwwjdicApplication.getInstance();
        Intent intent = ActivityUtils.createOpenIntent(getApplicationContext(), filename, ANKI_MIME_TYPE);
        PendingIntent pendingIntent = PendingIntent.getActivity(appCtx, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String title = appCtx.getString(R.string.export_finished);
        NotificationCompat.Builder builder = ActivityUtils.createNotification(appCtx, pendingIntent,
                title, message, R.drawable.ic_stat_export);
        builder.setOngoing(false);
        builder.addAction(android.R.drawable.ic_menu_view,
                appCtx.getString(R.string.import_into_anki), pendingIntent);
        Intent shareIntent = ActivityUtils.createShareFileIntent(appCtx,
                filename, ANKI_MIME_TYPE);
        PendingIntent sharePendingIntent = PendingIntent.getActivity(appCtx, 1,
                shareIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(android.R.drawable.ic_menu_share,
                appCtx.getString(R.string.share), sharePendingIntent);

        notificationManager.notify(EXPORT_DONE_NOTIFICATION_ID, builder.build());
    }

    private void showForegroundNotification(@NonNull String message) {
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);

        Context appCtx = getApplicationContext();
        Notification notification = ActivityUtils.createNotification(appCtx, pendingIntent, getResources().getString(
                R.string.exporting_to_anki), message, R.drawable.ic_stat_export).build();

        startForeground(EXPORT_STARTED_NOTIFICATION_ID, notification);
    }

}
