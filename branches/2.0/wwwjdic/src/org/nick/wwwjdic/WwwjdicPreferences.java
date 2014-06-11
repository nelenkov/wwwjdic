package org.nick.wwwjdic;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
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
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.format.DateUtils;
import android.util.Log;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

import org.nick.wwwjdic.widgets.KodWidgetConfigure;
import org.nick.wwwjdic.widgets.KodWidgetProvider;

import java.util.Arrays;
import java.util.List;

public class WwwjdicPreferences extends SherlockPreferenceActivity implements
        OnPreferenceChangeListener {

    private static final String TAG = WwwjdicPreferences.class.getSimpleName();

    public static final boolean ACRA_DEBUG = false;
    public static final boolean WWWJDIC_DEBUG = false;

    public static final String PREF_USE_KR_KEY = "pref_kr_use_kanji_recognizer";

    public static final String PREF_AUTO_SELECT_MIRROR_KEY = "pref_auto_select_mirror";
    public static final String PREF_WWWJDIC_MIRROR_URL_KEY = "pref_wwwjdic_mirror_url";
    public static final String OLD_DEFAULT_WWWJDIC_URL = "http://mygengo.com/wwwjdic/cgi-data/wwwjdic";
    public static final String DEFAULT_WWWJDIC_URL = "http://gengo.com/wwwjdic/cgi-data/wwwjdic";

    public static final String PREF_WWWJDIC_TIMEOUT_KEY = "pref_wwwjdic_timeout";
    private static final int WWWJDIC_TIMEOUT_DEFAULT = 10 * 1000;

    public static final String KR_PACKAGE = "org.nick.kanjirecognizer";

    public static final String PREF_DEFAULT_DICT_PREF_KEY = "pref_default_dict";

    private static final String PREF_EXPORT_MEANINGS_SEPARATOR_CHAR = "pref_export_meanings_separator_char";

    public static final String PREF_KR_URL_KEY = "pref_kr_url";
    public static final String KR_DEFAULT_URL = "http://kanji.sljfaq.org/kanji-0.016.cgi";
    private static final String PREF_KR_TIMEOUT_KEY = "pref_kr_timeout";
    private static final int KR_TIMEOUT_DEFAULT = 10 * 1000;
    private static final String PREF_KR_ANNOTATE = "pref_kr_annotate";
    private static final String PREF_KR_ANNOTATE_MIDWAY = "pref_kr_annotate_midway";
    private static final String PREF_KR_USE_KANJI_RECOGNIZER_KEY = "pref_kr_use_kanji_recognizer";

    private static final String WEOCR_DEFAULT_URL = "http://maggie.ocrgrid.org/cgi-bin/weocr/nhocr.cgi";
    private static final String PREF_DUMP_CROPPED_IMAGES_KEY = "pref_ocr_dump_cropped_images";
    private static final String PREF_WEOCR_URL_KEY = "pref_weocr_url";
    private static final String PREF_WEOCR_TIMEOUT_KEY = "pref_weocr_timeout";
    private static final int WEOCR_TIMEOUT_DEFAULT = 10 * 1000;

    private static final String PREF_DIRECT_SEARCH_KEY = "pref_ocr_direct_search";

    private static final String PREF_SOD_ANIMATION_DELAY = "pref_sod_animation_delay";
    private static final String PREF_SOD_TIMEOUT = "pref_sod_server_timeout";
    private static final int SOD_TIMEOUT_DEFAULT = 30 * 1000;

    private static final String PREF_ACCOUNT_NAME_KEY = "pref_account_name";

    public static final String PREF_WHATS_NEW_SHOWN = "pref_whats_new_shown";
    public static final String PREF_DONATION_THANKS_SHOWN = "pref_donation_thanks_shown";

    public static final String PREF_TIP_SHOWN = "pref_tip_shown";

    private static final String PREF_KOD_LEVEL1_ONLY_KEY = "pref kod_level_one_only";
    private static final String PREF_KOD_USE_JLPT_KEY = "pref_kod_use_jlpt";
    private static final String PREF_KOD_JLPT_LEVEL = "prf_kod_jlpt_level";
    private static final String PREF_KOD_SHOW_READING_KEY = "pref_kod_show_reading";
    private static final String PREF_KOD_UPDATE_INTERAVL_KEY = "pref_kod_update_interval";

    private static final String PREF_WANTS_TTS_KEY = "pref_wants_tts";

    public static final long KOD_DEFAULT_UPDATE_INTERVAL = 24 * DateUtils.HOUR_IN_MILLIS;

    private static final String PREF_LAST_KOD_UPDATE_ERROR_KEY = "pref_last_kod_update_error";

    public static final String DEFAULT_STROKE_ANIMATION_DELAY = "150";

    private static final String PREF_RANDOM_EXAMPLES_KEY = "pref_random_examples";

    static final String PREF_JP_TTS_ENGINE = "pref_jp_tts_engine";
    static final String DEFAULT_JP_TTS_ENGINE_PACKAGE = "jp.kddilabs.n2tts";

    static final String PREF_KOD_KEY = "pref_kod";

    private static final String PREF_KOD_IS_RANDOM_KEY = "kod_is_random";
    private static final String PREF_KOD_CURRENT_KANJI_KEY = "kod_current_kanji";
    private static final String PREF_KOD_TRANSPARENT_BG_KEY = "kod_transparent_bg";

    private static final String PREF_SELECTED_DICTIONARY = "selected_dictionary";
    private static final String PREF_KANJI_SEARCH_TYPE_KEY = "kanji_search_type";
    private static final String PREF_SENTENCE_MODE_KEY = "sentence_mode";

    private static final String PREF_POPUP_KEYBOARD_KEY = "pref_popup_keyboard";

    private CheckBoxPreference useKrPreference;
    private CheckBoxPreference autoSelectMirrorPreference;
    private ListPreference mirrorPreference;
    private ListPreference defaultDictPreference;
    private ListPreference jpTtsEnginePreference;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.settings);

        addPreferencesFromResource(R.xml.wwwjdic_prefs);
        addPreferencesFromResource(R.xml.ocr_prefs);
        addPreferencesFromResource(R.xml.kr_prefs);
        addPreferencesFromResource(R.xml.widget_prefs);
        addPreferencesFromResource(R.xml.misc_prefs);

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

        jpTtsEnginePreference = (ListPreference) findPreference(PREF_JP_TTS_ENGINE);
        jpTtsEnginePreference.setSummary(getTtsEngineName(this,
                jpTtsEnginePreference.getValue()));
        jpTtsEnginePreference.setOnPreferenceChangeListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onResume() {
        super.onResume();

        AppWidgetManager wm = AppWidgetManager.getInstance(this);
        ComponentName kodWidget = new ComponentName(this,
                KodWidgetProvider.class);
        int[] ids = wm.getAppWidgetIds(kodWidget);
        boolean hasWidgets = ids != null && ids.length > 0;
        findPreference(PREF_KOD_KEY).setEnabled(hasWidgets);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (PREF_USE_KR_KEY.equals(preference.getKey())) {
            Boolean enabled = (Boolean) newValue;
            if (enabled) {
                if (!isKrInstalled(this, getApplication())) {
                    showInstallKrDialog();
                    return false;
                }

                return true;
            }
        }

        if (PREF_AUTO_SELECT_MIRROR_KEY.equals(preference.getKey())) {
            boolean autoSelect = (Boolean) newValue;
            if (autoSelect) {
                WwwjdicApplication.getInstance().setMirrorBasedOnLocation();
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

        if (PREF_JP_TTS_ENGINE.equals(preference.getKey())) {
            preference.setSummary(getTtsEngineName(this, (String) newValue));
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        String key = preference.getKey();
        if (PREF_KOD_KEY.equals(key)) {
            Intent intent = new Intent(this, KodWidgetConfigure.class);
            // anything but 0 should do
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 1);
            // configure activity will launch the update service, so
            // we don't really care about the result
            startActivity(intent);

            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    static String getTtsEngineName(Context ctx, String enginePackage) {
        return matchNameToValue(ctx, R.array.jp_tts_engine_names,
                R.array.jp_tts_engine_packages, enginePackage);
    }

    private static String matchNameToValue(Context ctx, int nameArrayResId,
            int valueArrayResId, String value) {
        Resources r = ctx.getResources();
        String[] names = r.getStringArray(nameArrayResId);
        List<String> values = Arrays.asList(r.getStringArray(valueArrayResId));
        int idx = values.indexOf(value);
        if (idx != -1) {
            return names[idx];
        }

        return "";
    }

    private String getMirrorName(String url) {
        return getMirrorName(this, url);
    }

    static String getMirrorName(Context ctx, String url) {
        return matchNameToValue(ctx, R.array.wwwjdic_mirror_names,
                R.array.wwwjdic_mirror_urls, url);
    }

    private void showInstallKrDialog() {
        showInstallKrDialog(this);
    }

    public static void showInstallKrDialog(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(R.string.install_kr)
                .setCancelable(false)
                .setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String kanjiRecognizerUri = activity.getResources()
                                        .getString(R.string.kr_download_uri);
                                Intent intent = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse(kanjiRecognizerUri));
                                activity.startActivity(intent);
                                activity.finish();
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

    public static boolean isKrInstalled(Context context, Application application) {
        Log.d(TAG, "Checking for Kanji Recognizer...");
        PackageManager pm = context.getPackageManager();
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

            String myPackageName = application.getPackageName();
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

        try {
            return Integer.parseInt(idxStr);
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    public static String getDefaultDictionary(Context context) {
        String[] dictionaries = context.getResources().getStringArray(
                R.array.dictionary_codes_array);

        return dictionaries[getDefaultDictionaryIdx(context)];
    }

    private String getDictionaryName(int dictIdx) {
        return getDictionaryName(this, dictIdx);
    }

    static String getDictionaryName(Context ctx, int dictIdx) {
        String[] dictionaryNames = ctx.getResources().getStringArray(
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

        try {
            return Integer.parseInt(timeoutStr);
        } catch (NumberFormatException nfe) {
            return WWWJDIC_TIMEOUT_DEFAULT;
        }
    }

    public static int getKrTimeout(Context context) {
        SharedPreferences preferences = getPrefs(context);

        String timeoutStr = preferences.getString(PREF_KR_TIMEOUT_KEY, "10");

        try {
            return Integer.parseInt(timeoutStr) * 1000;
        } catch (NumberFormatException nfe) {
            return KR_TIMEOUT_DEFAULT;
        }
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

    public static void setUseKanjiRecognizer(boolean useKr, Context context) {
        SharedPreferences.Editor editor = getPrefsEditor(context);

        editor.putBoolean(PREF_KR_USE_KANJI_RECOGNIZER_KEY, useKr);
        editor.commit();
    }

    public static int getWeocrTimeout(Context context) {
        SharedPreferences preferences = getPrefs(context);

        String timeoutStr = preferences.getString(PREF_WEOCR_TIMEOUT_KEY, "10");

        try {
            return Integer.parseInt(timeoutStr) * 1000;
        } catch (NumberFormatException nfe) {
            return WEOCR_TIMEOUT_DEFAULT;
        }
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

        String delayStr = preferences.getString(PREF_SOD_ANIMATION_DELAY,
                DEFAULT_STROKE_ANIMATION_DELAY);

        try {
            return Integer.parseInt(delayStr);
        } catch (NumberFormatException e) {
            return Integer.parseInt(DEFAULT_STROKE_ANIMATION_DELAY);
        }
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

        try {
            return Integer.parseInt(delayStr) * 1000;
        } catch (NumberFormatException nfe) {
            return SOD_TIMEOUT_DEFAULT;
        }
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
        return PreferenceManager.getDefaultSharedPreferences(context).edit();
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

    public static boolean isKodLevelOneOnly(Context context) {
        return getBooleanPref(context, PREF_KOD_LEVEL1_ONLY_KEY, false);
    }

    public static void setKodLevelOneOnly(Context context, boolean levelOneOnly) {
        saveBooleanPref(context, PREF_KOD_LEVEL1_ONLY_KEY, levelOneOnly);
    }

    public static boolean isKodUseJlpt(Context context) {
        return getBooleanPref(context, PREF_KOD_USE_JLPT_KEY, false);
    }

    public static void setKodUseJlpt(Context context, boolean levelOneOnly) {
        saveBooleanPref(context, PREF_KOD_USE_JLPT_KEY, levelOneOnly);
    }

    public static void setKodShowReading(Context context, boolean showReading) {
        saveBooleanPref(context, PREF_KOD_SHOW_READING_KEY, showReading);
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

    public static void setKodJlptLevel(Context context, int jlptLevel) {
        SharedPreferences.Editor editor = getPrefsEditor(context);
        editor.putInt(PREF_KOD_JLPT_LEVEL, jlptLevel);
        editor.commit();
    }

    public static int getKodJlptLevel(Context context) {
        return getPrefs(context).getInt(PREF_KOD_JLPT_LEVEL, 1);
    }

    public static boolean wantsTts(Context context) {
        return getBooleanPref(context, PREF_WANTS_TTS_KEY, true);
    }

    public static synchronized void setWantsTts(Context context,
            boolean wantsTts) {
        SharedPreferences.Editor editor = getPrefsEditor(context);
        editor.putBoolean(PREF_WANTS_TTS_KEY, wantsTts);
        editor.commit();
    }

    public static synchronized void setLastKodUpdateError(Context context,
            long lastUpdateError) {
        SharedPreferences.Editor editor = getPrefsEditor(context);
        editor.putLong(PREF_LAST_KOD_UPDATE_ERROR_KEY, lastUpdateError);
        editor.commit();
    }

    public static long getLastKodUpdateError(Context context) {
        return getPrefs(context).getLong(PREF_LAST_KOD_UPDATE_ERROR_KEY, 0);
    }

    public static boolean isReturnRandomExamples(Context context) {
        SharedPreferences preferences = getPrefs(context);

        return preferences.getBoolean(PREF_RANDOM_EXAMPLES_KEY, false);
    }

    public static String getJpTtsEnginePackage(Context context) {
        return getPrefs(context).getString(PREF_JP_TTS_ENGINE,
                DEFAULT_JP_TTS_ENGINE_PACKAGE);
    }

    public static synchronized boolean isKodRandom(Context context) {
        return getPrefs(context).getBoolean(PREF_KOD_IS_RANDOM_KEY, true);
    }

    public static synchronized void setKodRandom(Context context,
            boolean isRandom) {
        getPrefsEditor(context).putBoolean(PREF_KOD_IS_RANDOM_KEY, isRandom)
                .commit();
    }

    public static synchronized boolean isKodTransparentBg(Context context) {
        return getPrefs(context).getBoolean(PREF_KOD_TRANSPARENT_BG_KEY, false);
    }

    public static void setKodTransparentBg(Context context,
            boolean isTransparent) {
        getPrefsEditor(context).putBoolean(PREF_KOD_TRANSPARENT_BG_KEY,
                isTransparent).commit();
    }

    public static synchronized String getKodCurrentKanji(Context context) {
        return getPrefs(context).getString(PREF_KOD_CURRENT_KANJI_KEY, null);
    }

    public static synchronized void setKodCurrentKanji(Context context,
            String kanji) {
        getPrefsEditor(context).putString(PREF_KOD_CURRENT_KANJI_KEY, kanji)
                .commit();
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

    public static synchronized void setSelectedDictionaryIdx(Context context,
            int selectedDictionary) {
        getPrefsEditor(context).putInt(PREF_SELECTED_DICTIONARY,
                selectedDictionary).commit();
    }

    public static int getSelectedDictionaryIdx(Context context) {
        return getPrefs(context).getInt(PREF_SELECTED_DICTIONARY, 0);
    }

    public static synchronized void setKanjiSearchTypeIdx(Context context,
            int kanjiSearchType) {
        getPrefsEditor(context).putInt(PREF_KANJI_SEARCH_TYPE_KEY,
                kanjiSearchType).commit();
    }

    public static int getKanjiSearchTypeIdx(Context context) {
        return getPrefs(context).getInt(PREF_KANJI_SEARCH_TYPE_KEY, 0);
    }

    public static synchronized void setSentenceModeIdx(Context context,
            int translationMode) {
        getPrefsEditor(context).putInt(PREF_SENTENCE_MODE_KEY, translationMode)
                .commit();
    }

    public static int getSentenceModeIdx(Context context) {
        return getPrefs(context).getInt(PREF_SENTENCE_MODE_KEY, 0);
    }

    public static boolean isPopupKeyboard(Context context) {
        return getPrefs(context).getBoolean(PREF_POPUP_KEYBOARD_KEY, true);
    }

}
