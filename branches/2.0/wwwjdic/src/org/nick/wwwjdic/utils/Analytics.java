package org.nick.wwwjdic.utils;

import org.nick.wwwjdic.WwwjdicApplication;
import org.nick.wwwjdic.WwwjdicPreferences;

import android.content.Context;

import com.flurry.android.FlurryAgent;

public class Analytics {

    public static void startSession(Context ctx) {
        if (WwwjdicPreferences.isAnalyticsEnabled(ctx)
                && WwwjdicApplication.getFlurryKey() != null) {
            FlurryAgent.onStartSession(ctx, WwwjdicApplication.getFlurryKey());
        }
    }

    public static void endSession(Context ctx) {
        if (WwwjdicPreferences.isAnalyticsEnabled(ctx)
                && WwwjdicApplication.getFlurryKey() != null) {
            FlurryAgent.onEndSession(ctx);
        }
    }

    public static void event(String eventId, Context ctx) {
        if (WwwjdicPreferences.isAnalyticsEnabled(ctx)
                && WwwjdicApplication.getFlurryKey() != null) {
            FlurryAgent.onEvent(eventId);
        }
    }

}
