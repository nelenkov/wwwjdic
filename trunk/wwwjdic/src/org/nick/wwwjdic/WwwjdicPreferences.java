package org.nick.wwwjdic;

import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

public class WwwjdicPreferences extends PreferenceActivity implements
        OnPreferenceChangeListener {

    private static final String TAG = WwwjdicPreferences.class.getSimpleName();

    public static final String PREF_USE_KR_KEY = "pref_kr_use_kanji_recognizer";

    public static final String PREF_AUTO_SELECT_MIRROR_KEY = "pref_auto_select_mirror";
    public static final String PREF_WWWJDIC_MIRROR_URL_KEY = "pref_wwwjdic_mirror_url";
    public static final String DEFAULT_WWWJDIC_URL = "http://www.csse.monash.edu.au/~jwb/cgi-bin/wwwjdic.cgi";

    public static final String PREF_WWWJDIC_TIMEOUT_KEY = "pref_wwwjdic_timeout";

    private static final String KR_PACKAGE = "org.nick.kanjirecognizer";

    private static final String PREF_DEFAULT_DICT_PREF_KEY = "pref_default_dict";

    private static final String PREF_EXPORT_MEANINGS_SEPARATOR_CHAR = "pref_export_meanings_separator_char";

    public static final String PREF_KR_URL_KEY = "pref_kr_url";
    public static final String KR_DEFAULT_URL = "http://kanji.sljfaq.org/kanji-0.016.cgi";
    private static final String PREF_KR_TIMEOUT_KEY = "pref_kr_timeout";
    private static final String PREF_KR_ANNOTATE = "pref_kr_annotate";
    private static final String PREF_KR_ANNOTATE_MIDWAY = "pref_kr_annotate_midway";
    private static final String PREF_KR_USE_KANJI_RECOGNIZER_KEY = "pref_kr_use_kanji_recognizer";

    private static final String WEOCR_DEFAULT_URL = "http://maggie.ocrgrid.org/cgi-bin/weocr/nhocr.cgi";
    private static final String PREF_DUMP_CROPPED_IMAGES_KEY = "pref_ocr_dump_cropped_images";
    private static final String PREF_WEOCR_URL_KEY = "pref_weocr_url";
    private static final String PREF_WEOCR_TIMEOUT_KEY = "pref_weocr_timeout";

    private static final String PREF_DIRECT_SEARCH_KEY = "pref_ocr_direct_search";

    private static final String PREF_SOD_ANIMATION_DELAY = "pref_sod_animation_delay";
    private static final String PREF_SOD_TIMEOUT = "pref_sod_server_timeout";

    private static final String PREF_ACCOUNT_NAME_KEY = "pref_account_name";

    private static final String PREF_ENABLE_ANALYTICS_KEY = "pref_enable_analytics";

    public static final String PREF_WHATS_NEW_SHOWN = "pref_whats_new_shown";
    public static final String PREF_DONATION_THANKS_SHOWN = "pref_donation_thanks_shown";

    public static final String PREF_TIP_SHOWN = "pref_tip_shown";

    public static final String PREF_KOD_LEVEL1_ONLY_KEY = "pref kod_level_one_only";
    public static final String PREF_KOD_SHOW_READING_KEY = "pref_kod_show_reading";
    public static final String PREF_KOD_UPDATE_INTERAVL_KEY = "pref_kod_update_interval";

    public static final long KOD_DEFAULT_UPDATE_INTERVAL = 24 * DateUtils.HOUR_IN_MILLIS;

    public static final String DEFAULT_STROKE_ANIMATION_DELAY = "150";

    private CheckBoxPreference useKrPreference;
    private CheckBoxPreference autoSelectMirrorPreference;
    private ListPreference mirrorPreference;
    private ListPreference defaultDictPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.settings);
        addPreferencesFromResource(R.xml.preferences);

        useKrPreference = (CheckBoxPreference) findPreference(PREF_USE_KR_KEY);
        useKrPreference.setOnPreferenceChangeListener(this);

        autoSelectMirrorPreference = (CheckBoxPreference) findPreference(PREF_AUTO_SELECT_MIRROR_KEY);
        autoSelectMirrorPreference.setOnPreferenceChangeListener(this);

        mirrorPreference = (ListPreference) findPreference(PREF_WWWJDIC_MIRROR_URL_KEY);
        mirrorPreference.setSummary(mirrorPreference.getEntry());
        mirrorPreference.setOnPreferenceChangeListener(this);

        defaultDictPreference = (ListPreference) findPreference(PREF_DEFAULT_DICT_PREF_KEY);
        defaultDictPreference.setSummary(defaultDictPreference.getEntry());
        defaultDictPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (PREF_USE_KR_KEY.equals(preference.getKey())) {
            Boolean enabled = (Boolean) newValue;
            if (enabled) {
                if (!isKrInstalled()) {
                    showInstallKrDialog();
                    return false;
                }

                return true;
            }
        }

        if (PREF_AUTO_SELECT_MIRROR_KEY.equals(preference.getKey())) {
            boolean autoSelect = (Boolean) newValue;
            if (autoSelect) {
                WwwjdicApplication app = (WwwjdicApplication) getApplication();
                app.setMirrorBasedOnLocation();
                mirrorPreference.setSummary(getMirrorName(getWwwjdicUrl(this)));
            }

            return true;
        }

        if (PREF_WWWJDIC_MIRROR_URL_KEY.equals(preference.getKey())) {
            preference.setSummary(getMirrorName((String) newValue));
        }

        if (PREF_DEFAULT_DICT_PREF_KEY.equals(preference.getKey())) {
            preference.setSummary(getDictionaryName(Integer
                    .valueOf((String) newValue)));
        }

        return true;
    }

    private String getMirrorName(String url) {
        Resources r = getResources();
        List<String> mirrorUrls = Arrays.asList(r
                .getStringArray(R.array.wwwjdic_mirror_urls));
        String[] mirrorNames = r.getStringArray(R.array.wwwjdic_mirror_names);
        int idx = mirrorUrls.indexOf(url);
        if (idx != -1) {
            return mirrorNames[idx];
        }

        return "";
    }

    private void showInstallKrDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.install_kr)
                .setCancelable(false)
                .setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent intent = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id="
                                                + KR_PACKAGE));
                                startActivity(intent);
                            }
                        })
                .setNegativeButton(R.string.no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private boolean isKrInstalled() {
        Log.d(TAG, "Checking for Kanji Recognizer...");
        PackageManager pm = getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(KR_PACKAGE, 0);
            Log.d(TAG, String.format("Found KR: %s, version %s(%d)",
                    pi.packageName, pi.versionName, pi.versionCode));
            if (pi.versionCode < 2) {
                Log.d(TAG, String.format(
                        "Kanji recognizer %s is installed, but we need 1.1",
                        pi.versionName));
                return false;
            }

            String myPackageName = getApplication().getPackageName();
            Log.d(TAG, String.format("Checking for signature match: "
                    + "my package = %s, KR package = %s", myPackageName,
                    pi.packageName));
            boolean result = pm.checkSignatures(myPackageName, pi.packageName) == PackageManager.SIGNATURE_MATCH;
            Log.d(TAG, "signature match: " + result);

            return result;
        } catch (NameNotFoundException e) {
            Log.w(TAG, "Kanji Recognizer not found", e);
            return false;
        }
    }

    public static int getDefaultDictionaryIdx(Context context) {
        SharedPreferences preferences = getPrefs(context);

        String idxStr = preferences.getString(PREF_DEFAULT_DICT_PREF_KEY, "0");

        return Integer.parseInt(idxStr);
    }

    public static String getDefaultDictionary(Context context) {
        String[] dictionaries = context.getResources().getStringArray(
                R.array.dictionary_codes_array);

        return dictionaries[getDefaultDictionaryIdx(context)];
    }

    private String getDictionaryName(int dictIdx) {
        String[] dictionaryNames = getResources().getStringArray(
                R.array.dictionaries_array);

        if (dictIdx >= 0 && dictIdx < dictionaryNames.length) {
            return dictionaryNames[dictIdx];
        }

        return "";
    }

    public static String getMeaningsSeparatorCharacter(Context context) {
        SharedPreferences preferences = getPrefs(context);

        return preferences.getString(PREF_EXPORT_MEANINGS_SEPARATOR_CHAR, "\n");
    }

    public static String getWwwjdicUrl(Context context) {
        SharedPreferences prefs = getPrefs(context);

        return prefs
                .getString(PREF_WWWJDIC_MIRROR_URL_KEY, DEFAULT_WWWJDIC_URL);
    }

    public static void setWwwjdicUrl(String url, Context context) {
        SharedPreferences.Editor editor = getPrefsEditor(context);

        editor.putString(PREF_WWWJDIC_MIRROR_URL_KEY, url);
        editor.commit();
    }

    public static int getWwwjdicTimeoutSeconds(Context context) {
        SharedPreferences preferences = getPrefs(context);

        String timeoutStr = preferences.getString(PREF_WWWJDIC_TIMEOUT_KEY,
                "10");

        return Integer.parseInt(timeoutStr);
    }

    public static int getKrTimeout(Context context) {
        SharedPreferences preferences = getPrefs(context);

        String timeoutStr = preferences.getString(PREF_KR_TIMEOUT_KEY, "10");

        return Integer.parseInt(timeoutStr) * 1000;
    }

    public static String getKrUrl(Context context) {
        SharedPreferences preferences = getPrefs(context);

        return preferences.getString(PREF_KR_URL_KEY, KR_DEFAULT_URL);
    }

    public static boolean isAnnotateStrokesMidway(Context context) {
        SharedPreferences preferences = getPrefs(context);

        return preferences.getBoolean(PREF_KR_ANNOTATE_MIDWAY, false);
    }

    public static boolean isAnnoateStrokes(Context context) {
        SharedPreferences preferences = getPrefs(context);

        return preferences.getBoolean(PREF_KR_ANNOTATE, true);
    }

    public static boolean isUseKanjiRecognizer(Context context) {
        SharedPreferences preferences = getPrefs(context);

        return preferences.getBoolean(PREF_KR_USE_KANJI_RECOGNIZER_KEY, false);
    }

    public static int getWeocrTimeout(Context context) {
        SharedPreferences preferences = getPrefs(context);

        String timeoutStr = preferences.getString(PREF_WEOCR_TIMEOUT_KEY, "10");

        return Integer.parseInt(timeoutStr) * 1000;
    }

    public static String getWeocrUrl(Context context) {
        SharedPreferences preferences = getPrefs(context);

        return preferences.getString(PREF_WEOCR_URL_KEY, WEOCR_DEFAULT_URL);
    }

    public static boolean isDumpCroppedImages(Context context) {
        SharedPreferences preferences = getPrefs(context);

        return preferences.getBoolean(PREF_DUMP_CROPPED_IMAGES_KEY, false);
    }

    public static boolean isDirectSearch(Context context) {
        SharedPreferences preferences = getPrefs(context);

        return preferences.getBoolean(PREF_DIRECT_SEARCH_KEY, false);
    }

    public static int getStrokeAnimationDelay(Context context) {
        SharedPreferences preferences = getPrefs(context);

        String delayStr = preferences
                .getString(PREF_SOD_ANIMATION_DELAY, DEFAULT_STROKE_ANIMATION_DELAY);

        return Integer.parseInt(delayStr);
    }

    public static void setStrokeAnimationDelay(Context context,
            String delayMillisStr) {
        SharedPreferences.Editor editor = getPrefsEditor(context);
        editor.putString(PREF_SOD_ANIMATION_DELAY, delayMillisStr);
        editor.commit();
    }

    public static int getSodServerTimeout(Context context) {
        SharedPreferences preferences = getPrefs(context);

        String delayStr = preferences.getString(PREF_SOD_TIMEOUT, "30");

        return Integer.parseInt(delayStr) * 1000;
    }

    public static String getGoogleAcountName(Context context) {
        SharedPreferences settings = getPrefs(context);

        return settings.getString(PREF_ACCOUNT_NAME_KEY, null);
    }

    public static synchronized void setGoogleAccountName(Context context,
            String accountName) {
        SharedPreferences settings = getPrefs(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_ACCOUNT_NAME_KEY, accountName);
        editor.commit();
    }

    public static boolean isAnalyticsEnabled(Context ctx) {
        SharedPreferences preferences = getPrefs(ctx);

        return preferences.getBoolean(PREF_ENABLE_ANALYTICS_KEY, true);
    }

    public static boolean isDonationThanksShown(Context context) {
        SharedPreferences prefs = getPrefs(context);

        return prefs.getBoolean(PREF_DONATION_THANKS_SHOWN, false);
    }

    public static synchronized void setDonationThanksShown(Context context) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().putBoolean(PREF_DONATION_THANKS_SHOWN, true).commit();
    }

    public static boolean isWhatsNewShown(Context context, String versionName) {
        SharedPreferences prefs = getPrefs(context);
        String key = WwwjdicPreferences.PREF_WHATS_NEW_SHOWN + "_"
                + versionName;
        return prefs.getBoolean(key, false);
    }

    public static synchronized void setWhantsNewShown(Context context,
            String versionName) {
        SharedPreferences prefs = getPrefs(context);
        String key = WwwjdicPreferences.PREF_WHATS_NEW_SHOWN + "_"
                + versionName;
        prefs.edit().putBoolean(key, true).commit();
    }

    public static boolean isTipShown(Context context, String tipKey) {
        SharedPreferences prefs = getPrefs(context);
        String key = PREF_TIP_SHOWN + "_" + tipKey;

        return prefs.getBoolean(key, false);
    }

    public static void setTipShown(Context context, String tipKey) {
        SharedPreferences prefs = getPrefs(context);
        String key = PREF_TIP_SHOWN + "_" + tipKey;
        prefs.edit().putBoolean(key, true).commit();
    }

    private static void saveBooleanPref(Context context, String key,
            boolean value) {
        SharedPreferences.Editor editor = getPrefsEditor(context);
        editor.putBoolean(key, value);
        editor.commit();
    }

    private static SharedPreferences.Editor getPrefsEditor(Context context) {
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(context).edit();
        return editor;
    }

    private static boolean getBooleanPref(Context context, String key,
            boolean defValue) {
        SharedPreferences prefs = getPrefs(context);

        return prefs.getBoolean(key, defValue);
    }

    private static SharedPreferences getPrefs(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        return prefs;
    }

    public static void setKodLevelOneOnly(Context context, boolean levelOneOnly) {
        saveBooleanPref(context, PREF_KOD_LEVEL1_ONLY_KEY, levelOneOnly);
    }

    public static void setKodShowReading(Context context, boolean showReading) {
        saveBooleanPref(context, PREF_KOD_SHOW_READING_KEY, showReading);
    }

    public static boolean isKodLevelOneOnly(Context context) {
        return getBooleanPref(context, PREF_KOD_LEVEL1_ONLY_KEY, false);
    }

    public static boolean isKodShowReading(Context context) {
        return getBooleanPref(context, PREF_KOD_SHOW_READING_KEY, false);
    }

    public static void setKodUpdateInterval(Context context,
            long updateIntervalMillis) {
        SharedPreferences.Editor editor = getPrefsEditor(context);
        editor.putLong(PREF_KOD_UPDATE_INTERAVL_KEY, updateIntervalMillis);
        editor.commit();
    }

    public static long getKodUpdateInterval(Context context) {
        return getPrefs(context).getLong(PREF_KOD_UPDATE_INTERAVL_KEY,
                KOD_DEFAULT_UPDATE_INTERVAL);
    }
}
