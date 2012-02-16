package org.nick.wwwjdic.widgets;

import java.util.List;

import org.nick.wwwjdic.KanjiEntryDetail;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicPreferences;
import org.nick.wwwjdic.model.KanjiEntry;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class KodWidgetProvider extends AppWidgetProvider {

    private static final String TAG = KodWidgetProvider.class.getSimpleName();

    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        Log.d(TAG, "KOD widget udpate");
        context.startService(new Intent(context, GetKanjiService.class));
    }

    public void onReceive(Context context, Intent intent) {
        // v1.5 fix that doesn't call onDelete Action
        final String action = intent.getAction();
        Log.d(TAG, "KOD widget onReceive: " + action);
        if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
            final int appWidgetId = intent.getExtras().getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                this.onDeleted(context, new int[] { appWidgetId });
            }
        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            Bundle extras = intent.getExtras();
            // only on IS04 2.2.2?
            if (extras == null) {
                return;
            }
            boolean noConnectivity = extras.getBoolean(
                    ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            Log.d(TAG, "CONNECTIVITY_ACTION::noConnectivity : "
                    + noConnectivity);
            if (noConnectivity) {
                return;
            }

            NetworkInfo ni = (NetworkInfo) extras
                    .getParcelable(ConnectivityManager.EXTRA_NETWORK_INFO);
            if (ni.isConnected()) {
                Log.d(TAG, ni.getTypeName() + " is connecting");
                if (WwwjdicPreferences.getLastKodUpdateError(context) != 0) {
                    Log.d(TAG,
                            "KOD widget is in error state, trying to update...");
                    context.startService(new Intent(context,
                            GetKanjiService.class));
                }
            }
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d(TAG, "onDeleted");

        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context,
                KodWidgetProvider.class);
        int[] thisWidgetIds = manager.getAppWidgetIds(thisWidget);
        Log.d(TAG, "widget IDs: " + thisWidgetIds.length);

        if (thisWidgetIds.length == 0) {
            Log.d(TAG, "we are the last widget, cleaning up");

            Log.d(TAG, "stopping update service...");
            boolean stopped = context.stopService(new Intent(context,
                    GetKanjiService.class));
            Log.d(TAG, "stopped: " + stopped);

            Log.d(TAG, "cancelling update timer...");
            AlarmManager alarmManager = (AlarmManager) context
                    .getSystemService(Context.ALARM_SERVICE);
            Intent updateIntent = new Intent(context, GetKanjiService.class);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                    updateIntent, 0);
            alarmManager.cancel(pendingIntent);

            ConnectivityMonitor.stop(context);
        }
    }

    public static void showError(Context context, RemoteViews views) {
        views.setViewVisibility(R.id.kod_message_text, View.VISIBLE);
        views.setTextViewText(R.id.kod_message_text, context.getResources()
                .getString(R.string.error));
        views.setViewVisibility(R.id.widget, View.GONE);

        Intent updateIntent = new Intent(context, GetKanjiService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.kod_message_text, pendingIntent);
    }

    public static void showLoading(Context context, RemoteViews views) {
        views.setTextViewText(R.id.kod_message_text, context.getResources()
                .getString(R.string.widget_loading));
        views.setViewVisibility(R.id.kod_message_text, View.VISIBLE);
        views.setViewVisibility(R.id.widget, View.GONE);
    }

    public static void clearLoading(Context context, RemoteViews views) {
        views.setViewVisibility(R.id.kod_message_text, View.GONE);
        views.setViewVisibility(R.id.widget, View.VISIBLE);
    }

    public static void showKanji(Context context, RemoteViews views,
            boolean showReadingAndMeaning, List<KanjiEntry> entries) {
        KanjiEntry entry = entries.get(0);
        String kod = entry.getHeadword();
        Log.d(TAG, "KOD: " + kod);
        Intent intent = new Intent(context, KanjiEntryDetail.class);
        intent.putExtra(KanjiEntryDetail.EXTRA_KANJI_ENTRY, entries.get(0));
        intent.putExtra(KanjiEntryDetail.EXTRA_KOD_WIDGET_CLICK, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);

        views.setTextViewText(R.id.kod_text, kod);
        if (showReadingAndMeaning) {
            views.setTextViewText(R.id.kod_reading, entry.getReading());
            views.setTextViewText(R.id.kod_meaning, entry.getMeaningsAsString());
        }

        views.setOnClickPendingIntent(R.id.widget, pendingIntent);
        KodWidgetProvider.clearLoading(context, views);
    }
}
