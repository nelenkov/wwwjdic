package org.nick.wwwjdic.widgets;

import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicPreferences;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.Spinner;

public class KodWidgetConfigure extends Activity implements OnClickListener {

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
    private CheckBox showReadingCb;
    private Spinner updateIntervalSpinner;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setResult(RESULT_CANCELED);

        setContentView(R.layout.kod_widget_configure);

        findViews();

        levelOneCb.setChecked(WwwjdicPreferences.isKodLevelOneOnly(this));
        showReadingCb.setChecked(WwwjdicPreferences.isKodShowReading(this));
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

        findViewById(R.id.kod_configure_ok_button).setOnClickListener(this);
        findViewById(R.id.kod_configure_cancel_button).setOnClickListener(this);

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
        showReadingCb = (CheckBox) findViewById(R.id.kod_show_reading_cb);
        updateIntervalSpinner = (Spinner) findViewById(R.id.kod_update_interval_spinner);
    }

    @Override
    public void onClick(View v) {
        final Context context = KodWidgetConfigure.this;

        switch (v.getId()) {
        case R.id.kod_configure_ok_button:
            WwwjdicPreferences.setKodLevelOneOnly(this, levelOneCb.isChecked());
            WwwjdicPreferences.setKodShowReading(this, showReadingCb
                    .isChecked());

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

            startService(new Intent(context, GetKanjiService.class));

            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    appWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
            break;
        case R.id.kod_configure_cancel_button:
            finish();
            break;
        default:
            // do nothing
        }

    }

}
