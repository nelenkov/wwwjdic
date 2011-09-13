package org.nick.wwwjdic;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItem;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class AboutActivity extends FragmentActivity implements OnClickListener {

    private static final String DONATE_VERSION_PACKAGE = "org.nick.wwwjdic.donate";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.about_dialog);

        TextView versionText = (TextView) findViewById(R.id.versionText);
        versionText.setText("version " + WwwjdicApplication.getVersion());

        TextView faqText = (TextView) findViewById(R.id.faqText);
        faqText.setMovementMethod(LinkMovementMethod.getInstance());

        TextView kradfileAttributionText = (TextView) findViewById(R.id.kradfile_attribution_text);
        kradfileAttributionText.setMovementMethod(LinkMovementMethod
                .getInstance());

        Button buyDonateButton = (Button) findViewById(R.id.buy_donate);
        if (!isDonateVersion()) {
            buyDonateButton.setOnClickListener(this);
        } else {
            buyDonateButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + DONATE_VERSION_PACKAGE));
        startActivity(intent);
    }

    private boolean isDonateVersion() {
        String appPackage = getApplication().getPackageName();

        return DONATE_VERSION_PACKAGE.equals(appPackage);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent intent = new Intent(this, Wwwjdic.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
