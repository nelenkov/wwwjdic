package org.nick.wwwjdic;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class FavoritesAndHistory extends TabActivity {

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
                .setContent(new Intent(this, SearchHistory.class)));
        tabHost.addTab(tabHost.newTabSpec("history").setIndicator("History",
                getResources().getDrawable(R.drawable.ic_tab_history))
                .setContent(new Intent(this, SearchHistory.class)));

        tabHost.setCurrentTab(0);
    }
}
