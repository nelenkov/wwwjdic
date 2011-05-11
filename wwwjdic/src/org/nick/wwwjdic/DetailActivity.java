package org.nick.wwwjdic;

import java.util.Locale;
import java.util.regex.Pattern;

import org.nick.wwwjdic.history.HistoryDbHelper;
import org.nick.wwwjdic.utils.DictUtils;
import org.nick.wwwjdic.utils.IntentSpan;
import org.nick.wwwjdic.utils.Pair;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.speech.tts.TextToSpeech;
import android.text.ClipboardManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public abstract class DetailActivity extends Activity implements
        OnCheckedChangeListener, OnLongClickListener,
        TextToSpeech.OnInitListener {

    protected static final Pattern CROSS_REF_PATTERN = Pattern
            .compile("^.*\\(See (\\S+)\\).*$");

    protected static final int ITEM_ID_HOME = 0;

    private static final String TAG = DetailActivity.class.getSimpleName();

    private static final int TTS_DATA_CHECK_CODE = 0;

    protected HistoryDbHelper db;
    protected WwwjdicEntry wwwjdicEntry;
    protected boolean isFavorite;

    protected TextToSpeech tts;

    protected DetailActivity() {
        db = HistoryDbHelper.getInstance(this);
    }

    protected void addToFavorites() {
        long favoriteId = db.addFavorite(wwwjdicEntry);
        wwwjdicEntry.setId(favoriteId);
    }

    protected void removeFromFavorites() {
        db.deleteFavorite(wwwjdicEntry.getId());
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
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setText(wwwjdicEntry.getHeadword());
        String messageTemplate = getResources().getString(
                R.string.copied_to_clipboard);
        Toast t = Toast.makeText(this,
                String.format(messageTemplate, wwwjdicEntry.getHeadword()),
                Toast.LENGTH_SHORT);
        t.show();

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ITEM_ID_HOME, 0, R.string.home).setIcon(
                android.R.drawable.ic_menu_compass);

        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
        case ITEM_ID_HOME:
            Intent intent = new Intent(this, Wwwjdic.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            setHomeActivityExtras(intent);

            startActivity(intent);
            finish();

            return true;
        default:
            // do nothing
        }

        return super.onMenuItemSelected(featureId, item);
    }

    protected abstract void setHomeActivityExtras(Intent homeActivityIntent);

    protected WwwjdicApplication getApp() {
        return (WwwjdicApplication) getApplication();
    }

    protected void makeClickable(TextView textView, int start, int end,
            Intent intent) {
        SpannableString str = SpannableString.valueOf(textView.getText());
        str.setSpan(new IntentSpan(this, intent), start, end,
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
        if (status != TextToSpeech.SUCCESS) {
            hideTtsButtons();
            return;
        }

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

    protected abstract void showTtsButtons();

    protected abstract void hideTtsButtons();

    protected Pair<LinearLayout, TextView> createMeaningTextView(
            final Context ctx, String meaning) {
        LayoutInflater inflater = LayoutInflater.from(ctx);
        LinearLayout translationLayout = (LinearLayout) inflater.inflate(
                R.layout.translation_item, null);
        final TextView translationText = (TextView) translationLayout
                .findViewById(R.id.translation_text);
        translationText.setText(meaning);
        Button speakButton = (Button) translationLayout
                .findViewById(R.id.speak_button);
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

        Pair<LinearLayout, TextView> result = new Pair<LinearLayout, TextView>(
                translationLayout, translationText);
        return result;
    }

    protected abstract Locale getSpeechLocale();

    protected void checkTtsAvailability() {
        Intent checkIntent = new Intent(
                TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, TTS_DATA_CHECK_CODE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TTS_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                tts = new TextToSpeech(this, this);
            } else {
                if (WwwjdicPreferences.wantsTts(this)) {
                    Dialog dialog = createInstallTtsDataDialog();
                    dialog.show();
                } else {
                    hideTtsButtons();
                }
            }
        }
    }

    public Dialog createInstallTtsDataDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.install_tts_data_message)
                .setTitle(R.string.install_tts_data_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                WwwjdicPreferences.setWantsTts(
                                        DetailActivity.this, true);
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
                                WwwjdicPreferences.setWantsTts(
                                        DetailActivity.this, false);
                                dialog.dismiss();
                            }
                        });

        return builder.create();
    }
}
