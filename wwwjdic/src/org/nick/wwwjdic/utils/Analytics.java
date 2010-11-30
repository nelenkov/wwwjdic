package org.nick.wwwjdic.utils;

import org.nick.wwwjdic.WwwjdicApplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.flurry.android.FlurryAgent;

public class Analytics {

    private static final String PREF_ENABLE_ANALYTICS_KEY = "pref_enable_analytics";

    public static void startSession(Context ctx) {
        if (isEnabled(ctx)) {
            FlurryAgent.onStartSession(ctx, WwwjdicApplication.getFlurryKey());
        }
    }

    public static void endSession(Context ctx) {
        if (isEnabled(ctx)) {
            FlurryAgent.onEndSession(ctx);
        }
    }

    public static void event(String eventId, Context ctx) {
        if (isEnabled(ctx)) {
            FlurryAgent.onEvent(eventId);
        }
    }

    private static boolean isEnabled(Context ctx) {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(ctx);

        return preferences.getBoolean(PREF_ENABLE_ANALYTICS_KEY, true);
    }
}
