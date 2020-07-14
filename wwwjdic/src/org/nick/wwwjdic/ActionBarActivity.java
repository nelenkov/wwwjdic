package org.nick.wwwjdic;

import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AppCompatActivity;

public abstract class ActionBarActivity extends AppCompatActivity {

    @Override
    protected void onStart() {
        super.onStart();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    protected Intent getHomeIntent() {
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(getPackageName());
        if (intent == null) {
            return null;
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return intent;
    }

}
