package org.nick.wwwjdic.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

public class UIUtils {

    public static boolean isHoneycomb() {
        // Can use static final constants like HONEYCOMB, declared in later
        // versions
        // of the OS since they are inlined at compile time. This is guaranteed
        // behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean isFroyo() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isHoneycombTablet(Context context) {
        return isHoneycomb() && isTablet(context);
    }

    public static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static boolean isPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }
}
