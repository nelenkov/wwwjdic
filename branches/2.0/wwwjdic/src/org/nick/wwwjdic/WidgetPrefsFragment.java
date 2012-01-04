package org.nick.wwwjdic;

import static org.nick.wwwjdic.WwwjdicPreferences.PREF_KOD_KEY;

import org.nick.wwwjdic.widgets.KodWidgetConfigure;

import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceScreen;

public class WidgetPrefsFragment extends GenericPrefsFragment {

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        String key = preference.getKey();
        if (PREF_KOD_KEY.equals(key)) {
            Intent intent = new Intent(getActivity(), KodWidgetConfigure.class);
            startActivity(intent);

            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
