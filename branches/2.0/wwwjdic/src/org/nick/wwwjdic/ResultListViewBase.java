package org.nick.wwwjdic;

import org.nick.wwwjdic.utils.Analytics;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItem;

public abstract class ResultListViewBase extends FragmentActivity {

    protected ResultListViewBase() {
    }

    @Override
    protected void onStart() {
        super.onStart();

        Analytics.startSession(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Analytics.endSession(this);
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
