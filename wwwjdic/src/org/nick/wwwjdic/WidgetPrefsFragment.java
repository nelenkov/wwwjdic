package org.nick.wwwjdic;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import org.nick.wwwjdic.widgets.KodWidgetConfigure;
import org.nick.wwwjdic.widgets.KodWidgetProvider;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import static org.nick.wwwjdic.WwwjdicPreferences.PREF_KOD_KEY;

public class WidgetPrefsFragment extends PreferenceFragmentCompat {

    @Override
    public void onResume() {
        super.onResume();

        AppWidgetManager wm = AppWidgetManager.getInstance(getActivity());
        ComponentName kodWidget = new ComponentName(getActivity(),
                KodWidgetProvider.class);
        int[] ids = wm.getAppWidgetIds(kodWidget);
        boolean hasWidgets = ids != null && ids.length > 0;
        findPreference(PREF_KOD_KEY).setEnabled(hasWidgets);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.widget_prefs, rootKey);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();
        if (PREF_KOD_KEY.equals(key)) {
            Intent intent = new Intent(getActivity(), KodWidgetConfigure.class);
            // anything but 0 should do
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 1);
            // configure activity will launch the update service, so
            // we don't really care about the result
            startActivity(intent);

            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }

}
