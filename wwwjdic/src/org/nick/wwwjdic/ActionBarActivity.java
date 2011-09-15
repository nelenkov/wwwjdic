package org.nick.wwwjdic;

import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItem;

public abstract class ActionBarActivity extends FragmentActivity {

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

}
