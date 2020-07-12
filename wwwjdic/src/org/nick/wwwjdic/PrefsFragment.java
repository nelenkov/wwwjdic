package org.nick.wwwjdic;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

@SuppressLint("NewApi")
public class PrefsFragment extends PreferenceFragmentCompat  {

    private CheckBoxPreference autoSelectMirrorPreference;
    private ListPreference mirrorPreference;
    private ListPreference defaultDictPreference;
    private ListPreference jpTtsEnginePreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

}
