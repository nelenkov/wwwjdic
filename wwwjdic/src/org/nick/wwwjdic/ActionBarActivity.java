package org.nick.wwwjdic;

import android.app.Activity;
import android.view.MenuItem;

public abstract class ActionBarActivity extends Activity {

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

        getActionBar().setHomeButtonEnabled(true);
    }

}
