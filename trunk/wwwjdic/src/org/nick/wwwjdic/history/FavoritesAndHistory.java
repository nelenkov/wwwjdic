package org.nick.wwwjdic.history;

import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.R;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

public class FavoritesAndHistory extends TabActivity implements
        OnTabChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.favorites_history);

        Intent intent = getIntent();
        int filterType = intent.getIntExtra(Constants.FILTER_TYPE,
                HistoryBase.FILTER_ALL);
        int tabIdx = intent.getIntExtra(
                Constants.FAVORITES_HISTORY_SELECTED_TAB_IDX, 0);

        setupTabs(tabIdx, filterType);
    }

    private void setupTabs(int tabIdx, int filterType) {
        TabHost tabHost = getTabHost();

        Intent favoritesIntent = new Intent(this, Favorites.class);
        if (tabIdx == 0) {
            favoritesIntent.putExtra(Constants.FILTER_TYPE, filterType);
        }
        tabHost.addTab(tabHost.newTabSpec("favorites").setIndicator(
                getResources().getString(R.string.favorites),
                getResources().getDrawable(R.drawable.ic_tab_favorites))
                .setContent(favoritesIntent));

        Intent historyIntent = new Intent(this, SearchHistory.class);
        if (tabIdx == 1) {
            historyIntent.putExtra(Constants.FILTER_TYPE, filterType);
        }
        tabHost.addTab(tabHost.newTabSpec("history").setIndicator(
                getResources().getString(R.string.search_history),
                getResources().getDrawable(R.drawable.ic_tab_history))
                .setContent(historyIntent));

        tabHost.setOnTabChangedListener(this);

        tabHost.setCurrentTab(tabIdx);
    }

    @Override
    public void onTabChanged(String tabId) {
        HistoryBase history = (HistoryBase) getLocalActivityManager()
                .getActivity(tabId);
        if (history != null) {
            history.refresh();
        }
    }

}
