package org.nick.wwwjdic.utils;

import java.util.Locale;

import org.nick.wwwjdic.R;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.widget.TextView;

public class UIUtils {

    public static boolean isHoneycomb() {
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

    public static boolean isIcs() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static Drawable getListActivatedDrawable(Context ctx) {
        Drawable result = ctx.getResources().getDrawable(
                R.drawable.list_activated_holo);

        if (isHoneycomb()) {
            TypedArray a = ctx
                    .obtainStyledAttributes(new int[] { android.R.attr.activatedBackgroundIndicator });
            int resource = a.getResourceId(0, 0);
            a.recycle();

            Drawable d = ctx.getResources().getDrawable(resource);
            d.setState(new int[] { android.R.attr.state_activated });
            result = d.getCurrent();
        }

        return result;
    }

    public static void setTextLocale(TextView tv, Locale locale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            tv.setTextLocale(locale);
        }
    }

    public static void setJpTextLocale(TextView tv) {
        setTextLocale(tv, Locale.JAPAN);
    }

    public static int fetchOnBackgroundColor(Context ctx) {
        TypedValue typedValue = new TypedValue();

        TypedArray a = ctx.obtainStyledAttributes(typedValue.data, new int[] { R.attr.colorOnBackground });
        int color = a.getColor(0, 0);

        a.recycle();

        return color;
    }
}
