package org.nick.wwwjdic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.nick.wwwjdic.history.HistoryDbHelper;
import org.nick.wwwjdic.model.WwwjdicEntry;
import org.nick.wwwjdic.utils.DictUtils;
import org.nick.wwwjdic.utils.IntentSpan;
import org.nick.wwwjdic.utils.Pair;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.ClipboardManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

@SuppressWarnings("deprecation")
public abstract class DetailFragment extends SherlockFragment implements
        OnCheckedChangeListener, OnLongClickListener,
        TextToSpeech.OnInitListener {

    protected static final Pattern CROSS_REF_PATTERN = Pattern
            .compile("^.*\\(See (\\S+)\\).*$");

    protected static final int ITEM_ID_HOME = 0;

    private static final String TAG = DetailFragment.class.getSimpleName();

    private static final int TTS_DATA_CHECK_CODE = 0;

    private static final boolean IS_FROYO = Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;

    private static Method tts_getDefaultEngine;
    private static Method tts_setEngineByPackageName;

    static {
        try {
            tts_getDefaultEngine = TextToSpeech.class.getMethod(
                    "getDefaultEngine", (Class[]) null);
            tts_setEngineByPackageName = TextToSpeech.class.getMethod(
                    "setEngineByPackageName", new Class[] { String.class });
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        }
    }

    protected HistoryDbHelper db;
    protected WwwjdicEntry wwwjdicEntry;
    protected boolean isFavorite;

    protected TextToSpeech tts;
    protected TextToSpeech jpTts;

    private String jpTtsEnginePackageName;

    protected DetailFragment() {
    }

    protected void addToFavorites() {
        long favoriteId = db.addFavorite(wwwjdicEntry);
        wwwjdicEntry.setId(favoriteId);
    }

    protected void removeFromFavorites() {
        db.deleteFavorite(wwwjdicEntry.getId());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        db = HistoryDbHelper.getInstance(getActivity());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        jpTtsEnginePackageName = WwwjdicPreferences
                .getJpTtsEnginePackage(activity);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (tts != null) {
            tts.shutdown();
        }

        if (jpTts != null) {
            jpTts.shutdown();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            addToFavorites();
        } else {
            removeFromFavorites();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        copy();

        return true;
    }

    protected WwwjdicApplication getApp() {
        return WwwjdicApplication.getInstance();
    }

    protected void makeClickable(TextView textView, int start, int end,
            Intent intent) {
        SpannableString str = SpannableString.valueOf(textView.getText());
        str.setSpan(new IntentSpan(getActivity(), intent), start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(str);
        textView.setLinkTextColor(Color.WHITE);
        MovementMethod m = textView.getMovementMethod();
        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            if (textView.getLinksClickable()) {
                textView.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }

    @Override
    public void onInit(int status) {
        // TODO should check which engine failed?
        if (status != TextToSpeech.SUCCESS) {
            hideTtsButtons();
            toggleJpTtsButtons(false);
            return;
        }

        if (jpTts != null && IS_FROYO) {
            try {
                String defaultEngine = (String) tts_getDefaultEngine.invoke(
                        jpTts, (Object[]) null);
                if (!defaultEngine.equals(jpTtsEnginePackageName)) {
                    int rc = (Integer) tts_setEngineByPackageName.invoke(jpTts,
                            new Object[] { jpTtsEnginePackageName });
                    if (rc == TextToSpeech.ERROR) {
                        Log.w(TAG, jpTtsEnginePackageName + " not available?");
                        jpTts.shutdown();
                        jpTts = null;
                        toggleJpTtsButtons(false);

                        return;
                    }
                }
                jpTts.setLanguage(Locale.JAPAN);

                toggleJpTtsButtons(true);
            } catch (InvocationTargetException e) {
                Log.e(TAG, "error calling by reflection: " + e.getMessage());
                toggleJpTtsButtons(false);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "error calling by reflection: " + e.getMessage());
                toggleJpTtsButtons(false);
            }
        }

        if (tts != null) {
            Locale locale = getSpeechLocale();
            if (locale == null) {
                Log.w(TAG, "TTS locale " + locale + "not recognized");
                hideTtsButtons();
                return;
            }

            if (tts.isLanguageAvailable(locale) != TextToSpeech.LANG_MISSING_DATA
                    && tts.isLanguageAvailable(locale) != TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(getSpeechLocale());
                showTtsButtons();
            } else {
                Log.w(TAG, "TTS locale " + locale + " not available");
                hideTtsButtons();
            }
        }
    }

    protected abstract void showTtsButtons();

    protected abstract void hideTtsButtons();

    protected abstract void toggleJpTtsButtons(boolean show);

    protected Pair<LinearLayout, TextView> createMeaningTextView(
            final Context ctx, String meaning) {
        return createMeaningTextView(ctx, meaning, true);
    }

    protected Pair<LinearLayout, TextView> createMeaningTextView(
            final Context ctx, String meaning, boolean enableTts) {
        LayoutInflater inflater = LayoutInflater.from(ctx);
        LinearLayout translationLayout = (LinearLayout) inflater.inflate(
                R.layout.translation_item, null);
        final TextView translationText = (TextView) translationLayout
                .findViewById(R.id.translation_text);
        translationText.setText(meaning);
        Button speakButton = (Button) translationLayout
                .findViewById(R.id.speak_button);
        if (enableTts) {
            speakButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    String toSpeak = DictUtils.stripWwwjdicTags(ctx,
                            translationText.getText().toString());
                    if (tts != null) {
                        tts.speak(toSpeak, TextToSpeech.QUEUE_ADD, null);
                    }
                }
            });
        } else {
            translationLayout.removeView(speakButton);
        }

        Pair<LinearLayout, TextView> result = new Pair<LinearLayout, TextView>(
                translationLayout, translationText);
        return result;
    }

    protected abstract Locale getSpeechLocale();

    protected void checkTtsAvailability() {
        PackageManager pm = getActivity().getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(jpTtsEnginePackageName, 0);
            if (pi != null) {
                jpTts = new TextToSpeech(getActivity(), this);
            }
        } catch (NameNotFoundException e) {
            Log.w(TAG, jpTtsEnginePackageName + " not found");
        }

        if (!isIntentAvailable(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)) {
            return;
        }

        Intent checkIntent = new Intent(
                TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, TTS_DATA_CHECK_CODE);
    }

    private boolean isIntentAvailable(String action) {
        PackageManager packageManager = getActivity().getPackageManager();
        Intent intent = new Intent(action);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);

        return list.size() > 0;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TTS_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                tts = new TextToSpeech(getActivity(), this);
            } else {
                if (WwwjdicPreferences.wantsTts(getActivity())) {
                    Dialog dialog = createInstallTtsDataDialog();
                    dialog.show();
                } else {
                    hideTtsButtons();
                }
            }
        }
    }

    public Dialog createInstallTtsDataDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity());
        builder.setMessage(R.string.install_tts_data_message)
                .setTitle(R.string.install_tts_data_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                WwwjdicPreferences.setWantsTts(getActivity(),
                                        true);
                                Intent installIntent = new Intent(
                                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                                dialog.dismiss();
                                startActivity(installIntent);
                            }
                        })
                .setNegativeButton(R.string.not_now,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                hideTtsButtons();
                                dialog.dismiss();

                            }
                        })
                .setNeutralButton(R.string.dont_ask_again,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                hideTtsButtons();
                                WwwjdicPreferences.setWantsTts(getActivity(),
                                        false);
                                dialog.dismiss();
                            }
                        });

        return builder.create();
    }

    protected void copy() {
        ClipboardManager cm = (ClipboardManager) getActivity()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setText(wwwjdicEntry.getHeadword());
        String messageTemplate = getResources().getString(
                R.string.copied_to_clipboard);
        Toast.makeText(getActivity(),
                String.format(messageTemplate, wwwjdicEntry.getHeadword()),
                Toast.LENGTH_SHORT).show();
    }

    protected void share() {
        Intent shareIntent = createShareIntent();
        getActivity().startActivity(shareIntent);
    }

    protected Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        String str = wwwjdicEntry.getHeadword() + " "
                + wwwjdicEntry.getDetailString();
        shareIntent.putExtra(Intent.EXTRA_TEXT, str);

        return shareIntent;
    }
}
