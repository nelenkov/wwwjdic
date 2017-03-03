package org.nick.wwwjdic;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import java.util.List;

public class WwwjdicPreferencesHC extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.settings);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }


    @Override
    protected void onStart() {
        super.onStart();

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Activities.home(this);
            return true;
        default:
            // do nothing
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean isValidFragment(String fragment) {
        if (fragment.equals(WwwjdicPrefsFragment.class.getName()) ||
            fragment.equals(WidgetPrefsFragment.class.getName()) ||
            fragment.equals(KrPrefsFragment.class.getName()) ||
            fragment.equals(GenericPrefsFragment.class.getName())) {
            return true;
        }

        return false;
    }

}
