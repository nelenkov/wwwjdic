package org.nick.wwwjdic;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public class KrPrefsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.kr_prefs, rootKey);
    }

}
