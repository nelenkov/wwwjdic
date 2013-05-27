/**
 * 
 */
package org.nick.wwwjdic.widgets;

import static org.nick.wwwjdic.WwwjdicPreferences.WWWJDIC_DEBUG;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicPreferences;
import org.nick.wwwjdic.client.HttpClientFactory;
import org.nick.wwwjdic.model.KanjiEntry;
import org.nick.wwwjdic.utils.StringUtils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;

public class GetKanjiService extends Service {

    private static final String TAG = GetKanjiService.class.getSimpleName();

    private static final Pattern PRE_START_PATTERN = Pattern
            .compile("^<pre>.*$");

    private static final Pattern PRE_END_PATTERN = Pattern
            .compile("^</pre>.*$");

    private static final String PRE_END_TAG = "</pre>";

    private static final int NUM_RETRIES = 5;

    private static final int RETRY_INTERVAL = 15 * 1000;

    private final Executor executor = Executors.newSingleThreadExecutor();

    private HttpClient httpclient;
    private ResponseHandler<String> responseHandler;

    @Override
    public void onStart(Intent intent, int startId) {
        httpclient = HttpClientFactory
                .createWwwjdicHttpClient(WwwjdicPreferences
                        .getWwwjdicTimeoutSeconds(this) * 1000);
        responseHandler = HttpClientFactory.createWwwjdicResponseHandler();

        executor.execute(new GetKanjiTask());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class GetKanjiTask implements Runnable {
        public void run() {
            try {
                Context ctx = GetKanjiService.this;
                ComponentName kodWidget = new ComponentName(ctx,
                        KodWidgetProvider.class);
                AppWidgetManager manager = AppWidgetManager.getInstance(ctx);
                int[] ids = manager.getAppWidgetIds(kodWidget);

                for (int id : ids) {
                    showLoading(ctx, id);
                }

                String wwwjdicResponse = fetchKanjiFromWwwjdic(ctx);

                for (int id : ids) {
                    RemoteViews updateViews = buildUpdate(ctx, wwwjdicResponse,
                            id);
                    manager.updateAppWidget(id, updateViews);
                }
            } finally {
                scheduleNextUpdate();

                stopSelf();
            }
        }
    }

    private void scheduleNextUpdate() {
        Time time = new Time();
        long updateIntervalMillis = WwwjdicPreferences
                .getKodUpdateInterval(this);
        time.set(System.currentTimeMillis() + updateIntervalMillis);

        long nextUpdate = time.toMillis(false);
        long nowMillis = System.currentTimeMillis();

        long deltaMinutes = (nextUpdate - nowMillis)
                / DateUtils.MINUTE_IN_MILLIS;
        Log.d(TAG, "Requesting next update at " + time + ", in " + deltaMinutes
                + " min");

        Intent updateIntent = new Intent(this, GetKanjiService.class);

        PendingIntent pendingIntent = PendingIntent.getService(this, 0,
                updateIntent, 0);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        alarmManager.set(AlarmManager.RTC, nextUpdate, pendingIntent);
    }

    private void showLoading(Context context, int id) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        RemoteViews views = null;

        boolean showReadingAndMeaning = WwwjdicPreferences
                .isKodShowReading(this);
        if (showReadingAndMeaning) {
            views = new RemoteViews(context.getPackageName(),
                    R.layout.kod_widget_details);
        } else {
            views = new RemoteViews(context.getPackageName(),
                    R.layout.kod_widget);
        }
        KodWidgetProvider.showLoading(this, views);
        manager.updateAppWidget(id, views);
    }

    private RemoteViews buildUpdate(Context context, String wwwjdicResponse,
            int id) {
        RemoteViews views = null;

        try {
            boolean showReadingAndMeaning = WwwjdicPreferences
                    .isKodShowReading(this);
            if (showReadingAndMeaning) {
                views = new RemoteViews(context.getPackageName(),
                        R.layout.kod_widget_details);
            } else {
                views = new RemoteViews(context.getPackageName(),
                        R.layout.kod_widget);
            }

            if (wwwjdicResponse == null) {
                Log.e(TAG, String.format("Failed to get WWWJDIC response "
                        + "after %d tries, giving up.", NUM_RETRIES));
                WwwjdicPreferences.setLastKodUpdateError(context,
                        System.currentTimeMillis());
                KodWidgetProvider.showError(this, views);

                return views;
            }

            if (WWWJDIC_DEBUG) {
                Log.d(TAG, "WWWJDIC response " + wwwjdicResponse);
            }
            List<KanjiEntry> entries = parseResult(wwwjdicResponse);

            if (entries.isEmpty()) {
                WwwjdicPreferences.setLastKodUpdateError(context,
                        System.currentTimeMillis());
                KodWidgetProvider.showError(this, views);

                return views;
            }

            if (!WwwjdicPreferences.isKodRandom(context)) {
                KanjiEntry entry = entries.get(0);
                WwwjdicPreferences.setKodCurrentKanji(context,
                        entry.getHeadword());
            }

            KodWidgetProvider.showKanji(context, views, showReadingAndMeaning,
                    entries, id);
            WwwjdicPreferences.setLastKodUpdateError(context, 0);

            return views;

        } catch (Exception e) {
            Log.e(TAG, "Couldn't contact WWWJDIC", e);
            views = new RemoteViews(context.getPackageName(),
                    R.layout.kod_widget);
            WwwjdicPreferences.setLastKodUpdateError(context,
                    System.currentTimeMillis());
            KodWidgetProvider.showError(this, views);

            return views;
        }
    }

    private String fetchKanjiFromWwwjdic(Context context) {
        try {
            String unicodeCp = selectKanji(context);
            if (WWWJDIC_DEBUG) {
                Log.d(TAG, "KOD Unicode CP: " + unicodeCp);
            }
            String backdoorCode = generateBackdoorCode(unicodeCp);
            if (WWWJDIC_DEBUG) {
                Log.d(TAG, "backdoor code: " + backdoorCode);
            }
            String wwwjdicResponse = null;

            for (int i = 0; i < NUM_RETRIES; i++) {
                try {
                    wwwjdicResponse = query(
                            WwwjdicPreferences.getWwwjdicUrl(this),
                            backdoorCode);
                    if (wwwjdicResponse != null) {
                        break;
                    }
                } catch (Exception e) {
                    if (i < NUM_RETRIES - 1) {
                        Log.w(TAG, String.format("Couldn't contact "
                                + "WWWJDIC, will retry after %d ms.",
                                RETRY_INTERVAL), e);
                        Thread.sleep(RETRY_INTERVAL * (i + 1));
                    } else {
                        Log.e(TAG, "Couldn't contact WWWJDIC.", e);
                    }
                }
            }
            return wwwjdicResponse;
        } catch (InterruptedException e) {
            return null;
        }
    }

    private String selectKanji(Context context) {
        boolean isRandom = WwwjdicPreferences.isKodRandom(this);
        KanjiGenerator generator = new JisGenerator(isRandom,
                WwwjdicPreferences.isKodLevelOneOnly(context));
        if (WwwjdicPreferences.isKodUseJlpt(this)
                && !WwwjdicPreferences.isKodLevelOneOnly(this)) {
            generator = new JlptLevelGenerator(isRandom,
                    WwwjdicPreferences.getKodJlptLevel(this));
        }

        if (!isRandom) {
            String currentKanji = WwwjdicPreferences.getKodCurrentKanji(this);
            generator.setCurrentKanji(currentKanji);
        }

        return generator.selectNextUnicodeCp();
    }

    private String query(String url, String backdoorCode) {
        try {
            String lookupUrl = String.format("%s?%s", url, backdoorCode);
            HttpGet get = new HttpGet(lookupUrl);

            String responseStr = httpclient.execute(get, responseHandler);

            return responseStr;
        } catch (ClientProtocolException cpe) {
            Log.e(TAG, "ClientProtocolException", cpe);
            throw new RuntimeException(cpe);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            throw new RuntimeException(e);
        }
    }

    protected List<KanjiEntry> parseResult(String html) {
        List<KanjiEntry> result = new ArrayList<KanjiEntry>();

        boolean isInPre = false;
        String[] lines = html.split("\n");
        for (String line : lines) {
            if (StringUtils.isEmpty(line)) {
                continue;
            }

            Matcher m = PRE_START_PATTERN.matcher(line);
            if (m.matches()) {
                isInPre = true;
                continue;
            }

            m = PRE_END_PATTERN.matcher(line);
            if (m.matches()) {
                break;
            }

            if (isInPre) {
                boolean hasEndPre = false;
                // some entries have </pre> on the same line
                if (line.contains(PRE_END_TAG)) {
                    hasEndPre = true;
                    line = line.replaceAll(PRE_END_TAG, "");
                }
                if (WWWJDIC_DEBUG) {
                    Log.d(TAG, "dic entry line: " + line);
                }
                KanjiEntry entry = KanjiEntry.parseKanjidic(line);
                result.add(entry);

                if (hasEndPre) {
                    break;
                }
            }
        }

        return result;
    }

    private String generateBackdoorCode(String jisCode) {
        StringBuffer buff = new StringBuffer();
        // always "1" for kanji?
        buff.append("1");
        // raw
        buff.append("Z");
        // code
        buff.append("K");
        // Unicode
        buff.append("U");
        try {
            buff.append(URLEncoder.encode(jisCode, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        return buff.toString();
    }

}
