package org.nick.wwwjdic.history;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;

import com.google.android.material.tabs.TabLayout;

import org.nick.wwwjdic.ActionBarActivity;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.history.HistoryFragmentBase.ConfirmDeleteDialog;
import org.nick.wwwjdic.utils.Dialogs;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager.widget.ViewPager;

public class FavoritesAndHistory extends ActionBarActivity {

    private static final String TAG = FavoritesAndHistory.class.getSimpleName();

    public static final String EXTRA_FILTER_TYPE = "org.nick.wwwjdic.filterType";

    public static final String EXTRA_SELECTED_TAB_IDX = "org.nick.wwwjdic.favoritesAndHistorySelectedTabIdx";
    public static final int FAVORITES_TAB_IDX = 0;
    public static final int HISTORY_TAB_IDX = 1;

    public static final int FILTER_ALL = -1;
    public static final int FILTER_DICT = 0;
    public static final int FILTER_KANJI = 1;
    public static final int FILTER_EXAMPLES = 2;

    private static final String FAVORITES_EXPORT_TIP_DIALOG = "tips_favorites_export";

    private static final String SEARCH_HISTORY_FRAGMENT_KEY = "searchHistoryFragment";
    private static final String FAVORITES_FRAGMENT_KEY = "favoritesFragment";

    private ViewPager viewPager;
    private TabsPagerAdapter tabsAdapter;
    private TabLayout tabLayout;

    private Toolbar toolbar;

    private HistoryFragmentBase favoritesFragment;
    private HistoryFragmentBase searchHistoryFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.favorites_history);
        // setTitle(R.string.favorites_hist);

        // collapse action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        toolbar.inflateMenu(R.menu.history_favorites);
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View child = toolbar.getChildAt(i);
            if (child instanceof ActionMenuView) {
                child.getLayoutParams().width = ActionMenuView.LayoutParams.MATCH_PARENT;
            }
        }

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });
        toolbar.setOnCreateContextMenuListener(new Toolbar.OnCreateContextMenuListener() {

            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {

            }
        });

        tabLayout = findViewById(R.id.tablayout);
        tabLayout.setBackgroundColor(getResources().getColor(R.color.ab_blue));
        tabLayout.setSelectedTabIndicatorColor(getResources().getColor(R.color.ab_blue_dark));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (viewPager.getCurrentItem() != tab.getPosition()) {
                    viewPager.setCurrentItem(tab.getPosition());
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        viewPager = findViewById(R.id.content);
        tabsAdapter = new TabsPagerAdapter(this, viewPager);
        viewPager.setAdapter(tabsAdapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageSelected(int i) {
                Menu menu = toolbar.getMenu();
                onPrepareOptionsMenu(menu);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

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

        if (savedInstanceState == null) {
            favoritesFragment = new FavoritesFragment();
            favoritesFragment.setArguments(favoritesArgs);
        } else {
            favoritesFragment = (HistoryFragmentBase) getSupportFragmentManager()
                    .getFragment(savedInstanceState, FAVORITES_FRAGMENT_KEY);
        }
        tabsAdapter.addTab(favoritesFragment);

        if (savedInstanceState == null) {
            searchHistoryFragment = new SearchHistoryFragment();
            searchHistoryFragment.setArguments(historyArgs);
        } else {
            searchHistoryFragment = (HistoryFragmentBase) getSupportFragmentManager()
                    .getFragment(savedInstanceState,
                            SEARCH_HISTORY_FRAGMENT_KEY);
        }
        tabsAdapter.addTab(searchHistoryFragment);

        tabLayout.setScrollPosition(tabIdx,0f,true);
        //tabLayout.setupWithViewPager(viewPager);
        viewPager.setCurrentItem(tabIdx);

        Dialogs.showTipOnce(this, FAVORITES_EXPORT_TIP_DIALOG,
                R.string.tips_favorites_export);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(EXTRA_SELECTED_TAB_IDX, tabLayout.getSelectedTabPosition());

        getSupportFragmentManager().putFragment(outState,
                FAVORITES_FRAGMENT_KEY, favoritesFragment);
        getSupportFragmentManager().putFragment(outState,
                SEARCH_HISTORY_FRAGMENT_KEY, searchHistoryFragment);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // boolean showHome = UIUtils.isHoneycombTablet(this)
        // || UIUtils.isPortrait(this);
        // boolean showTitle = !UIUtils.isHoneycombTablet(this)
        // && UIUtils.isPortrait(this);
        // getSupportActionBar().setDisplayShowHomeEnabled(showHome);
        // getSupportActionBar().setDisplayHomeAsUpEnabled(showHome);
        // getSupportActionBar().setDisplayShowTitleEnabled(showTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history_favorites, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        supportInvalidateOptionsMenu();

        int tabIdx = getIntent().getIntExtra(EXTRA_SELECTED_TAB_IDX, 0);
        Log.d(TAG, "tabIdx: " + tabIdx);
        tabLayout.setScrollPosition(tabIdx,0f,true);
        viewPager.setCurrentItem(tabIdx);

        int currentTabIdx = tabLayout.getSelectedTabPosition();
        boolean showFilter = hasFilter(currentTabIdx);
        MenuItem filterItem = toolbar.getMenu().findItem(R.id.menu_filter);
        if (filterItem != null) {
            filterItem.setVisible(showFilter);
        }
    }

    protected boolean hasFilter(int tabIdx) {
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int currentTabIdx = tabLayout.getSelectedTabPosition();
        HistoryFragmentBase currentTab = (HistoryFragmentBase) tabsAdapter
                .getItem(currentTabIdx);
        ListAdapter adapter = currentTab.getListAdapter();
        final boolean hasItems = adapter != null && adapter.getCount() > 0;
        File backupFile = new File(currentTab.getImportExportFilename());

        boolean importEnabled = backupFile.exists();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            importEnabled = true;
        }
        menu.findItem(R.id.menu_import).setEnabled(importEnabled);
        menu.findItem(R.id.menu_export).setEnabled(hasItems);
        menu.findItem(R.id.menu_delete).setEnabled(hasItems);

        boolean showFilter = hasFilter(currentTabIdx);
        menu.findItem(R.id.menu_filter).setVisible(showFilter);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int currentTabIdx = tabLayout.getSelectedTabPosition();
        HistoryFragmentBase currentTab = (HistoryFragmentBase) tabsAdapter
                .getItem(currentTabIdx);

        if (item.getItemId() == android.R.id.home) {
            PackageManager pm = getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(getPackageName());
            if (intent == null) {
                return false;
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_import) {
            currentTab.importItems();
            supportInvalidateOptionsMenu();
        } else if (item.getItemId() == R.id.menu_export) {
            currentTab.exportItems();
            supportInvalidateOptionsMenu();
        } else if (item.getItemId() == R.id.menu_filter) {
            currentTab.showFilterDialog();
        } else if (item.getItemId() == R.id.menu_delete) {
            DialogFragment confirmDeleteDialog = ConfirmDeleteDialog
                    .newInstance(currentTab);
            confirmDeleteDialog.show(getSupportFragmentManager(),
                    "confirmDeleteDialog");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
