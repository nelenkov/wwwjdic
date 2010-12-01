package org.nick.wwwjdic;

import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

public class WwwjdicPreferences extends PreferenceActivity implements
        OnPreferenceChangeListener {

    private static final String TAG = WwwjdicPreferences.class.getSimpleName();

    private static final String PREF_USE_KR_KEY = "pref_kr_use_kanji_recognizer";
    private static final String PREF_MIRROR_URL_KEY = "pref_wwwjdic_mirror_url";

    private static final String KR_PACKAGE = "org.nick.kanjirecognizer";

    private CheckBoxPreference useKrPreference;
    private ListPreference mirrorPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.settings);
        addPreferencesFromResource(R.xml.preferences);

        useKrPreference = (CheckBoxPreference) findPreference(PREF_USE_KR_KEY);
        useKrPreference.setOnPreferenceChangeListener(this);
        mirrorPreference = (ListPreference) findPreference(PREF_MIRROR_URL_KEY);
        mirrorPreference.setSummary(mirrorPreference.getEntry());
        mirrorPreference.setOnPreferenceChangeListener(this);
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

        if (PREF_MIRROR_URL_KEY.equals(preference.getKey())) {
            preference.setSummary(getMirrorName((String) newValue));
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
        PackageManager pm = getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(KR_PACKAGE, 0);
            if (pi.versionCode < 2) {
                Log.d(TAG, String.format(
                        "Kanji recognizer %s is installed, but we need 1.1",
                        pi.versionName));
                return false;
            }

            return pm.checkSignatures("org.nick.wwwjdic", pi.packageName) == PackageManager.SIGNATURE_MATCH;
        } catch (NameNotFoundException e) {
            return false;
        }
    }
}
