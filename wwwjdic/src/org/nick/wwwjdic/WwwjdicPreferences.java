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
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

public class WwwjdicPreferences extends PreferenceActivity implements
        OnPreferenceChangeListener {

    private static final String TAG = WwwjdicPreferences.class.getSimpleName();

    private static final String PREF_USE_KR_KEY = "pref_kr_use_kanji_recognizer";

    private static final String PREF_AUTO_SELECT_MIRROR_KEY = "pref_auto_select_mirror";
    private static final String PREF_MIRROR_URL_KEY = "pref_wwwjdic_mirror_url";

    private static final String KR_PACKAGE = "org.nick.kanjirecognizer";

    private static final String PREF_DEFAULT_DICT_PREF_KEY = "pref_default_dict";

    private static final String PREF_EXPORT_MEANINGS_SEPARATOR_CHAR = "pref_export_meanings_separator_char";

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

        mirrorPreference = (ListPreference) findPreference(PREF_MIRROR_URL_KEY);
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
            }

            return true;
        }

        if (PREF_MIRROR_URL_KEY.equals(preference.getKey())) {
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
        builder.setMessage(R.string.install_kr).setCancelable(false)
                .setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent intent = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id="
                                                + KR_PACKAGE));
                                startActivity(intent);
                            }
                        }).setNegativeButton(R.string.no,
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
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(context);

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
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(context);

        return preferences.getString(PREF_EXPORT_MEANINGS_SEPARATOR_CHAR, "\n");
    }
}
