package org.nick.wwwjdic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Window;
import android.widget.TabHost;

public class Wwwjdic extends TabActivity {

    private static final String TAG = Wwwjdic.class.getSimpleName();

    private static final int WHATS_NEW_DIALOG_ID = 1;
    private static final int DONATION_THANKS_DIALOG_ID = 2;

    private static final int DICTIONARY_TAB_IDX = 0;
    private static final String DICTIONARY_TAB = "dictionaryTab";
    private static final int KANJI_TAB_IDX = 1;
    private static final String KANJI_TAB = "kanjiTab";
    private static final int EXAMPLE_SEARRCH_TAB_IDX = 2;
    private static final String EXAMPLE_SEARCH_TAB = "exampleSearchTab";

    private static final String PREF_WHATS_NEW_SHOWN = "pref_whats_new_shown";

    private static final String PREF_AUTO_SELECT_MIRROR_KEY = "pref_auto_select_mirror";
    private static final String PREF_WWWJDIC_URL_KEY = "pref_wwwjdic_mirror_url";

    private static final String DONATE_VERSION_PACKAGE = "org.nick.wwwjdic.donate";
    private static final String PREF_DONATION_THANKS_SHOWN = "pref_donation_thanks_shown";

    private LocationManager locationManager;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.main);

        setupTabs();

        if (isAutoSelectMirror()) {
            setMirrorBasedOnLocation();
        }

        if (!isDonateVersion() || isDonationThanksShown()) {
            showWhatsNew();
        }

        showDonationThanks();
    }

    private boolean isAutoSelectMirror() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        return prefs.getBoolean(PREF_AUTO_SELECT_MIRROR_KEY, true);
    }

    private void setMirrorBasedOnLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location myLocation = locationManager
                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (myLocation == null) {
            Log.d(TAG, "failed to get cached location, giving up");

            return;
        }

        Log.d(TAG, "my location: " + myLocation);
        String[] mirrorCoords = getResources().getStringArray(
                R.array.wwwjdic_mirror_coords);
        String[] mirrorNames = getResources().getStringArray(
                R.array.wwwjdic_mirror_names);
        String[] mirrorUrls = getResources().getStringArray(
                R.array.wwwjdic_mirror_urls);

        List<Float> distanceToMirrors = new ArrayList<Float>(
                mirrorCoords.length);
        for (int i = 0; i < mirrorCoords.length; i++) {
            String[] latlng = mirrorCoords[i].split("/");
            double lat = Location.convert(latlng[0]);
            double lng = Location.convert(latlng[1]);

            float[] distance = new float[1];
            Location.distanceBetween(myLocation.getLatitude(), myLocation
                    .getLongitude(), lat, lng, distance);
            distanceToMirrors.add(distance[0]);
            Log.d(TAG, String.format("distance to %s: %f km", mirrorNames[i],
                    distance[0] / 1000));
        }

        float minDistance = Collections.min(distanceToMirrors);
        int mirrorIdx = distanceToMirrors.indexOf(minDistance);

        Log.d(TAG, String.format(
                "found closest mirror: %s (%s) (distance: %f km)",
                mirrorUrls[mirrorIdx], mirrorNames[mirrorIdx],
                minDistance / 1000));
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        prefs.edit().putString(PREF_WWWJDIC_URL_KEY, mirrorUrls[mirrorIdx])
                .commit();
    }

    private void showDonationThanks() {
        if (!isDonateVersion()) {
            return;
        }

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        String key = PREF_DONATION_THANKS_SHOWN;
        boolean thanksShown = prefs.getBoolean(key, false);
        if (!thanksShown) {
            prefs.edit().putBoolean(key, true).commit();
            showDialog(DONATION_THANKS_DIALOG_ID);
        }
    }

    private boolean isDonationThanksShown() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        return prefs.getBoolean(PREF_DONATION_THANKS_SHOWN, false);
    }

    private boolean isDonateVersion() {
        String appPackage = getApplication().getPackageName();

        return DONATE_VERSION_PACKAGE.equals(appPackage);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Analytics.startSession(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Analytics.endSession(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void showWhatsNew() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        String key = PREF_WHATS_NEW_SHOWN + "_" + getVersionName();
        boolean whatsNewShown = prefs.getBoolean(key, false);
        if (!whatsNewShown) {
            prefs.edit().putBoolean(key, true).commit();
            showDialog(WHATS_NEW_DIALOG_ID);
        }
    }

    private void setupTabs() {
        TabHost tabHost = getTabHost();
        Bundle extras = getIntent().getExtras();

        Intent dictionaryIntent = new Intent(this, Dictionary.class);
        if (extras != null) {
            dictionaryIntent.putExtras(extras);
        }
        tabHost.addTab(tabHost.newTabSpec(DICTIONARY_TAB).setIndicator(
                getResources().getText(R.string.dictionary),
                getResources().getDrawable(R.drawable.ic_tab_dict)).setContent(
                dictionaryIntent));

        Intent kanjiLookup = new Intent(this, KanjiLookup.class);
        if (extras != null) {
            kanjiLookup.putExtras(extras);
        }
        tabHost.addTab(tabHost.newTabSpec(KANJI_TAB).setIndicator(
                getResources().getText(R.string.kanji_lookup),
                getResources().getDrawable(R.drawable.ic_tab_kanji))
                .setContent(kanjiLookup));

        Intent exampleSearch = new Intent(this, ExampleSearch.class);
        if (extras != null) {
            exampleSearch.putExtras(extras);
        }
        tabHost.addTab(tabHost.newTabSpec(EXAMPLE_SEARCH_TAB).setIndicator(
                getResources().getText(R.string.example_search),
                getResources().getDrawable(R.drawable.ic_tab_example))
                .setContent(exampleSearch));

        tabHost.setCurrentTab(DICTIONARY_TAB_IDX);
        if (extras != null) {
            String searchKey = extras.getString(Constants.SEARCH_TEXT_KEY);
            int searchType = extras.getInt(Constants.SEARCH_TYPE);
            if (searchKey != null) {
                switch (searchType) {
                case SearchCriteria.CRITERIA_TYPE_DICT:
                    tabHost.setCurrentTab(DICTIONARY_TAB_IDX);
                    break;
                case SearchCriteria.CRITERIA_TYPE_KANJI:
                    tabHost.setCurrentTab(KANJI_TAB_IDX);
                    break;
                case SearchCriteria.CRITERIA_TYPE_EXAMPLES:
                    tabHost.setCurrentTab(EXAMPLE_SEARRCH_TAB_IDX);
                    break;
                default:
                    // do nothing
                }
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;

        switch (id) {
        case WHATS_NEW_DIALOG_ID:
            dialog = createWhatsNewDialog();
            break;
        case DONATION_THANKS_DIALOG_ID:
            dialog = createDonationThanksDialog();
            break;
        default:
            dialog = null;
        }

        return dialog;
    }

    private Dialog createDonationThanksDialog() {
        DialogInterface.OnClickListener okAction = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                showWhatsNew();

            }
        };
        return createInfoDialog(R.string.donation_thanks_title,
                R.string.donation_thanks, okAction);
    }

    private Dialog createWhatsNewDialog() {
        DialogInterface.OnClickListener okAction = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

            }
        };
        return createInfoDialog(R.string.whats_new_title, R.string.whats_new,
                okAction);
    }

    private Dialog createInfoDialog(int titleId, int messageId,
            DialogInterface.OnClickListener okAction) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String titleTemplate = getResources().getString(titleId);
        String title = String.format(titleTemplate, getVersionName());
        builder.setTitle(title);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setMessage(messageId);
        builder.setPositiveButton(R.string.ok, okAction);

        return builder.create();
    }

    private String getVersionName() {
        return WwwjdicApplication.getVersion();
    }

}
