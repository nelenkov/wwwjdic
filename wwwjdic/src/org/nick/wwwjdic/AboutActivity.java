package org.nick.wwwjdic;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends ActionBarActivity  {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.about_dialog);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        TextView versionText = findViewById(R.id.versionText);
        versionText.setText("version " + WwwjdicApplication.getVersion());

        TextView faqText = findViewById(R.id.faqText);
        faqText.setMovementMethod(LinkMovementMethod.getInstance());

        TextView kradfileAttributionText = findViewById(R.id.kradfile_attribution_text);
        kradfileAttributionText.setMovementMethod(LinkMovementMethod
                .getInstance());
    }

}
