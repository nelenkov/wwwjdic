package org.nick.wwwjdic.widgets;

import java.util.List;

import org.nick.wwwjdic.BuildConfig;
import org.nick.wwwjdic.KanjiEntryDetail;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicPreferences;
import org.nick.wwwjdic.model.KanjiEntry;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

public class KodWidgetProvider extends AppWidgetProvider {

    private static final String TAG = KodWidgetProvider.class.getSimpleName();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "KOD widget udpate");
        }

        ContextCompat.startForegroundService(context, new Intent(context, GetKanjiService.class));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent);
        // v1.5 fix that doesn't call onDelete Action
        final String action = intent.getAction();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "KOD widget onReceive: " + action);
        }
        if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
            final int appWidgetId = intent.getExtras().getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                this.onDeleted(context, new int[]{appWidgetId});
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

            NetworkInfo ni = extras.getParcelable(ConnectivityManager.EXTRA_NETWORK_INFO);
            if (ni !=null && ni.isConnected()) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, ni.getTypeName() + " is connecting");
                }
                if (WwwjdicPreferences.getLastKodUpdateError(context) != 0) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG,
                                "KOD widget is in error state, trying to update...");
                    }
                    ContextCompat.startForegroundService(context,
                            new Intent(context, GetKanjiService.class));
                }
            }
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDeleted");
        }

        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context,
                KodWidgetProvider.class);
        int[] thisWidgetIds = manager.getAppWidgetIds(thisWidget);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "widget IDs: " + thisWidgetIds.length);
        }

        if (thisWidgetIds.length == 0) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "we are the last widget, cleaning up");

                Log.d(TAG, "stopping update service...");
            }
            boolean stopped = context.stopService(new Intent(context,
                    GetKanjiService.class));
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "stopped: " + stopped);

                Log.d(TAG, "cancelling update timer...");
            }
            AlarmManager alarmManager = (AlarmManager) context
                    .getSystemService(Context.ALARM_SERVICE);
            Intent updateIntent = new Intent(context, GetKanjiService.class);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                    updateIntent, 0);
            alarmManager.cancel(pendingIntent);

            ConnectivityMonitor.stop(context);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context,
                                          AppWidgetManager appWidgetManager, int appWidgetId,
                                          Bundle newOptions) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onAppWidgetOptionsChanged " + appWidgetId);
            Log.d(TAG, "newOptions: " + newOptions);
        }
        for (String key : newOptions.keySet()) {
            Log.d(TAG, key + "=" + newOptions.get(key));
        }

        RemoteViews views = null;
        boolean showReadingAndMeaning = WwwjdicPreferences
                .isKodShowReading(context);
        views = currentRemoveViews(context, showReadingAndMeaning);

        float textSize = getKodTextSize(context, newOptions, appWidgetId,
                showReadingAndMeaning);
        float detailsTextSize = getDetailsTextSize(context, newOptions,
                appWidgetId);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "text size: " + textSize + "sp");
            Log.d(TAG, "details text size: " + detailsTextSize + "sp");
        }

        setTextSizes(views, textSize, detailsTextSize, showReadingAndMeaning);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    static RemoteViews currentRemoveViews(Context context,
                                          boolean showReadingAndMeaning) {
        boolean transparent = WwwjdicPreferences.isKodTransparentBg(context);

        if (showReadingAndMeaning) {
            return new RemoteViews(context.getPackageName(),
                    transparent ? R.layout.kod_widget_details_transparent
                            : R.layout.kod_widget_details);
        }

        return new RemoteViews(context.getPackageName(),
                transparent ? R.layout.kod_widget_transparent
                        : R.layout.kod_widget);
    }

    static float getKodTextSize(Context ctx, Bundle options, int id,
                                boolean showMeaning) {
        float ratio = showMeaning ? getDetailedKodTextSizeRatio(ctx)
                : getKodTextSizeRatio(ctx);

        return getTextSize(ctx, options, id, ratio);
    }

    private static float getDetailedKodTextSizeRatio(Context ctx) {
        int ratio = ctx.getResources().getInteger(
                R.integer.kod_detailed_text_size_ratio);

        return ratio / 100.0f;
    }

    private static float getKodTextSizeRatio(Context ctx) {
        int ratio = ctx.getResources()
                .getInteger(R.integer.kod_text_size_ratio);

        return ratio / 100.0f;
    }

    static float getDetailsTextSize(Context ctx, Bundle options, int id) {
        return getTextSize(ctx, options, id, getKodDetailsTextSizeRatio(ctx));
    }

    private static float getKodDetailsTextSizeRatio(Context ctx) {
        int ratio = ctx.getResources().getInteger(
                R.integer.kod_details_text_size_ratio);

        return ratio / 100.0f;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    static float getTextSize(Context ctx, Bundle options, int id, float scale) {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(ctx);
        if (options == null) {
            options = widgetManager.getAppWidgetOptions(id);
        }

        if (options != null) {
            int appWidgetMaxWidth = options
                    .getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
            int appWidgetMaxHeight = options
                    .getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
            float candidateHeight = (float) (scale * appWidgetMaxHeight);
            float candidateWidth = (float) (scale * appWidgetMaxWidth);

            return Math.min(candidateHeight, candidateWidth);
        }

        return 1;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    static void setTextSizes(RemoteViews views,
                             float textSize, float detailsTextSize,
                             boolean showReadingAndMeaning) {
        if (textSize == 0 || detailsTextSize == 0) {
            return;
        }

        views.setTextViewTextSize(R.id.kod_text, TypedValue.COMPLEX_UNIT_SP,
                textSize);
        views.setTextViewTextSize(R.id.kod_header_text,
                TypedValue.COMPLEX_UNIT_SP, detailsTextSize);
        if (showReadingAndMeaning) {
            views.setTextViewTextSize(R.id.kod_reading,
                    TypedValue.COMPLEX_UNIT_SP, detailsTextSize);
            views.setTextViewTextSize(R.id.kod_meaning,
                    TypedValue.COMPLEX_UNIT_SP, detailsTextSize);
        }
    }

    protected static void setKodWidgetTextSizes(Context context,
                                                RemoteViews views, int widgetId,
                                                boolean showReadingAndMeaning) {
        float textSize = getKodTextSize(context, null, widgetId,
                showReadingAndMeaning);
        float detailsTextSize = getDetailsTextSize(context, null, widgetId);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "text size: " + textSize + "sp");
            Log.d(TAG, "details text size: " + detailsTextSize + "sp");
        }
        if (textSize > 0 && detailsTextSize > 0) {
            setTextSizes(views, textSize, detailsTextSize, showReadingAndMeaning);
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

    public static void clearLoading(RemoteViews views) {
        views.setViewVisibility(R.id.kod_message_text, View.GONE);
        views.setViewVisibility(R.id.widget, View.VISIBLE);
    }

    public static void showKanji(Context context, RemoteViews views,
                                 boolean showReadingAndMeaning, List<KanjiEntry> entries,
                                 int widgetId) {
        KanjiEntry entry = entries.get(0);
        String kod = entry.getHeadword();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "KOD: " + kod);
        }
        Intent intent = new Intent(context, KanjiEntryDetail.class);
        intent.putExtra(KanjiEntryDetail.EXTRA_KANJI_ENTRY, entries.get(0));
        intent.putExtra(KanjiEntryDetail.EXTRA_KOD_WIDGET_CLICK, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        views.setTextViewText(R.id.kod_text, kod);
        if (showReadingAndMeaning) {
            views.setTextViewText(R.id.kod_reading, entry.getReading());
            views.setTextViewText(R.id.kod_meaning, entry.getMeaningsAsString());
        }
        setKodWidgetTextSizes(context, views, widgetId, showReadingAndMeaning);

        views.setOnClickPendingIntent(R.id.widget, pendingIntent);
        KodWidgetProvider.clearLoading(views);
    }
}
