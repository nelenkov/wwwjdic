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
        return (WwwjdicApplication) getActivity().getApplication();
    }

    protected void setupFavoritesHistoryFragments(int filterType) {
        // XXX fix!
        //        SearchHistoryFragment historyFragment = (SearchHistoryFragment) getSupportFragmentManager()
        //                .findFragmentById(R.id.history_fragment);
        //        if (historyFragment != null) {
        //            Bundle args = new Bundle();
        //            args.putInt(FavoritesAndHistory.EXTRA_FILTER_TYPE, filterType);
        //        }
        //    
        //        FavoritesFragment favoritesFragment = (FavoritesFragment) getSupportFragmentManager()
        //                .findFragmentById(R.id.favorites_fragment);
        //        if (favoritesFragment != null) {
        //            Bundle args = new Bundle();
        //            args.putInt(FavoritesAndHistory.EXTRA_FILTER_TYPE, filterType);
        //        }
    }

}
