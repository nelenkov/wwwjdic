package org.nick.wwwjdic.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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

            Log.d(TAG, "done");
        }
    }
}
