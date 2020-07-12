package org.nick.wwwjdic;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public class GenericPrefsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.misc_prefs, rootKey);
    }

}
