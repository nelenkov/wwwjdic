package org.nick.wwwjdic;

import static org.nick.wwwjdic.WwwjdicPreferences.PREF_USE_KR_KEY;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;

public class KrPrefsFragment extends PreferenceFragment implements
        OnPreferenceChangeListener {

    private CheckBoxPreference useKrPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.kr_prefs);

        useKrPreference = (CheckBoxPreference) findPreference(PREF_USE_KR_KEY);
        useKrPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (PREF_USE_KR_KEY.equals(preference.getKey())) {
            Boolean enabled = (Boolean) newValue;
            if (enabled) {
                if (!WwwjdicPreferences.isKrInstalled(getActivity(),
                        getActivity().getApplication())) {
                    WwwjdicPreferences.showInstallKrDialog(getActivity());
                    return false;
                }

                return true;
            }
        }

        return true;
    }
}
