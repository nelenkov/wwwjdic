package org.nick.wwwjdic;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class KodWidgetProvider extends AppWidgetProvider {

    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this
        // provider
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Create an Intent to launch ExampleActivity
            SearchCriteria criteria = SearchCriteria
                    .createForKanjiOrReading("Š¿");
            Intent intent = new Intent(context, KanjiResultListView.class);
            intent.putExtra(Constants.CRITERIA_KEY, criteria);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    intent, 0);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.kod_widget);
            views.setTextViewText(R.id.kod_text, "Š¿Žš");
            views.setOnClickPendingIntent(R.id.kod_text, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current App
            // Widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
