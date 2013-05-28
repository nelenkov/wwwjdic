package org.nick.wwwjdic.widgets;

import org.nick.wwwjdic.ActionBarActivity;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicPreferences;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

public class KodWidgetConfigure extends ActionBarActivity implements
        OnClickListener, OnCheckedChangeListener {

    private static final int ONE_DAY_IDX = 0;
    private static final int TWELVE_HOURS_IDX = 1;
    private static final int SIX_HOURS_IDX = 2;
    private static final int TWO_MINUTES_IDX = 3;

    private static final long ONE_DAY_MILLIS = 24 * DateUtils.HOUR_IN_MILLIS;
    private static final long TWELVE_HOURS_MILLIS = 12 * DateUtils.HOUR_IN_MILLIS;
    private static final long SIX_HOURS_MILLIS = 6 * DateUtils.HOUR_IN_MILLIS;
    private static final long TWO_MINUTES_MILLIS = 2 * DateUtils.MINUTE_IN_MILLIS;

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private CheckBox levelOneCb;
    private CheckBox useJlptCb;
    private TextView jlptLevelLabel;
    private Spinner jlptLevelSpinner;
    private CheckBox showReadingCb;
    private CheckBox transparentBackgroundCb;
    private Spinner updateIntervalSpinner;
    private RadioButton kodRandomRb;
    private RadioButton kodSequentialRb;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setResult(RESULT_CANCELED);

        setContentView(R.layout.kod_widget_configure);

        findViews();

        boolean isKodRandom = WwwjdicPreferences.isKodRandom(this);
        kodRandomRb.setChecked(isKodRandom);
        kodSequentialRb.setChecked(!isKodRandom);

        boolean isJisLevel1 = WwwjdicPreferences.isKodLevelOneOnly(this);
        boolean isUseJlpt = WwwjdicPreferences.isKodUseJlpt(this);
        levelOneCb.setChecked(isJisLevel1);
        useJlptCb.setChecked(isUseJlpt);

        levelOneCb.setEnabled(!isUseJlpt);
        useJlptCb.setEnabled(!isJisLevel1);
        jlptLevelSpinner.setEnabled(isUseJlpt);
        jlptLevelLabel.setEnabled(isUseJlpt);

        showReadingCb.setChecked(WwwjdicPreferences.isKodShowReading(this));
        transparentBackgroundCb.setChecked(WwwjdicPreferences
                .isKodTransparentBg(this));

        long updateInterval = WwwjdicPreferences.getKodUpdateInterval(this);
        if (updateInterval == TWELVE_HOURS_MILLIS) {
            updateIntervalSpinner.setSelection(TWELVE_HOURS_IDX);
        } else if (updateInterval == SIX_HOURS_MILLIS) {
            updateIntervalSpinner.setSelection(SIX_HOURS_IDX);
        } else if (updateInterval == TWO_MINUTES_MILLIS) {
            updateIntervalSpinner.setSelection(TWO_MINUTES_IDX);
        } else {
            // default to 24h
            updateIntervalSpinner.setSelection(ONE_DAY_IDX);
        }

        int jlptLevel = WwwjdicPreferences.getKodJlptLevel(this);
        jlptLevelSpinner.setSelection(jlptLevel - 1);

        findViewById(R.id.okButton).setOnClickListener(this);
        findViewById(R.id.cancelButton).setOnClickListener(this);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
    }

    private void findViews() {
        levelOneCb = (CheckBox) findViewById(R.id.kod_level1_only_cb);
        levelOneCb.setOnCheckedChangeListener(this);
        useJlptCb = (CheckBox) findViewById(R.id.kod_use_jlpt_cb);
        useJlptCb.setOnCheckedChangeListener(this);
        jlptLevelLabel = (TextView) findViewById(R.id.jlpt_level_label);
        jlptLevelSpinner = (Spinner) findViewById(R.id.kod_jlpt_level_spinner);
        showReadingCb = (CheckBox) findViewById(R.id.kod_show_reading_cb);
        transparentBackgroundCb = (CheckBox) findViewById(R.id.kod_transparent_bg_cb);
        updateIntervalSpinner = (Spinner) findViewById(R.id.kod_update_interval_spinner);
        kodRandomRb = (RadioButton) findViewById(R.id.kod_random);
        kodSequentialRb = (RadioButton) findViewById(R.id.kod_sequential);
    }

    @Override
    public void onClick(View v) {
        final Context context = KodWidgetConfigure.this;

        if (v.getId() == R.id.okButton) {
            boolean isRandom = kodRandomRb.isChecked();
            WwwjdicPreferences.setKodRandom(this, isRandom);
            if (isRandom) {
                clearCurrentKanji();
            }

            boolean isSequential = !isRandom;
            boolean isL1Current = WwwjdicPreferences.isKodLevelOneOnly(this);
            boolean isL1 = levelOneCb.isChecked();
            // clear previous value if state has changed
            // e.g., JLPT -> JIS L1, JIS L1 -> JPLT, none -> JIS L1/JLPT
            if (isSequential && isL1Current != isL1) {
                clearCurrentKanji();
            }
            WwwjdicPreferences.setKodLevelOneOnly(this, isL1);

            boolean isUseJlptCurrent = WwwjdicPreferences.isKodUseJlpt(this);
            boolean isUseJlpt = useJlptCb.isChecked();
            WwwjdicPreferences.setKodUseJlpt(this, isUseJlpt);
            if (isSequential && isUseJlptCurrent != isUseJlpt) {
                clearCurrentKanji();
            }
            WwwjdicPreferences.setKodJlptLevel(this,
                    jlptLevelSpinner.getSelectedItemPosition() + 1);
            WwwjdicPreferences.setKodShowReading(this,
                    showReadingCb.isChecked());
            WwwjdicPreferences.setKodTransparentBg(this,
                    transparentBackgroundCb.isChecked());

            setUpdateInterval();

            ConnectivityMonitor.start(context);

            startService(new Intent(context, GetKanjiService.class));
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    appWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        } else if (v.getId() == R.id.cancelButton) {
            finish();
        }
    }

    private void clearCurrentKanji() {
        WwwjdicPreferences.setKodCurrentKanji(this, null);
    }

    private void setUpdateInterval() {
        long updateInterval = WwwjdicPreferences.KOD_DEFAULT_UPDATE_INTERVAL;
        switch (updateIntervalSpinner.getSelectedItemPosition()) {
        case ONE_DAY_IDX:
            updateInterval = ONE_DAY_MILLIS;
            break;
        case TWELVE_HOURS_IDX:
            updateInterval = TWELVE_HOURS_MILLIS;
            break;
        case SIX_HOURS_IDX:
            updateInterval = SIX_HOURS_MILLIS;
            break;
        case TWO_MINUTES_IDX:
            updateInterval = TWO_MINUTES_MILLIS;
            break;
        default:
            // do nothing
        }
        WwwjdicPreferences.setKodUpdateInterval(this, updateInterval);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.kod_level1_only_cb) {
            useJlptCb.setEnabled(!isChecked);
            jlptLevelSpinner.setEnabled(!isChecked);
            jlptLevelLabel.setEnabled(!isChecked);
        } else if (buttonView.getId() == R.id.kod_use_jlpt_cb) {
            levelOneCb.setEnabled(!isChecked);
            jlptLevelLabel.setEnabled(isChecked);
            jlptLevelSpinner.setEnabled(isChecked);
        }
    }

}
