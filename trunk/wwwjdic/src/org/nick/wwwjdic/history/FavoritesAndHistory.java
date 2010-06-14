package org.nick.wwwjdic.history;

import org.nick.wwwjdic.R;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

public class FavoritesAndHistory extends TabActivity implements
        OnTabChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favorites_history);

        setupTabs();
    }

    private void setupTabs() {
        TabHost tabHost = getTabHost();

        tabHost.addTab(tabHost.newTabSpec("favorites").setIndicator(
                "Favorites",
                getResources().getDrawable(R.drawable.ic_tab_favorites))
                .setContent(new Intent(this, Favorites.class)));
        tabHost.addTab(tabHost.newTabSpec("history").setIndicator(
                "Search history",
                getResources().getDrawable(R.drawable.ic_tab_history))
                .setContent(new Intent(this, SearchHistory.class)));

        tabHost.setOnTabChangedListener(this);
        tabHost.setCurrentTab(0);
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
