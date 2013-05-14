package org.nick.wwwjdic;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;
import org.nick.wwwjdic.model.Radicals;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.location.LocationManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

@ReportsCrashes(formKey = "dummy", mode = ReportingInteractionMode.TOAST)
public class WwwjdicApplication extends Application {

    private static final String TAG = WwwjdicApplication.class.getSimpleName();

    private static WwwjdicApplication instance;

    private static final String WWWJDIC_DIR = "wwwjdic";

    private static final String OLD_JAPAN_MIRROR = "http://www.aa.tufs.ac.jp/~jwb/cgi-bin/wwwjdic.cgi";

    private static final String OLD_MYGENGO_MIRROR = "http://wwwjdic.mygengo.com/cgi-data/wwwjdic";
    private static final String OLD_MYGENGO_MIRROR2 = "http://mygengo.com/wwwjdic/cgi-data/wwwjdic";

    private static final String NEW_JAPAN_MIRROR = WwwjdicPreferences.DEFAULT_WWWJDIC_URL;

    private ExecutorService executorService;

    private LocationManager locationManager;

    private static String version;

    // EDICT by default
    private String currentDictionary = "1";
    private String currentDictionaryName = "General";

    public static WwwjdicApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        instance = this;

        version = getVersionName();

        initAcra();

        createWwwjdicDirIfNecessary();

        updateJapanMirror();

        initRadicals();

        if (isAutoSelectMirror()) {
            try {
                setMirrorBasedOnLocation();
            } catch (Exception e) {
                // workaround: parsing mirror coords fails on Xperia (SO-01B)
                // with
                // StringIndexOutOfBoundsException
                Log.w(TAG,
                        "Failed to set mirror based on location, setting default");
                setDefaultMirror();
            }
        }

        updateKanjiRecognizerUrl();

        WwwjdicPreferences.setStrokeAnimationDelay(this,
                WwwjdicPreferences.DEFAULT_STROKE_ANIMATION_DELAY);
    }

    private void initAcra() {
        if (isDebug()) {
            return;
        }

        try {
            ACRAConfiguration config = ACRA.getNewDefaultConfig(this);
            config.setResToastText(R.string.crash_toast_text);
            //            config.setSendReportsInDevMode(true);
            ACRA.setConfig(config);
            ACRA.init(this);

            String bugsenseUrl = getResources()
                    .getString(R.string.bugsense_url);
            HttpSender bugSenseSender = new HttpSender(HttpSender.Method.POST,
                    HttpSender.Type.FORM, bugsenseUrl, null);
            ACRA.getErrorReporter().setReportSender(bugSenseSender);
        } catch (IllegalStateException e) {
            Log.w(TAG, "ACRA.init() called more than once?: " + e.getMessage(),
                    e);
        }
    }

    public boolean isDebug() {
        return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;
    }

    private void updateJapanMirror() {
        String mirrorUlr = WwwjdicPreferences.getWwwjdicUrl(this);
        if (OLD_JAPAN_MIRROR.equals(mirrorUlr)) {
            WwwjdicPreferences.setWwwjdicUrl(NEW_JAPAN_MIRROR, this);
        }
        if (OLD_MYGENGO_MIRROR.equals(mirrorUlr)
                || OLD_MYGENGO_MIRROR2.equals(mirrorUlr)) {
            WwwjdicPreferences.setWwwjdicUrl(NEW_JAPAN_MIRROR, this);
        }
    }

    private void updateKanjiRecognizerUrl() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        String krUrl = prefs.getString(WwwjdicPreferences.PREF_KR_URL_KEY,
                WwwjdicPreferences.KR_DEFAULT_URL);
        if (krUrl.contains("kanji.cgi")) {
            Log.d(TAG, "found old KR URL, will overwrite with "
                    + WwwjdicPreferences.KR_DEFAULT_URL);
            prefs.edit()
                    .putString(WwwjdicPreferences.PREF_KR_URL_KEY,
                            WwwjdicPreferences.KR_DEFAULT_URL).commit();
        }
    }

    private boolean isAutoSelectMirror() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        return prefs.getBoolean(WwwjdicPreferences.PREF_AUTO_SELECT_MIRROR_KEY,
                true);
    }

    public synchronized void setMirrorBasedOnLocation() {
        Log.d(TAG, "auto selecting mirror...");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location myLocation = null;

        try {
            boolean isEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            Log.d(TAG, LocationManager.NETWORK_PROVIDER + " enabled: "
                    + isEnabled);
            if (!isEnabled) {
                Log.d(TAG, "provider not enabled, giving up");
                return;
            }
            myLocation = locationManager
                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (myLocation == null) {
                Log.w(TAG, "failed to get cached location, giving up");

                return;
            }
        } catch (Exception e) {
            Log.w(TAG, "error getting location, giving up", e);

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
            Location.distanceBetween(myLocation.getLatitude(),
                    myLocation.getLongitude(), lat, lng, distance);
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
        prefs.edit()
                .putString(WwwjdicPreferences.PREF_WWWJDIC_MIRROR_URL_KEY,
                        mirrorUrls[mirrorIdx]).commit();
    }

    private void setDefaultMirror() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        prefs.edit()
                .putString(WwwjdicPreferences.PREF_WWWJDIC_MIRROR_URL_KEY,
                        WwwjdicPreferences.DEFAULT_WWWJDIC_URL).commit();
    }

    private void createWwwjdicDirIfNecessary() {
        File wwwjdicDir = getWwwjdicDir();
        if (!wwwjdicDir.exists()) {
            boolean success = wwwjdicDir.mkdir();
            if (success) {
                Log.d(TAG,
                        "successfully created " + wwwjdicDir.getAbsolutePath());
            } else {
                Log.d(TAG, "failed to create " + wwwjdicDir.getAbsolutePath());
            }
        }
    }

    public static File getWwwjdicDir() {
        return new File(Environment.getExternalStorageDirectory(), WWWJDIC_DIR);
    }

    private String getVersionName() {
        try {
            PackageInfo pinfo = getPackageManager().getPackageInfo(
                    getPackageName(), 0);

            return pinfo.versionName;
        } catch (NameNotFoundException e) {
            return "";
        }
    }

    public WwwjdicApplication() {
        executorService = Executors.newSingleThreadExecutor();
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public static String getVersion() {
        return version;
    }

    public static String getUserAgentString() {
        return "Android-WWWJDIC/" + getVersion();
    }

    private void initRadicals() {
        Radicals radicals = Radicals.getInstance();
        if (!radicals.isInitialized()) {
            radicals.addRadicals(1,
                    getIntArray(R.array.one_stroke_radical_numbers),
                    getStrArray(R.array.one_stroke_radicals));
            radicals.addRadicals(2,
                    getIntArray(R.array.two_stroke_radical_numbers),
                    getStrArray(R.array.two_stroke_radicals));
            radicals.addRadicals(3,
                    getIntArray(R.array.three_stroke_radical_numbers),
                    getStrArray(R.array.three_stroke_radicals));
            radicals.addRadicals(4,
                    getIntArray(R.array.four_stroke_radical_numbers),
                    getStrArray(R.array.four_stroke_radicals));
            radicals.addRadicals(5,
                    getIntArray(R.array.five_stroke_radical_numbers),
                    getStrArray(R.array.five_stroke_radicals));
            radicals.addRadicals(6,
                    getIntArray(R.array.six_stroke_radical_numbers),
                    getStrArray(R.array.six_stroke_radicals));
            radicals.addRadicals(7,
                    getIntArray(R.array.seven_stroke_radical_numbers),
                    getStrArray(R.array.seven_stroke_radicals));
            radicals.addRadicals(8,
                    getIntArray(R.array.eight_stroke_radical_numbers),
                    getStrArray(R.array.eight_stroke_radicals));
            radicals.addRadicals(9,
                    getIntArray(R.array.nine_stroke_radical_numbers),
                    getStrArray(R.array.nine_stroke_radicals));
            radicals.addRadicals(10,
                    getIntArray(R.array.ten_stroke_radical_numbers),
                    getStrArray(R.array.ten_stroke_radicals));
            radicals.addRadicals(11,
                    getIntArray(R.array.eleven_stroke_radical_numbers),
                    getStrArray(R.array.eleven_stroke_radicals));
            radicals.addRadicals(12,
                    getIntArray(R.array.twelve_stroke_radical_numbers),
                    getStrArray(R.array.twelve_stroke_radicals));
            radicals.addRadicals(13,
                    getIntArray(R.array.thirteen_stroke_radical_numbers),
                    getStrArray(R.array.thirteen_stroke_radicals));
            radicals.addRadicals(14,
                    getIntArray(R.array.fourteen_stroke_radical_numbers),
                    getStrArray(R.array.fourteen_stroke_radicals));
            radicals.addRadicals(15,
                    getIntArray(R.array.fivteen_stroke_radical_numbers),
                    getStrArray(R.array.fivteen_stroke_radicals));
            radicals.addRadicals(16,
                    getIntArray(R.array.sixteen_stroke_radical_numbers),
                    getStrArray(R.array.sixteen_stroke_radicals));
            radicals.addRadicals(17,
                    getIntArray(R.array.seventeen_stroke_radical_numbers),
                    getStrArray(R.array.seventeen_stroke_radicals));
        }
    }

    private int[] getIntArray(int id) {
        return getResources().getIntArray(id);
    }

    private String[] getStrArray(int id) {
        return getResources().getStringArray(id);
    }

    public synchronized String getCurrentDictionary() {
        return currentDictionary;
    }

    public synchronized void setCurrentDictionary(String currentDictionary) {
        this.currentDictionary = currentDictionary;
    }

    public synchronized String getCurrentDictionaryName() {
        return currentDictionaryName;
    }

    public synchronized void setCurrentDictionaryName(
            String currentDictionaryName) {
        this.currentDictionaryName = currentDictionaryName;
    }

}
