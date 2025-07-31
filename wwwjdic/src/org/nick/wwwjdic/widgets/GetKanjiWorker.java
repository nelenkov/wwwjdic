package org.nick.wwwjdic.widgets;

import static org.nick.wwwjdic.WwwjdicPreferences.WWWJDIC_DEBUG;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.nick.wwwjdic.BuildConfig;
import org.nick.wwwjdic.WwwjdicApplication;
import org.nick.wwwjdic.WwwjdicPreferences;
import org.nick.wwwjdic.client.HttpClientFactory;
import org.nick.wwwjdic.model.KanjiEntry;
import org.nick.wwwjdic.utils.StringUtils;

@SuppressLint("Registered")
@SuppressWarnings("deprecation")
public class GetKanjiWorker extends Worker {

    private static final String TAG = GetKanjiWorker.class.getSimpleName();

    private static final Pattern PRE_START_PATTERN = Pattern.compile("^<pre>.*$");

    private static final Pattern PRE_END_PATTERN = Pattern.compile("^</pre>.*$");

    private static final String PRE_END_TAG = "</pre>";

    private static final String FONT_TAG = "<font";
    private static final String BR_TAG = "<br";

    private static final int NUM_RETRIES = 5;

    private static final int RETRY_INTERVAL = 15 * 1000;

    private final HttpClient httpclient;
    private final ResponseHandler<String> responseHandler;

    public GetKanjiWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        httpclient = HttpClientFactory
            .createWwwjdicHttpClient(WwwjdicPreferences
                .getWwwjdicTimeoutSeconds(context) * 1000);
        responseHandler = HttpClientFactory.createWwwjdicResponseHandler();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork()");
        getKanji();
        return Result.success();
    }

    private void getKanji() {
        Log.d(TAG, "getKanji()");
        try {
            Context ctx = getApplicationContext();
            ComponentName kodWidget = new ComponentName(ctx,
                KodWidgetProvider.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(ctx);
            int[] ids = manager.getAppWidgetIds(kodWidget);

            for (int id : ids) {
                showLoading(ctx, id);
            }

            String wwwjdicResponse = fetchKanjiFromWwwjdic(ctx);

            for (int id : ids) {
                RemoteViews updateViews = buildUpdate(ctx, wwwjdicResponse, id);
                manager.updateAppWidget(id, updateViews);
            }
        } finally {
            scheduleNextUpdate();
        }
    }

    private void scheduleNextUpdate() {
        long updateIntervalMillis = WwwjdicPreferences.getKodUpdateInterval(getApplicationContext());
        long nowMillis = System.currentTimeMillis();

        Calendar dueDate = Calendar.getInstance();
        dueDate.setTimeInMillis(nowMillis + updateIntervalMillis);
        long nextUpdate = dueDate.getTimeInMillis();
        long deltaMinutes = (nextUpdate - nowMillis) / DateUtils.MINUTE_IN_MILLIS;
        Log.d(TAG, "Requesting next update at " + dueDate + ", in " + deltaMinutes + " min");

        long timeDiff = dueDate.getTimeInMillis() - nowMillis;
        WorkRequest nextWorkRequest = new  OneTimeWorkRequest.Builder(GetKanjiWorker.class)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .build();

        WorkManager
            .getInstance(WwwjdicApplication.getInstance().getApplicationContext())
            .enqueue(nextWorkRequest);
    }

    private void showLoading(Context context, int id) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        boolean showReadingAndMeaning = WwwjdicPreferences
                .isKodShowReading(getApplicationContext());
        RemoteViews views = KodWidgetProvider.currentRemoveViews(context,
                showReadingAndMeaning);

        KodWidgetProvider.showLoading(getApplicationContext(), views);
        manager.updateAppWidget(id, views);
    }

    private RemoteViews buildUpdate(Context context, String wwwjdicResponse,
                                    int id) {
        try {
            boolean showReadingAndMeaning = WwwjdicPreferences
                    .isKodShowReading(getApplicationContext());
            RemoteViews views = KodWidgetProvider.currentRemoveViews(context,
                    showReadingAndMeaning);

            if (wwwjdicResponse == null) {
                Log.e(TAG, String.format("Failed to get WWWJDIC response "
                        + "after %d tries, giving up.", NUM_RETRIES));
                WwwjdicPreferences.setLastKodUpdateError(context,
                        System.currentTimeMillis());
                KodWidgetProvider.showError(getApplicationContext(), views);

                return views;
            }

            if (WWWJDIC_DEBUG) {
                Log.d(TAG, "WWWJDIC response " + wwwjdicResponse);
            }
            List<KanjiEntry> entries = parseResult(wwwjdicResponse);

            if (entries.isEmpty()) {
                WwwjdicPreferences.setLastKodUpdateError(context, System.currentTimeMillis());
                KodWidgetProvider.showError(getApplicationContext(), views);

                return views;
            }

            if (!WwwjdicPreferences.isKodRandom(context)) {
                KanjiEntry entry = entries.get(0);
                WwwjdicPreferences.setKodCurrentKanji(context, entry.getHeadword());
            }

            KodWidgetProvider.showKanji(context, views, showReadingAndMeaning, entries, id);
            WwwjdicPreferences.setLastKodUpdateError(context, 0);

            return views;

        } catch (Exception e) {
            Log.e(TAG, "Couldn't contact WWWJDIC", e);
            RemoteViews views = KodWidgetProvider.currentRemoveViews(context,
                    false);
            WwwjdicPreferences.setLastKodUpdateError(context,
                    System.currentTimeMillis());
            KodWidgetProvider.showError(getApplicationContext(), views);

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
                            WwwjdicPreferences.getWwwjdicUrl(getApplicationContext()),
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
        boolean isRandom = WwwjdicPreferences.isKodRandom(context);
        KanjiGenerator generator = new JisGenerator(isRandom,
                WwwjdicPreferences.isKodLevelOneOnly(context));
        if (WwwjdicPreferences.isKodUseJlpt(context)
                && !WwwjdicPreferences.isKodLevelOneOnly(context)) {
            generator = new JlptLevelGenerator(isRandom,
                    WwwjdicPreferences.getKodJlptLevel(context));
        }

        if (!isRandom) {
            String currentKanji = WwwjdicPreferences.getKodCurrentKanji(context);
            generator.setCurrentKanji(currentKanji);
        }

        return generator.selectNextUnicodeCp();
    }

    @SuppressWarnings("deprecation")
    private String query(String url, String backdoorCode) {
        try {
            String lookupUrl = String.format("%s?%s", url, backdoorCode);
            HttpGet get = new HttpGet(lookupUrl);

            String responseStr = httpclient.execute(get, responseHandler);
            if (BuildConfig.DEBUG) {
                 Log.d(TAG, "WWWJDIC response: " + responseStr);
            }

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
        List<KanjiEntry> result = new ArrayList<>();

        boolean isInPre = false;
        String[] lines = html.split("\n");
        for (String line : lines) {
            if (StringUtils.isEmpty(line)) {
                continue;
            }

            if (line.toLowerCase().startsWith(FONT_TAG)) {
                continue;
            }

            if (line.toLowerCase().startsWith(BR_TAG)) {
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
        StringBuilder buff = new StringBuilder();
        // always "1" for kanji?
        buff.append("1");
        // raw
        buff.append("Z");
        // code
        buff.append("K");
        // Unicode
        buff.append("U");
        buff.append(URLEncoder.encode(jisCode, StandardCharsets.UTF_8));

        return buff.toString();
    }

}
