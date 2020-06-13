
package org.nick.wwwjdic.history;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ListAdapter;

import org.nick.wwwjdic.ActionBarActivity;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.Wwwjdic;
import org.nick.wwwjdic.history.HistoryFragmentBase.ConfirmDeleteDialog;
import org.nick.wwwjdic.utils.Dialogs;

import java.io.File;

import androidx.fragment.app.DialogFragment;
import androidx.viewpager.widget.ViewPager;

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

        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(Boolean.FALSE);

        setContentView(R.layout.favorites_history);
        // setTitle(R.string.favorites_hist);

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

        //getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        viewPager = (ViewPager) findViewById(R.id.content);
        tabsAdapter = new TabsPagerAdapter(this, getSupportActionBar(),
                viewPager);
        androidx.appcompat.app.ActionBar.Tab favoritesTab = getSupportActionBar().newTab();
        FavoritesFragment favoritesFragment = new FavoritesFragment();
        favoritesFragment.setArguments(favoritesArgs);
        favoritesTab.setText(R.string.favorites).setIcon(
                R.drawable.ic_tab_favorites);
        tabsAdapter.addTab(favoritesTab, favoritesFragment);

        androidx.appcompat.app.ActionBar.Tab historyTab = getSupportActionBar().newTab();
        SearchHistoryFragment historyFragment = new SearchHistoryFragment();
        historyFragment.setArguments(historyArgs);
        historyTab.setIcon(R.drawable.ic_tab_history).setText(
                R.string.history);
        tabsAdapter.addTab(historyTab, historyFragment);

        //getSupportActionBar().setSelectedNavigationItem(tabIdx);

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
    public boolean onPrepareOptionsMenu(Menu menu) {
        // FIXME
        int currentTabIdx = 0;//getSupportActionBar().getSelectedNavigationIndex();
        HistoryFragmentBase currentTab = (HistoryFragmentBase) tabsAdapter
                .getItem(currentTabIdx);
        ListAdapter adapter = currentTab.getListAdapter();
        final boolean hasItems = adapter == null ? false
                : adapter.getCount() > 0;
        File backupFile = new File(currentTab.getImportExportFilename());

        boolean importEnabled = backupFile.exists();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            importEnabled = true;
        }
        menu.findItem(R.id.menu_import).setEnabled(importEnabled);
        menu.findItem(R.id.menu_export).setEnabled(hasItems);
        menu.findItem(R.id.menu_delete).setEnabled(hasItems);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int currentTabIdx = getSupportActionBar().getSelectedNavigationIndex();
        HistoryFragmentBase currentTab = (HistoryFragmentBase) tabsAdapter
                .getItem(currentTabIdx);

        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, Wwwjdic.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_import) {
            currentTab.importItems();
            invalidateOptionsMenu();
        } else if (item.getItemId() == R.id.menu_export) {
            currentTab.exportItems();
            invalidateOptionsMenu();
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
