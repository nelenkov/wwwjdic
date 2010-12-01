package org.nick.wwwjdic;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.about_dialog);

        TextView versionText = (TextView) findViewById(R.id.versionText);
        versionText.setText("version " + WwwjdicApplication.getVersion());
    }
}
