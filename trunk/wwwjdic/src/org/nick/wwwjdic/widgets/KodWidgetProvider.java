package org.nick.wwwjdic.widgets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
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
import org.nick.wwwjdic.StringUtils;
import org.nick.wwwjdic.WwwjdicApplication;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

public class KodWidgetProvider extends AppWidgetProvider {

    private static final String TAG = KodWidgetProvider.class.getSimpleName();

    private static final String DEFAULT_WWWJDIC_URL = "http://www.csse.monash.edu.au/~jwb/cgi-bin/wwwjdic.cgi";

    private static final String PREF_WWWJDIC_URL_KEY = "pref_wwwjdic_mirror_url";
    private static final String PREF_WWWJDIC_TIMEOUT_KEY = "pref_wwwjdic_timeout";

    private static final Pattern PRE_START_PATTERN = Pattern
            .compile("^<pre>.*$");

    private static final Pattern PRE_END_PATTERN = Pattern
            .compile("^</pre>.*$");

    private static final String PRE_END_TAG = "</pre>";

    private static final RandomJisGenerator jisGenerator = new RandomJisGenerator();

    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        context.startService(new Intent(context, GetKanjiService.class));
    }

    public static class GetKanjiService extends Service {
        @Override
        public void onStart(Intent intent, int startId) {
            RemoteViews updateViews = buildUpdate(this);

            ComponentName thisWidget = new ComponentName(this,
                    KodWidgetProvider.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        public RemoteViews buildUpdate(Context context) {
            RemoteViews views = null;

            try {
                HttpClient client = createHttpClient(getWwwjdicUrl(),
                        getHttpTimeoutSeconds() * 1000);
                String jisCode = jisGenerator.generate();
                Log.d(TAG, "KOD JIS: " + jisCode);
                String backdoorCode = generateBackdoorCode(jisCode);
                Log.d(TAG, "backdoor code: " + backdoorCode);
                String wwwjdicResponse = query(client, getWwwjdicUrl(),
                        backdoorCode);
                Log.d(TAG, "WWWJDIC response " + wwwjdicResponse);
                List<KanjiEntry> entries = parseResult(wwwjdicResponse);

                views = new RemoteViews(context.getPackageName(),
                        R.layout.kod_widget);

                if (entries.isEmpty()) {
                    views.setTextViewText(R.id.kod_text, "E");

                    return views;
                }

                String kod = entries.get(0).getHeadword();
                Log.d(TAG, "KOD: " + kod);
                Intent intent = new Intent(context, KanjiEntryDetail.class);
                intent.putExtra(Constants.KANJI_ENTRY_KEY, entries.get(0));
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

                views.setTextViewText(R.id.kod_text, kod);
                views.setOnClickPendingIntent(R.id.kod_text, pendingIntent);

                return views;

            } catch (Exception e) {
                Log.e(TAG, "Couldn't contact WWWJDIC", e);
                views = new RemoteViews(context.getPackageName(),
                        R.layout.kod_widget);
                views.setTextViewText(R.id.kod_text, "E");

                return views;
            }
        }

        private HttpClient createHttpClient(String url, int timeoutMillis) {
            Log.d(TAG, "WWWJDIC URL: " + url);
            Log.d(TAG, "HTTP timeout: " + timeoutMillis);
            HttpClient httpclient = new DefaultHttpClient();
            HttpParams httpParams = httpclient.getParams();
            HttpConnectionParams
                    .setConnectionTimeout(httpParams, timeoutMillis);
            HttpConnectionParams.setSoTimeout(httpParams, timeoutMillis);
            HttpProtocolParams.setUserAgent(httpParams, WwwjdicApplication
                    .getUserAgentString());

            return httpclient;
        }

        private String query(HttpClient httpclient, String url,
                String backdoorCode) {
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

        private String getWwwjdicUrl() {
            SharedPreferences preferences = PreferenceManager
                    .getDefaultSharedPreferences(this);

            return preferences.getString(PREF_WWWJDIC_URL_KEY,
                    DEFAULT_WWWJDIC_URL);
        }

        private int getHttpTimeoutSeconds() {
            SharedPreferences preferences = PreferenceManager
                    .getDefaultSharedPreferences(this);

            String timeoutStr = preferences.getString(PREF_WWWJDIC_TIMEOUT_KEY,
                    "10");

            return Integer.parseInt(timeoutStr);
        }
    }
}
