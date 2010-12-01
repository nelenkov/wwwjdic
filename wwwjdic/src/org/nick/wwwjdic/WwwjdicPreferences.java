package org.nick.wwwjdic;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class WwwjdicPreferences extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.settings);
        addPreferencesFromResource(R.xml.preferences);
    }
}
