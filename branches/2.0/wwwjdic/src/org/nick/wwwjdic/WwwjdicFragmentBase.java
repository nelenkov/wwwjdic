package org.nick.wwwjdic;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class WwwjdicFragmentBase extends Fragment {

    protected static final int NUM_RECENT_HISTORY_ENTRIES = 5;

    protected boolean inputTextFromBundle;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected WwwjdicApplication getApp() {
        return WwwjdicApplication.getInstance();
    }

}
