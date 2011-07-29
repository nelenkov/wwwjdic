package org.nick.wwwjdic;

import org.nick.wwwjdic.utils.Analytics;

import android.support.v4.app.FragmentActivity;

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

}
