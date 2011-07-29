package org.nick.wwwjdic;

import org.nick.wwwjdic.history.HistoryDbHelper;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

public abstract class DetailActivity extends FragmentActivity {

    protected static final int ITEM_ID_HOME = 0;

    protected HistoryDbHelper db;
    protected WwwjdicEntry wwwjdicEntry;

    protected DetailActivity() {
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        db = HistoryDbHelper.getInstance(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ITEM_ID_HOME, 0, R.string.home).setIcon(
                android.R.drawable.ic_menu_compass);

        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
        case ITEM_ID_HOME:
            Intent intent = new Intent(this, Wwwjdic.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            setHomeActivityExtras(intent);

            startActivity(intent);
            finish();

            return true;
        default:
            // do nothing
        }

        return super.onMenuItemSelected(featureId, item);
    }

    protected abstract void setHomeActivityExtras(Intent homeActivityIntent);

}
