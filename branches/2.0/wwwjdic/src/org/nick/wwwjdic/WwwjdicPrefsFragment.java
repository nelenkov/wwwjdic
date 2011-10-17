package org.nick.wwwjdic;

import static org.nick.wwwjdic.WwwjdicPreferences.PREF_AUTO_SELECT_MIRROR_KEY;
import static org.nick.wwwjdic.WwwjdicPreferences.PREF_DEFAULT_DICT_PREF_KEY;
import static org.nick.wwwjdic.WwwjdicPreferences.PREF_JP_TTS_ENGINE;
import static org.nick.wwwjdic.WwwjdicPreferences.PREF_WWWJDIC_MIRROR_URL_KEY;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;

public class WwwjdicPrefsFragment extends PreferenceFragment implements
        OnPreferenceChangeListener {

    private CheckBoxPreference autoSelectMirrorPreference;
    private ListPreference mirrorPreference;
    private ListPreference defaultDictPreference;
    private ListPreference jpTtsEnginePreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.wwwjdic_prefs);

        autoSelectMirrorPreference = (CheckBoxPreference) findPreference(PREF_AUTO_SELECT_MIRROR_KEY);
        autoSelectMirrorPreference.setOnPreferenceChangeListener(this);

        mirrorPreference = (ListPreference) findPreference(PREF_WWWJDIC_MIRROR_URL_KEY);
        mirrorPreference.setSummary(mirrorPreference.getEntry());
        mirrorPreference.setOnPreferenceChangeListener(this);

        defaultDictPreference = (ListPreference) findPreference(PREF_DEFAULT_DICT_PREF_KEY);
        defaultDictPreference.setSummary(defaultDictPreference.getEntry());
        defaultDictPreference.setOnPreferenceChangeListener(this);

        jpTtsEnginePreference = (ListPreference) findPreference(PREF_JP_TTS_ENGINE);
        jpTtsEnginePreference.setSummary(WwwjdicPreferences.getTtsEngineName(
                getActivity(), jpTtsEnginePreference.getEntry().toString()));
        jpTtsEnginePreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (PREF_AUTO_SELECT_MIRROR_KEY.equals(preference.getKey())) {
            boolean autoSelect = (Boolean) newValue;
            if (autoSelect) {
                WwwjdicApplication.getInstance().setMirrorBasedOnLocation();
                mirrorPreference.setSummary(WwwjdicPreferences.getMirrorName(
                        getActivity(),
                        WwwjdicPreferences.getWwwjdicUrl(getActivity())));
            }

            return true;
        }

        if (PREF_WWWJDIC_MIRROR_URL_KEY.equals(preference.getKey())) {
            preference.setSummary(WwwjdicPreferences.getMirrorName(
                    getActivity(), (String) newValue));
        }

        if (PREF_DEFAULT_DICT_PREF_KEY.equals(preference.getKey())) {
            preference.setSummary(WwwjdicPreferences.getDictionaryName(
                    getActivity(), Integer.valueOf((String) newValue)));
        }

        if (PREF_JP_TTS_ENGINE.equals(preference.getKey())) {
            preference.setSummary(WwwjdicPreferences.getTtsEngineName(
                    getActivity(), (String) newValue));
        }

        return true;

    }
}
