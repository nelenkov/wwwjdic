package org.nick.wwwjdic;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class TtsManager implements TextToSpeech.OnInitListener {

    public interface TtsEnabled {

        Locale getSpeechLocale();

        void showTtsButtons();

        void hideTtsButtons();

        boolean wantsTts();

        void setWantsTts(boolean wantsTts);
    }

    private static final String TAG = TtsManager.class.getSimpleName();

    private static final String MARKET_URL_TEMPLATE = "market://details?id=%s";

    private static final boolean IS_FROYO = Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    private static final boolean IS_ICS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;

    private static Method tts_getDefaultEngine;
    private static Method tts_setEngineByPackageName;
    private static Constructor<TextToSpeech> tts_packageNameCtor;

    static {
        try {
            tts_getDefaultEngine = TextToSpeech.class.getMethod(
                    "getDefaultEngine", (Class[]) null);
            tts_setEngineByPackageName = TextToSpeech.class.getMethod(
                    "setEngineByPackageName", new Class[] { String.class });
            if (IS_ICS) {
                tts_packageNameCtor = TextToSpeech.class.getConstructor(
                        Context.class, TextToSpeech.OnInitListener.class,
                        String.class);
            }
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        }
    }

    private Context context;
    private TtsEnabled ttsActivitiy;

    private boolean showInstallDialog = false;
    private String ttsEnginePackage;

    private TextToSpeech tts;

    public TtsManager(Context context, TtsEnabled ttsActivitiy,
            String ttsEnginePackage, boolean showInstallDialog) {
        this.context = context;
        this.ttsActivitiy = ttsActivitiy;
        this.ttsEnginePackage = ttsEnginePackage;
        this.showInstallDialog = showInstallDialog;
    }

    public TtsManager(Context context, TtsEnabled ttsActivitiy,
            String ttsEnginePackage) {
        this(context, ttsActivitiy, ttsEnginePackage, true);
    }

    public void checkTtsAvailability() {
        boolean available = isPackageInstalled(ttsEnginePackage);
        if (available) {
            if (tts == null) {
                if (IS_ICS && ttsEnginePackage != null) {
                    try {
                        tts = tts_packageNameCtor.newInstance(context, this,
                                ttsEnginePackage);
                    } catch (InvocationTargetException e) {
                        disableTts(e);
                    } catch (IllegalArgumentException e) {
                        disableTts(e);
                    } catch (InstantiationException e) {
                        disableTts(e);
                    } catch (IllegalAccessException e) {
                        disableTts(e);
                    }
                } else {
                    tts = new TextToSpeech(context, this);
                }
            } else {
                if (showInstallDialog && ttsActivitiy.wantsTts()) {
                    Dialog dialog = createInstallTtsDataDialog();
                    dialog.show();
                } else {
                    ttsActivitiy.hideTtsButtons();
                }
            }
        }
    }

    private void disableTts(Exception e) {
        Log.e(TAG, "Failed to initialize TTS: " + e.getMessage(), e);
        tts = null;
        ttsActivitiy.hideTtsButtons();
    }

    @Override
    public void onInit(int status) {
        if (tts == null) {
            Log.w(TAG, "TTS not found or failed to initialize");
            return;
        }

        if (status != TextToSpeech.SUCCESS) {
            ttsActivitiy.hideTtsButtons();
            return;
        }

        // XXX -- use default engine when null?
        if (ttsEnginePackage != null && IS_FROYO && !IS_ICS) {
            try {
                String defaultEngine = (String) tts_getDefaultEngine.invoke(
                        tts, (Object[]) null);
                if (!defaultEngine.equals(ttsEnginePackage)) {
                    int rc = (Integer) tts_setEngineByPackageName.invoke(tts,
                            new Object[] { ttsEnginePackage });
                    if (rc == TextToSpeech.ERROR) {
                        Log.w(TAG, ttsEnginePackage + " not available?");
                        tts.shutdown();
                        tts = null;
                        ttsActivitiy.hideTtsButtons();

                        return;
                    }
                }
            } catch (InvocationTargetException e) {
                disableTts(e);
            } catch (IllegalAccessException e) {
                disableTts(e);
            }
        }

        Locale locale = ttsActivitiy.getSpeechLocale();
        if (locale == null) {
            Log.w(TAG, "TTS locale " + locale + "not recognized");
            ttsActivitiy.hideTtsButtons();
            return;
        }

        if (isLanguageAvailable(locale)) {
            try {
                tts.setLanguage(locale);
                ttsActivitiy.showTtsButtons();
                // Handle Samsung TTS: java.lang.IllegalArgumentException: Invalid int: "OS"
            } catch (Exception e) {
                Log.w(TAG, "TTS locale " + locale + " not available");
                ttsActivitiy.hideTtsButtons();
            }
        } else {
            Log.w(TAG, "TTS locale " + locale + " not available");
            ttsActivitiy.hideTtsButtons();
        }

    }

    private boolean isPackageInstalled(String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(packageName, 0);

            return pi != null;
        } catch (NameNotFoundException e) {
            Log.w(TAG, packageName + " not found", e);
            return false;
        }
    }

    public Dialog createInstallTtsDataDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.install_tts_data_message)
                .setTitle(R.string.install_tts_data_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                ttsActivitiy.setWantsTts(true);
                                Intent installIntent = new Intent(
                                        Intent.ACTION_VIEW);
                                installIntent.setData(Uri.parse(String.format(
                                        MARKET_URL_TEMPLATE, ttsEnginePackage)));
                                dialog.dismiss();
                                context.startActivity(installIntent);
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(R.string.not_now,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                ttsActivitiy.hideTtsButtons();
                                dialog.dismiss();

                            }
                        })
                .setNeutralButton(R.string.dont_ask_again,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                ttsActivitiy.hideTtsButtons();
                                ttsActivitiy.setWantsTts(false);
                                dialog.dismiss();
                            }
                        });

        return builder.create();
    }

    public void speak(String text) {
        if (tts == null) {
            return;
        }

        tts.speak(text, TextToSpeech.QUEUE_ADD, null);
    }

    public void setLanguage(Locale speechLocale) {
        tts.setLanguage(speechLocale);
    }

    public boolean isLanguageAvailable(Locale speechLocale) {
        return tts.isLanguageAvailable(speechLocale) != TextToSpeech.LANG_MISSING_DATA
                && tts.isLanguageAvailable(speechLocale) != TextToSpeech.LANG_NOT_SUPPORTED;
    }

    public void shutdown() {
        if (tts != null) {
            tts.shutdown();
        }
    }

}
