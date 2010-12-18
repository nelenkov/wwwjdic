/**
 * 
 */
package org.nick.wwwjdic.widgets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.GzipStringResponseHandler;
import org.nick.wwwjdic.KanjiEntry;
import org.nick.wwwjdic.KanjiEntryDetail;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicApplication;
import org.nick.wwwjdic.WwwjdicPreferences;
import org.nick.wwwjdic.utils.StringUtils;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class GetKanjiService extends Service {

    private static final String TAG = GetKanjiService.class.getSimpleName();

    private static final Pattern PRE_START_PATTERN = Pattern
            .compile("^<pre>.*$");

    private static final Pattern PRE_END_PATTERN = Pattern
            .compile("^</pre>.*$");

    private static final String PRE_END_TAG = "</pre>";

    private static final int NUM_RETRIES = 3;

    private static final int RETRY_INTERVAL = 1000;

    private final RandomJisGenerator jisGenerator = new RandomJisGenerator();

    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public void onStart(Intent intent, int startId) {
        executor.execute(new GetKanjiTask());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class GetKanjiTask implements Runnable {
        public void run() {
            RemoteViews updateViews = buildUpdate(GetKanjiService.this);

            ComponentName thisWidget = new ComponentName(GetKanjiService.this,
                    KodWidgetProvider.class);
            AppWidgetManager manager = AppWidgetManager
                    .getInstance(GetKanjiService.this);
            manager.updateAppWidget(thisWidget, updateViews);

            stopSelf();
        }
    }

    private RemoteViews buildUpdate(Context context) {
        RemoteViews views = null;

        try {
            views = new RemoteViews(context.getPackageName(),
                    R.layout.kod_widget);
            showLoading(views);

            HttpClient client = createHttpClient(WwwjdicPreferences
                    .getWwwjdicUrl(this), WwwjdicPreferences
                    .getWwwjdicTimeoutSeconds(this) * 1000);
            String jisCode = jisGenerator.generate(WwwjdicPreferences
                    .isKodLevelOneOnly(context));
            Log.d(TAG, "KOD JIS: " + jisCode);
            String backdoorCode = generateBackdoorCode(jisCode);
            Log.d(TAG, "backdoor code: " + backdoorCode);
            String wwwjdicResponse = null;

            for (int i = 0; i < NUM_RETRIES; i++) {
                try {
                    wwwjdicResponse = query(client, WwwjdicPreferences
                            .getWwwjdicUrl(this), backdoorCode);
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

            if (wwwjdicResponse == null) {
                Log.e(TAG, String.format("Failed to get WWWJDIC response "
                        + "after %d tries, giving up.", NUM_RETRIES));
                showError(views);

                return views;
            }

            Log.d(TAG, "WWWJDIC response " + wwwjdicResponse);
            List<KanjiEntry> entries = parseResult(wwwjdicResponse);

            if (entries.isEmpty()) {
                showError(views);

                return views;
            }

            String kod = entries.get(0).getHeadword();
            Log.d(TAG, "KOD: " + kod);
            Intent intent = new Intent(context, KanjiEntryDetail.class);
            intent.putExtra(Constants.KANJI_ENTRY_KEY, entries.get(0));
            intent.putExtra(Constants.KOD_WIDGET_CLICK, true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    intent, PendingIntent.FLAG_CANCEL_CURRENT);

            String dateStr = DateFormat.getDateFormat(this).format(new Date());
            views.setTextViewText(R.id.kod_date_text, dateStr);
            views.setTextViewText(R.id.kod_text, kod);
            views.setOnClickPendingIntent(R.id.kod_text, pendingIntent);
            clearLoading(views);

            return views;

        } catch (Exception e) {
            Log.e(TAG, "Couldn't contact WWWJDIC", e);
            views = new RemoteViews(context.getPackageName(),
                    R.layout.kod_widget);
            showError(views);

            return views;
        }
    }

    private void showError(RemoteViews views) {
        views.setViewVisibility(R.id.kod_message_text, View.VISIBLE);
        views.setTextViewText(R.id.kod_message_text, getResources().getString(
                R.string.error));
        views.setViewVisibility(R.id.widget, View.GONE);
    }

    private void showLoading(RemoteViews views) {
        views.setTextViewText(R.id.kod_message_text, getResources().getString(
                R.string.widget_loading));
        views.setViewVisibility(R.id.kod_message_text, View.VISIBLE);
        views.setViewVisibility(R.id.widget, View.GONE);
    }

    private void clearLoading(RemoteViews views) {
        views.setViewVisibility(R.id.kod_message_text, View.GONE);
        views.setViewVisibility(R.id.widget, View.VISIBLE);
    }

    private HttpClient createHttpClient(String url, int timeoutMillis) {
        Log.d(TAG, "WWWJDIC URL: " + url);
        Log.d(TAG, "HTTP timeout: " + timeoutMillis);
        HttpClient httpclient = new DefaultHttpClient();
        HttpParams httpParams = httpclient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, timeoutMillis);
        HttpConnectionParams.setSoTimeout(httpParams, timeoutMillis);
        HttpProtocolParams.setUserAgent(httpParams, WwwjdicApplication
                .getUserAgentString());

        return httpclient;
    }

    private String query(HttpClient httpclient, String url, String backdoorCode) {
        try {
            String lookupUrl = String.format("%s?%s", url, backdoorCode);
            HttpGet get = new HttpGet(lookupUrl);

            GzipStringResponseHandler responseHandler = new GzipStringResponseHandler();
            String responseStr = httpclient.execute(get, responseHandler);
            // localContext);

            return responseStr;
        } catch (ClientProtocolException cpe) {
            Log.e("WWWJDIC", "ClientProtocolException", cpe);
            throw new RuntimeException(cpe);
        } catch (IOException e) {
            Log.e("WWWJDIC", "IOException", e);
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
                Log.d(TAG, "dic entry line: " + line);
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
        // JIS
        buff.append("J");
        try {
            buff.append(URLEncoder.encode(jisCode, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        return buff.toString();
    }

}
