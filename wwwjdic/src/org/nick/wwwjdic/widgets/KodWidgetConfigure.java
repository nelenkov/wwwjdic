package org.nick.wwwjdic.widgets;

import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicPreferences;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

public class KodWidgetConfigure extends Activity implements OnClickListener {


    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private CheckBox levelOneCb;
    private CheckBox showReadingCb;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setResult(RESULT_CANCELED);

        setContentView(R.layout.kod_widget_configure);

        levelOneCb = (CheckBox) findViewById(R.id.kod_level1_only_cb);
        showReadingCb = (CheckBox) findViewById(R.id.kod_show_reading_cb);

        // Bind the action for the save button.
        findViewById(R.id.kod_configure_ok_button).setOnClickListener(this);

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

    @Override
    public void onClick(View arg0) {
        final Context context = KodWidgetConfigure.this;

        WwwjdicPreferences.setKodLevelOneOnly(this, levelOneCb.isChecked());
        WwwjdicPreferences.setKodShowReading(this, showReadingCb.isChecked());

        startService(new Intent(context, GetKanjiService.class));

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();

    }

}
