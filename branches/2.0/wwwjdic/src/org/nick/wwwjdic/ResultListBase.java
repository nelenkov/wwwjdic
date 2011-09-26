package org.nick.wwwjdic;

import org.nick.wwwjdic.utils.Analytics;

public abstract class ResultListBase extends ActionBarActivity {

    protected ResultListBase() {
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
