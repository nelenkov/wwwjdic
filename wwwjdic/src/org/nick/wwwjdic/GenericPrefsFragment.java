package org.nick.wwwjdic;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class GenericPrefsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int res = getActivity().getResources().getIdentifier(
                getArguments().getString("resource"), "xml",
                getActivity().getPackageName());

        addPreferencesFromResource(res);
    }

    protected void loadFromArgsResource() {
        int res = getActivity().getResources().getIdentifier(
                getArguments().getString("resource"), "xml",
                getActivity().getPackageName());
    
        addPreferencesFromResource(res);
    }
}
