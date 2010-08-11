package org.nick.wwwjdic;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;

public class WwwjdicPreferences extends PreferenceActivity implements
        OnPreferenceChangeListener {

    private CheckBoxPreference useKrPreference;

    private static final String PREF_USE_KR_KEY = "pref_kr_use_kanji_recognizer";

    private static final String KR_PACKAGE = "org.nick.kanjirecognizer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.settings);
        addPreferencesFromResource(R.xml.preferences);

        useKrPreference = (CheckBoxPreference) findPreference(PREF_USE_KR_KEY);
        useKrPreference.setOnPreferenceChangeListener(this);
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

        return true;
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

            return pm.checkSignatures("org.nick.wwwjdic", pi.packageName) == PackageManager.SIGNATURE_MATCH;
        } catch (NameNotFoundException e) {
            return false;
        }
    }
}
