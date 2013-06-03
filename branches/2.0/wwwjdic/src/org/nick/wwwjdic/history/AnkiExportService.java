package org.nick.wwwjdic.history;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicApplication;
import org.nick.wwwjdic.model.WwwjdicEntry;
import org.nick.wwwjdic.utils.MediaScannerWrapper;
import org.nick.wwwjdic.utils.UIUtils;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

@SuppressLint("InlinedApi")
public class AnkiExportService extends IntentService {

    private static final String TAG = AnkiExportService.class.getSimpleName();

    public static final int NOTIFICATION_ID = 1;

    public static final String EXTRA_FILTER_TYPE = "filterType";
    public static final String EXTRA_FILENAME = "filename";

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
        notifyExportFinished(NOTIFICATION_ID, message, exportFilename,
                "application/vnd.anki");
    }

    private boolean exportEntries() {
        String message = getApplicationContext().getString(
                R.string.exporting_to_anki);
        showNotification(message, NOTIFICATION_ID);

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

        List<WwwjdicEntry> entries = new ArrayList<WwwjdicEntry>();
        Cursor c = null;
        try {
            c = filterCursor();
            while (c.moveToNext()) {
                WwwjdicEntry entry = HistoryDbHelper.createWwwjdicEntry(c);
                entries.add(entry);
            }
        } finally {
            if (c != null) {
                c.close();
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

    private void notifyExportFinished(int notificationId, String message,
            String filename, String mimeType) {
        // clear progress notification
        notificationManager.cancel(notificationId);

        Context appCtx = WwwjdicApplication.getInstance();

        Intent intent = createOpenIntent(filename, mimeType);
        PendingIntent pendingIntent = PendingIntent.getActivity(appCtx, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String title = appCtx.getString(R.string.export_finished);

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
        builder.addAction(android.R.drawable.ic_menu_view,
                appCtx.getString(R.string.import_into_anki), pendingIntent);
        notificationManager.notify(notificationId, builder.build());
    }

    private Intent createOpenIntent(String filename, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.fromFile(new File(filename)), mimeType);

        return Intent.createChooser(intent, getString(R.string.open));
    }

    private void showNotification(String message, int notificationId) {
        Intent startActivityIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, startActivityIntent, 0);

        Builder builder = new NotificationCompat.Builder(
                getApplicationContext());
        builder.setSmallIcon(R.drawable.ic_stat_export);
        builder.setContentTitle(getResources().getString(
                R.string.exporting_to_anki));
        builder.setContentText(message);
        builder.setContentIntent(pendingIntent);
        builder.setDefaults(0);
        builder.setAutoCancel(true);
        builder.setOngoing(true);

        notificationManager.notify(notificationId, builder.build());
    }

}
