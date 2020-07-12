package org.nick.wwwjdic;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public class OcrPrefsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.ocr_prefs, rootKey);
    }

}
