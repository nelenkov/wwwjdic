package org.nick.wwwjdic;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class WwwjdicPreferencesHC extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback  {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.settings);

        setContentView(R.layout.preferences);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.pref_root, new PrefsFragment())
                .commit();
    }

//    @Override
//    public void onBuildHeaders(List<Header> target) {
//        loadHeadersFromResource(R.xml.pref_headers, target);
//    }


    @Override
    protected void onStart() {
        super.onStart();

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
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
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.pref_root, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }

//    @Override
//    public boolean isValidFragment(String fragment) {
//        if (fragment.equals(WwwjdicPrefsFragment.class.getName()) ||
//            fragment.equals(WidgetPrefsFragment.class.getName()) ||
//            fragment.equals(KrPrefsFragment.class.getName()) ||
//            fragment.equals(GenericPrefsFragment.class.getName())) {
//            return true;
//        }
//
//        return false;
//    }

}
