package org.nick.wwwjdic;

import android.os.Bundle;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class GenericPrefsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.misc_prefs, rootKey);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if ("acra.enable".equals(preference.getKey())) {
            boolean enabled = (boolean)newValue;
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled);
        }

        return true;
    }

}
