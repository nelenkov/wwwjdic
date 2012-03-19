package org.nick.wwwjdic.history;

import org.nick.wwwjdic.ActionBarActivity;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.utils.Dialogs;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Window;

public class FavoritesAndHistory extends ActionBarActivity {

    public static final String EXTRA_FILTER_TYPE = "org.nick.wwwjdic.filterType";

    public static final String EXTRA_SELECTED_TAB_IDX = "org.nick.wwwjdic.favoritesAndHistorySelectedTabIdx";

    private static final String FAVORITES_EXPORT_TIP_DIALOG = "tips_favorites_export";

    private ViewPager viewPager;
    private TabsPagerAdapter tabsAdapter;

    public static final int FILTER_ALL = -1;
    public static final int FILTER_DICT = 0;
    public static final int FILTER_KANJI = 1;
    public static final int FILTER_EXAMPLES = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);

        setContentView(R.layout.favorites_history);
        //        setTitle(R.string.favorites_hist);

        // collapse action bar
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Intent intent = getIntent();
        int filterType = intent.getIntExtra(EXTRA_FILTER_TYPE, FILTER_ALL);
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

        Dialogs.showTipOnce(this, FAVORITES_EXPORT_TIP_DIALOG,
                R.string.tips_favorites_export);
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

        //        boolean showHome = UIUtils.isHoneycombTablet(this)
        //                || UIUtils.isPortrait(this);
        //        boolean showTitle = !UIUtils.isHoneycombTablet(this)
        //                && UIUtils.isPortrait(this);
        //        getSupportActionBar().setDisplayShowHomeEnabled(showHome);
        //        getSupportActionBar().setDisplayHomeAsUpEnabled(showHome);
        //        getSupportActionBar().setDisplayShowTitleEnabled(showTitle);
    }

}
