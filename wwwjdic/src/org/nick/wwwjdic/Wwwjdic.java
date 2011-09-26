package org.nick.wwwjdic;

import java.util.ArrayList;
import java.util.List;

import org.nick.wwwjdic.history.FavoritesAndHistory;
import org.nick.wwwjdic.history.FavoritesAndHistorySummaryView;
import org.nick.wwwjdic.history.HistoryBase;
import org.nick.wwwjdic.history.HistoryDbHelper;
import org.nick.wwwjdic.history.HistoryFragmentBase;
import org.nick.wwwjdic.hkr.RecognizeKanjiActivity;
import org.nick.wwwjdic.krad.KradChart;
import org.nick.wwwjdic.model.SearchCriteria;
import org.nick.wwwjdic.ocr.OcrActivity;
import org.nick.wwwjdic.utils.Analytics;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;

public class Wwwjdic extends ActionBarActivity {

    private static final String TAG = Wwwjdic.class.getSimpleName();

    public static final String EXTRA_SELECTED_TAB_IDX = "org.nick.wwwjdic.selectedTabIdx";
    public static final int DICTIONARY_TAB_IDX = 0;
    public static final int KANJI_TAB_IDX = 1;
    public static final int EXAMPLE_SEARRCH_TAB_IDX = 2;

    public static final String EXTRA_CRITERIA = "org.nick.wwwjdic.searchCriteria";
    public static final String EXTRA_SEARCH_TEXT = "org.nick.wwwjdic.searchKey";
    public static final String EXTRA_SEARCH_TYPE = "org.nick.wwwjdic.searchType";

    private static final int WHATS_NEW_DIALOG_ID = 1;
    private static final int DONATION_THANKS_DIALOG_ID = 2;

    private static final String DONATE_VERSION_PACKAGE = "org.nick.wwwjdic.donate";

    private static final boolean IS_HONEYCOMB = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

    class WwwjdicTabsPagerAdapter extends PagerAdapter implements
            ViewPager.OnPageChangeListener, ActionBar.TabListener {

        private FragmentActivity activity;
        private FragmentManager fragmentManager;
        private FragmentTransaction currentTransaction = null;

        private final ActionBar actionBar;
        private final ViewPager viewPager;

        private final List<Integer> tabLayouts = new ArrayList<Integer>();

        public WwwjdicTabsPagerAdapter(FragmentActivity activity,
                ActionBar actionBar, ViewPager pager) {
            this.activity = activity;
            this.fragmentManager = activity.getSupportFragmentManager();
            this.actionBar = actionBar;
            this.viewPager = pager;
            this.viewPager.setAdapter(this);
            this.viewPager.setOnPageChangeListener(this);
        }

        public void addTab(ActionBar.Tab tab, int layoutId) {
            tabLayouts.add(layoutId);
            actionBar.addTab(tab.setTabListener(this));
            notifyDataSetChanged();
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            int position = tab.getPosition();
            viewPager.setCurrentItem(position);
            filterHistoryFragments(position);
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset,
                int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            actionBar.setSelectedNavigationItem(position);
            filterHistoryFragments(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public int getCount() {
            return tabLayouts.size();
        }

        @Override
        public void startUpdate(View container) {
        }

        @Override
        public Object instantiateItem(View container, int position) {
            Log.d(TAG, "instantiateItem at position " + position);

            LayoutInflater inflater = activity.getLayoutInflater();
            View view = inflater.inflate(tabLayouts.get(position), null);

            setupHistorySummary(position, view);

            filterHistoryFragments(position);

            ((ViewPager) container).addView(view, 0);

            return view;
        }

        private void setupHistorySummary(int position, View view) {
            switch (position) {
            case 0:
                setupDictSummary(view);
                break;
            case 1:
                setupKanjiSummary(view);
                break;
            case 2:
                setupExamplesSummary(view);
                break;
            default:
                // do nothing
            }
        }

        private void filterHistoryFragments(int position) {
            switch (position) {
            case 0:
                filterFavoritesHistoryFragment(R.id.favorites_fragment,
                        HistoryBase.FILTER_DICT);
                filterFavoritesHistoryFragment(R.id.history_fragment,
                        HistoryBase.FILTER_DICT);
                break;
            case 1:
                filterFavoritesHistoryFragment(R.id.kanji_favorites_fragment,
                        HistoryBase.FILTER_KANJI);
                filterFavoritesHistoryFragment(R.id.kanji_history_fragment,
                        HistoryBase.FILTER_KANJI);
                break;
            case 2:
                filterFavoritesHistoryFragment(R.id.examples_history_fragment,
                        HistoryBase.FILTER_EXAMPLES);
                break;
            default:
                // do nothing
            }

        }

        @Override
        public void destroyItem(View container, int position, Object view) {
            Log.d(TAG, "destroyItem at position " + position);

            if (currentTransaction == null) {
                currentTransaction = fragmentManager.beginTransaction();
            }

            switch (position) {
            case 0:
                Fragment fragment = fragmentManager
                        .findFragmentById(R.id.dictionary_fragment);
                if (fragment != null) {
                    currentTransaction.remove(fragment);
                }
                removeFragmentIfExists(R.id.favorites_fragment);
                removeFragmentIfExists(R.id.history_fragment);
                break;
            case 1:
                fragment = fragmentManager
                        .findFragmentById(R.id.kanji_lookup_fragment);
                if (fragment != null) {
                    currentTransaction.remove(fragment);
                }
                removeFragmentIfExists(R.id.kanji_favorites_fragment);
                removeFragmentIfExists(R.id.kanji_history_fragment);
                break;
            case 2:
                fragment = fragmentManager
                        .findFragmentById(R.id.example_search_fragment);
                if (fragment != null) {
                    currentTransaction.remove(fragment);
                }
                removeFragmentIfExists(R.id.examples_history_fragment);
                break;
            }

            ((ViewPager) container).removeView((View) view);
        }

        private void removeFragmentIfExists(int id) {
            Fragment fragment = fragmentManager.findFragmentById(id);
            if (fragment != null) {
                currentTransaction.remove(fragment);
            }
        }

        @Override
        public void finishUpdate(View container) {
            if (currentTransaction != null) {
                currentTransaction.commit();
                currentTransaction = null;
                fragmentManager.executePendingTransactions();
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }
    }

    private static final int NUM_RECENT_HISTORY_ENTRIES = 5;

    private ViewPager viewPager;
    // private TabsPagerAdapter tabsAdapter;
    private WwwjdicTabsPagerAdapter tabsAdapter;

    private HistoryDbHelper dbHelper;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        setupTabs();

        dbHelper = HistoryDbHelper.getInstance(this);

        invalidateOptionsMenu();

        if (!isDonateVersion()
                || WwwjdicPreferences.isDonationThanksShown(this)) {
            showWhatsNew();
        }

        showDonationThanks();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_about:
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        case R.id.menu_ocr:
            intent = new Intent(this, OcrActivity.class);

            startActivity(intent);
            return true;
        case R.id.menu_settings:
            intent = new Intent(this, WwwjdicPreferences.class);

            startActivity(intent);
            return true;
        case R.id.menu_draw:
            intent = new Intent(this, RecognizeKanjiActivity.class);

            startActivity(intent);
            return true;
        case R.id.menu_favorites_history:
            intent = new Intent(this, FavoritesAndHistory.class);

            startActivity(intent);
            return true;
        case R.id.menu_multi_radical:
            intent = new Intent(this, KradChart.class);

            startActivity(intent);
            return true;
        default:
            // do nothing
        }

        return super.onOptionsItemSelected(item);
    }

    private void showDonationThanks() {
        if (!isDonateVersion()) {
            return;
        }

        boolean thanksShown = WwwjdicPreferences.isDonationThanksShown(this);
        if (!thanksShown) {
            WwwjdicPreferences.setDonationThanksShown(this);
            showDialog(DONATION_THANKS_DIALOG_ID);
        }
    }

    private boolean isDonateVersion() {
        String appPackage = getApplication().getPackageName();

        return DONATE_VERSION_PACKAGE.equals(appPackage);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Analytics.startSession(this);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Analytics.endSession(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void showWhatsNew() {
        boolean whatsNewShown = WwwjdicPreferences.isWhatsNewShown(this,
                getVersionName());
        if (!whatsNewShown) {
            WwwjdicPreferences.setWhantsNewShown(this, getVersionName());
            showDialog(WHATS_NEW_DIALOG_ID);
        }
    }

    private void setupTabs() {
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        if (!IS_HONEYCOMB) {
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }
        Bundle extras = getIntent().getExtras();

        ActionBar.Tab dictionaryTab = getSupportActionBar().newTab();
        DictionaryFragment dictionary = new DictionaryFragment();
        if (extras != null) {
            dictionary.setArguments(extras);
        }

        dictionaryTab.setIcon(R.drawable.ic_tab_dict);
        if (IS_HONEYCOMB) {
            dictionaryTab.setText(R.string.dictionary);
        }
        viewPager = (ViewPager) findViewById(R.id.content);
        // tabsAdapter = new TabsPagerAdapter(this, getSupportActionBar(),
        // viewPager);
        tabsAdapter = new WwwjdicTabsPagerAdapter(this, getSupportActionBar(),
                viewPager);
        // tabsAdapter.addTab(dictionaryTab, dictionary);
        tabsAdapter.addTab(dictionaryTab, R.layout.dict_lookup_tab);

        ActionBar.Tab kanjiTab = getSupportActionBar().newTab();
        WwwjdicFragmentBase kanjiLookup = new KanjiLookupFragment();
        if (extras != null) {
            kanjiLookup.setArguments(extras);
        }

        kanjiTab.setIcon(R.drawable.ic_tab_kanji);
        if (IS_HONEYCOMB) {
            kanjiTab.setText(R.string.kanji_lookup);
        }
        // tabsAdapter.addTab(kanjiTab, kanjiLookup);
        tabsAdapter.addTab(kanjiTab, R.layout.kanji_lookup_tab);

        ActionBar.Tab examplesTab = getSupportActionBar().newTab();
        ExampleSearchFragment exampleSearch = new ExampleSearchFragment();
        if (extras != null) {
            exampleSearch.setArguments(extras);
        }

        examplesTab.setIcon(R.drawable.ic_tab_example);
        if (IS_HONEYCOMB) {
            examplesTab.setText(R.string.example_search);
        }
        // tabsAdapter.addTab(examplesTab, exampleSearch);
        tabsAdapter.addTab(examplesTab, R.layout.example_search_tab);

        getSupportActionBar().setSelectedNavigationItem(DICTIONARY_TAB_IDX);
        if (extras != null) {
            int selectedTab = extras.getInt(EXTRA_SELECTED_TAB_IDX, -1);
            if (selectedTab != -1) {
                getSupportActionBar().setSelectedNavigationItem(selectedTab);
            }

            String searchKey = extras.getString(EXTRA_SEARCH_TEXT);
            int searchType = extras.getInt(EXTRA_SEARCH_TYPE);
            if (searchKey != null) {
                switch (searchType) {
                case SearchCriteria.CRITERIA_TYPE_DICT:
                    getSupportActionBar().setSelectedNavigationItem(
                            DICTIONARY_TAB_IDX);
                    break;
                case SearchCriteria.CRITERIA_TYPE_KANJI:
                    getSupportActionBar().setSelectedNavigationItem(
                            KANJI_TAB_IDX);
                    break;
                case SearchCriteria.CRITERIA_TYPE_EXAMPLES:
                    getSupportActionBar().setSelectedNavigationItem(
                            EXAMPLE_SEARRCH_TAB_IDX);
                    break;
                default:
                    // do nothing
                }
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;

        switch (id) {
        case WHATS_NEW_DIALOG_ID:
            dialog = createWhatsNewDialog();
            break;
        case DONATION_THANKS_DIALOG_ID:
            dialog = createDonationThanksDialog();
            break;
        default:
            dialog = null;
        }

        return dialog;
    }

    private Dialog createDonationThanksDialog() {
        DialogInterface.OnClickListener okAction = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                showWhatsNew();

            }
        };
        return createInfoDialog(R.string.donation_thanks_title,
                R.string.donation_thanks, okAction);
    }

    private Dialog createWhatsNewDialog() {
        DialogInterface.OnClickListener okAction = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

            }
        };
        return createInfoDialog(R.string.whats_new_title, R.string.whats_new,
                okAction);
    }

    private Dialog createInfoDialog(int titleId, int messageId,
            DialogInterface.OnClickListener okAction) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String titleTemplate = getResources().getString(titleId);
        String title = String.format(titleTemplate, getVersionName());
        builder.setTitle(title);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setMessage(messageId);
        builder.setPositiveButton(R.string.ok, okAction);

        return builder.create();
    }

    private String getVersionName() {
        return WwwjdicApplication.getVersion();
    }

    private void setupDictSummary(View view) {
        FavoritesAndHistorySummaryView dictHistorySummary = (FavoritesAndHistorySummaryView) view
                .findViewById(R.id.dict_history_summary);
        if (dictHistorySummary == null) {
            return;
        }

        dbHelper.beginTransaction();
        try {
            long numAllFavorites = dbHelper.getDictFavoritesCount();
            List<String> recentFavorites = dbHelper
                    .getRecentDictFavorites(NUM_RECENT_HISTORY_ENTRIES);
            long numAllHistory = dbHelper.getDictHistoryCount();
            List<String> recentHistory = dbHelper
                    .getRecentDictHistory(NUM_RECENT_HISTORY_ENTRIES);

            dictHistorySummary
                    .setFavoritesFilterType(HistoryDbHelper.FAVORITES_TYPE_DICT);
            dictHistorySummary
                    .setHistoryFilterType(HistoryDbHelper.HISTORY_SEARCH_TYPE_DICT);
            dictHistorySummary.setRecentEntries(numAllFavorites,
                    recentFavorites, numAllHistory, recentHistory);
            dbHelper.setTransactionSuccessful();
        } finally {
            dbHelper.endTransaction();
        }
    }

    private void setupKanjiSummary(View view) {
        FavoritesAndHistorySummaryView kanjiHistorySummary = (FavoritesAndHistorySummaryView) view
                .findViewById(R.id.kanji_history_summary);
        if (kanjiHistorySummary == null) {
            return;
        }

        dbHelper.beginTransaction();
        try {
            long numAllFavorites = dbHelper.getKanjiFavoritesCount();
            List<String> recentFavorites = dbHelper
                    .getRecentKanjiFavorites(NUM_RECENT_HISTORY_ENTRIES);
            long numAllHistory = dbHelper.getKanjiHistoryCount();
            List<String> recentHistory = dbHelper
                    .getRecentKanjiHistory(NUM_RECENT_HISTORY_ENTRIES);

            kanjiHistorySummary
                    .setFavoritesFilterType(HistoryDbHelper.FAVORITES_TYPE_KANJI);
            kanjiHistorySummary
                    .setHistoryFilterType(HistoryDbHelper.HISTORY_SEARCH_TYPE_KANJI);
            kanjiHistorySummary.setRecentEntries(numAllFavorites,
                    recentFavorites, numAllHistory, recentHistory);
            dbHelper.setTransactionSuccessful();
        } finally {
            dbHelper.endTransaction();
        }
    }

    private void setupExamplesSummary(View view) {
        FavoritesAndHistorySummaryView examplesHistorySummary = (FavoritesAndHistorySummaryView) view
                .findViewById(R.id.examples_history_summary);
        if (examplesHistorySummary == null) {
            return;
        }

        dbHelper.beginTransaction();
        try {
            long numAllHistory = dbHelper.getExamplesHistoryCount();
            List<String> recentHistory = dbHelper
                    .getRecentExamplesHistory(NUM_RECENT_HISTORY_ENTRIES);

            examplesHistorySummary
                    .setHistoryFilterType(HistoryDbHelper.HISTORY_SEARCH_TYPE_EXAMPLES);
            examplesHistorySummary.setRecentEntries(0, null, numAllHistory,
                    recentHistory);
            dbHelper.setTransactionSuccessful();
        } finally {
            dbHelper.endTransaction();
        }
    }

    private void filterFavoritesHistoryFragment(int fragmentId, int filterType) {
        HistoryFragmentBase fragment = (HistoryFragmentBase) getSupportFragmentManager()
                .findFragmentById(fragmentId);
        if (fragment != null && !fragment.isDetached()
                && fragment.getActivity() != null) {
            fragment.setSelectedFilter(filterType);
            fragment.filter();
        }
    }

}
