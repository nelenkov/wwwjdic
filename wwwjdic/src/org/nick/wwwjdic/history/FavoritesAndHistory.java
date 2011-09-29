package org.nick.wwwjdic.history;

import org.nick.wwwjdic.ActionBarActivity;
import org.nick.wwwjdic.R;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.view.ViewPager;
import android.view.Window;

public class FavoritesAndHistory extends ActionBarActivity {

    public static final String EXTRA_FILTER_TYPE = "org.nick.wwwjdic.filterType";

    public static final String EXTRA_SELECTED_TAB_IDX = "org.nick.wwwjdic.favoritesAndHistorySelectedTabIdx";

    private static final boolean IS_HONEYCOMB = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

    private ViewPager viewPager;
    private TabsPagerAdapter tabsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.favorites_history);

        Intent intent = getIntent();
        int filterType = intent.getIntExtra(EXTRA_FILTER_TYPE,
                HistoryBase.FILTER_ALL);
        int tabIdx = intent.getIntExtra(EXTRA_SELECTED_TAB_IDX, 0);
        if (savedInstanceState != null) {
            tabIdx = savedInstanceState.getInt(EXTRA_SELECTED_TAB_IDX, 0);
        }

        Bundle favoritesArgs = new Bundle();
        Bundle historyArgs = new Bundle();
        if (tabIdx == 0) {
            favoritesArgs.putInt(EXTRA_FILTER_TYPE, filterType);
        }
        if (tabIdx == 1) {
            historyArgs.putInt(EXTRA_FILTER_TYPE, filterType);
        }

        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        viewPager = (ViewPager) findViewById(R.id.content);
        tabsAdapter = new TabsPagerAdapter(this, getSupportActionBar(),
                viewPager);
        ActionBar.Tab favoritesTab = getSupportActionBar().newTab();
        FavoritesFragment favoritesFragment = new FavoritesFragment();
        favoritesFragment.setArguments(favoritesArgs);
        favoritesTab.setText(R.string.favorites).setIcon(
                R.drawable.ic_tab_history);
        tabsAdapter.addTab(favoritesTab, favoritesFragment);

        ActionBar.Tab historyTab = getSupportActionBar().newTab();
        SearchHistoryFragment historyFragment = new SearchHistoryFragment();
        historyFragment.setArguments(historyArgs);
        historyTab.setIcon(R.drawable.ic_tab_favorites).setText(
                R.string.history);
        tabsAdapter.addTab(historyTab, historyFragment);

        getSupportActionBar().setSelectedNavigationItem(tabIdx);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_SELECTED_TAB_IDX, getSupportActionBar()
                .getSelectedNavigationIndex());
    }

    @Override
    protected void onStart() {
        super.onStart();

        getSupportActionBar().setDisplayShowHomeEnabled(IS_HONEYCOMB);
        getSupportActionBar().setDisplayHomeAsUpEnabled(IS_HONEYCOMB);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

}
