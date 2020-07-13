package org.nick.wwwjdic;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class AboutActivity extends ActionBarActivity implements OnClickListener {

    private static final String DONATE_VERSION_PACKAGE = "org.nick.wwwjdic.donate";

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

        Button buyDonateButton = findViewById(R.id.buy_donate);
        if (!isDonateVersion()) {
            buyDonateButton.setOnClickListener(this);
        } else {
            buyDonateButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=" + DONATE_VERSION_PACKAGE));
        startActivity(intent);
    }

    private boolean isDonateVersion() {
        String appPackage = getApplication().getPackageName();

        return DONATE_VERSION_PACKAGE.equals(appPackage);
    }

}
