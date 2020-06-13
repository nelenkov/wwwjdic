package org.nick.wwwjdic;

import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

public abstract class ActionBarActivity extends AppCompatActivity {

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Activities.home(this);
            return true;
        default:
            // do nothing
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

}
