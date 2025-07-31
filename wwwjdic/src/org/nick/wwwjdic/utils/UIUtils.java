package org.nick.wwwjdic.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;
import java.util.Locale;
import org.nick.wwwjdic.R;

@SuppressWarnings("deprecation")
public class UIUtils {

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean isHoneycombTablet(Context context) {
        return isTablet(context);
    }

    public static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static boolean isPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public static Drawable getListActivatedDrawable(Context ctx) {
        Drawable result = ResourcesCompat.getDrawable(ctx.getResources(),
            R.drawable.list_activated_holo, null);

        int resource;
        try (TypedArray a = ctx
              .obtainStyledAttributes(new int[]{android.R.attr.activatedBackgroundIndicator})) {
            resource = a.getResourceId(0, 0);
            // a.recycle();
        }

        Drawable d = ResourcesCompat.getDrawable(ctx.getResources(), resource, null);
        d.setState(new int[] { android.R.attr.state_activated });
        result = d.getCurrent();

        return result;
    }

    public static void setTextLocale(TextView tv, Locale locale) {
      tv.setTextLocale(locale);
    }

    public static void setJpTextLocale(TextView tv) {
        setTextLocale(tv, Locale.JAPAN);
    }
}
