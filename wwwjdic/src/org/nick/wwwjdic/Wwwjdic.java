package org.nick.wwwjdic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;

import org.nick.wwwjdic.history.FavoritesAndHistory;
import org.nick.wwwjdic.history.FavoritesAndHistorySummaryView;
import org.nick.wwwjdic.history.HistoryDbHelper;
import org.nick.wwwjdic.history.HistoryFragmentBase;
import org.nick.wwwjdic.hkr.RecognizeKanjiActivity;
import org.nick.wwwjdic.krad.KradChart;
import org.nick.wwwjdic.model.SearchCriteria;
import org.nick.wwwjdic.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

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

    private static final int REQUEST_CODE_PERMISSIONS = 1;

    private static final String DICT_LOOKUP_FRAGMENT_KEY = "dictLookupFragment";
    private static final String KANJI_LOOKUP_FRAGMENT_KEY = "kanjiLookupFragment";
    private static final String EXAMPLE_LOOKUP_FRAGMENT_KEY = "exampleLookupFragment";

    private final static String[] PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    class WwwjdicTabsPagerAdapter extends FragmentStatePagerAdapter implements
            ViewPager.OnPageChangeListener, TabLayout.OnTabSelectedListener {

        private AppCompatActivity activity;
        private FragmentManager fragmentManager;
        private FragmentTransaction currentTransaction = null;

        private final ViewPager viewPager;

        private final List<Fragment> tabs = new ArrayList<>();
        private final List<String> tabTitles = new ArrayList<>();
        private final List<Integer> tabIcons = new ArrayList<>();
        private final List<Integer> selectedTabIcons = new ArrayList<>();

        public WwwjdicTabsPagerAdapter(AppCompatActivity activity, ViewPager pager) {
            super(activity.getSupportFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.activity = activity;
            this.fragmentManager = activity.getSupportFragmentManager();
            this.viewPager = pager;
            this.viewPager.setAdapter(this);
            this.viewPager.addOnPageChangeListener(this);
        }

        public void addTab(Fragment tabFragment, String title, int icon, int selectedIcon) {
            tabFragment.setHasOptionsMenu(true);
            tabs.add(tabFragment);
            tabTitles.add(title);
            tabIcons.add(icon);
            selectedTabIcons.add(selectedIcon);

            notifyDataSetChanged();
        }

        @Override
        public void onPageScrolled(int position, float positionOffset,
                int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public int getCount() {
            return tabs.size();
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return tabs.get(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            //return tabTitles.get(position);
            return null;
        }

        public int getTabIcon(int idx) {
            return tabIcons.get(idx);
        }

        public int getSelectedTabIcon(int idx) {
            return selectedTabIcons.get(idx);
        }

        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            tab.setIcon(selectedTabIcons.get(tab.getPosition()));
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
            tab.setIcon(tabIcons.get(tab.getPosition()));
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    }

    private static final int NUM_RECENT_HISTORY_ENTRIES = 5;

    private ViewPager viewPager;
    private WwwjdicTabsPagerAdapter tabsAdapter;
    private TabLayout tabLayout;

    private Toolbar toolbar;

    private HistoryDbHelper dbHelper;

    private DictionaryLookpFragment dictLookupFragment;
    private KanjiLookpFragment kanjiLookupFragment;
    private ExampleLookupFragment exampleLookupFragment;

    private boolean hasCamera;

    @Override
    @SuppressLint("NewApi")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        // collapse action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        setupTabs(savedInstanceState);

        dbHelper = HistoryDbHelper.getInstance(this);

        hasCamera = getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_ANY);

        invalidateOptionsMenu();

        if (!isDonateVersion()
                || WwwjdicPreferences.isDonationThanksShown(this)) {
            showWhatsNew();
        }

        showDonationThanks();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_SELECTED_TAB_IDX, tabLayout.getSelectedTabPosition());

//        getSupportFragmentManager().putFragment(outState,
//                DICT_LOOKUP_FRAGMENT_KEY, dictLookupFragment);
//        getSupportFragmentManager().putFragment(outState,
//                KANJI_LOOKUP_FRAGMENT_KEY, kanjiLookupFragment);
//        getSupportFragmentManager().putFragment(outState,
//                EXAMPLE_LOOKUP_FRAGMENT_KEY, exampleLookupFragment);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermissions() {
        requestPermissions(PERMISSIONS, REQUEST_CODE_PERMISSIONS);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permission, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permission, grantResults);

        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.ext_storage_perm_message, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //menu.findItem(R.id.menu_ocr).setEnabled(hasCamera);
        menu.findItem(R.id.menu_ocr).setEnabled(false);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_ocr) {
            //Intent intent = new Intent(this, OcrActivity.class);
            //startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_settings) {
            Intent intent = new Intent(this,
                    UIUtils.isHoneycomb() ? WwwjdicPreferencesHC.class
                            : WwwjdicPreferences.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_draw) {
            Intent intent = new Intent(this, RecognizeKanjiActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_favorites_history) {
            Intent intent = new Intent(this, FavoritesAndHistory.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_multi_radical) {
            Intent intent = new Intent(this, KradChart.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            Activities.home(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("deprecation")
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

        if (WwwjdicPreferences.isPopupKeyboard(this)) {
            showKeyboard();
        }
    }

    private void showKeyboard() {
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        int position = tabLayout.getSelectedTabPosition();
        filterHistoryFragments(position);
        updateHistorySummary(position);
    }

    private void updateHistorySummary(int position) {
        updateHistorySummary(position, viewPager);
    }

    private void updateHistorySummary(int position, View view) {
        switch (position) {
        case 0:
            updateDictSummary(view);
            break;
        case 1:
            updateKanjiSummary(view);
            break;
        case 2:
            updateExamplesSummary(view);
            break;
        default:
            // do nothing
        }
    }

    private void filterHistoryFragments(int position) {
        switch (position) {
        case 0:
            filterFavoritesHistoryFragment(R.id.favorites_fragment,
                    FavoritesAndHistory.FILTER_DICT);
            filterFavoritesHistoryFragment(R.id.history_fragment,
                    FavoritesAndHistory.FILTER_DICT);
            break;
        case 1:
            filterFavoritesHistoryFragment(R.id.kanji_favorites_fragment,
                    FavoritesAndHistory.FILTER_KANJI);
            filterFavoritesHistoryFragment(R.id.kanji_history_fragment,
                    FavoritesAndHistory.FILTER_KANJI);
            break;
        case 2:
            filterFavoritesHistoryFragment(R.id.examples_history_fragment,
                    FavoritesAndHistory.FILTER_EXAMPLES);
            break;
        default:
            // do nothing
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    private void showWhatsNew() {
        boolean whatsNewShown = WwwjdicPreferences.isWhatsNewShown(this,
                getVersionName());
        if (!whatsNewShown) {
            WwwjdicPreferences.setWhantsNewShown(this, getVersionName());
            showDialog(WHATS_NEW_DIALOG_ID);
        }
    }

    private void setupTabs(Bundle savedInstanceState) {
        toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.main);
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

        //dictTab = findViewById(R.id.dict_tab);
        //kanjiTab = findViewById(R.id.kanji_tab);
        //examplesTab = findViewById(R.id.examples_tab);

        viewPager = findViewById(R.id.content);
        tabsAdapter = new WwwjdicTabsPagerAdapter(this, viewPager);

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

        //if (savedInstanceState == null) {
            dictLookupFragment = new DictionaryLookpFragment();
        //} else {
//            dictLookupFragment = (DictionaryLookpFragment) getSupportFragmentManager()
//                    .getFragment(savedInstanceState, DICT_LOOKUP_FRAGMENT_KEY);
        //}
        tabsAdapter.addTab(dictLookupFragment, "Dict", R.drawable.ic_dict_tab_unselected,
                R.drawable.ic_dict_tab_selected);

        // TODO: fix classname
//        if (savedInstanceState == null) {
            kanjiLookupFragment = new KanjiLookpFragment();
//        } else {
//            kanjiLookupFragment = (KanjiLookpFragment) getSupportFragmentManager()
//                    .getFragment(savedInstanceState, KANJI_LOOKUP_FRAGMENT_KEY);
//        }
        tabsAdapter.addTab(kanjiLookupFragment, "Kanji", R.drawable.ic_kanji_tab_unselected,
                R.drawable.ic_kanji_tab_selected);

        //if (savedInstanceState == null) {
            exampleLookupFragment = new ExampleLookupFragment();
        //} else {
//            exampleLookupFragment = (ExampleLookupFragment) getSupportFragmentManager()
//                    .getFragment(savedInstanceState, EXAMPLE_LOOKUP_FRAGMENT_KEY);
        //}
        tabsAdapter.addTab(exampleLookupFragment, "Examples", R.drawable.ic_example_tab_unselected,
                R.drawable.ic_example_tab_selected);

        viewPager.setAdapter(tabsAdapter);
        tabLayout.setupWithViewPager(viewPager);

        tabLayout.getTabAt(0).setIcon(R.drawable.ic_dict_tab_selected);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_kanji_tab_unselected);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_example_tab_unselected);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageSelected(int idx) {
                Menu menu = toolbar.getMenu();
                onPrepareOptionsMenu(menu);

                for (int i = 0; i < tabLayout.getTabCount(); i++) {
                    tabLayout.getTabAt(i).setIcon(tabsAdapter.getTabIcon(i));
                }

                tabLayout.getTabAt(idx).setIcon(tabsAdapter.getSelectedTabIcon(idx));
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });


        Bundle extras = getIntent().getExtras();
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

    private static class HistorySummaryParams {
        long numAllFavorites;
        long numAllHistory;
        List<String> recentHistory;
        List<String> recentFavorites;
    }

    private void updateDictSummary(View view) {
        final FavoritesAndHistorySummaryView dictHistorySummary = view.findViewById(R.id.dict_history_summary);
        if (dictHistorySummary == null) {
            return;
        }

        new AsyncTask<Void, Void, HistorySummaryParams>() {

            @Override
            protected HistorySummaryParams doInBackground(Void... params) {
                HistorySummaryParams result = new HistorySummaryParams();
                dbHelper.beginTransaction();

                try {
                    result.numAllFavorites = dbHelper.getDictFavoritesCount();
                    result.recentFavorites = dbHelper
                            .getRecentDictFavorites(NUM_RECENT_HISTORY_ENTRIES);
                    result.numAllHistory = dbHelper.getDictHistoryCount();
                    result.recentHistory = dbHelper
                            .getRecentDictHistory(NUM_RECENT_HISTORY_ENTRIES);
                    dbHelper.setTransactionSuccessful();

                    return result;
                } catch (Exception e) {
                    Log.w(TAG,
                            "error getting history/favorites summary: "
                                    + e.getMessage(), e);

                    return null;
                } finally {
                    dbHelper.endTransaction();
                }
            }

            @Override
            protected void onPostExecute(HistorySummaryParams result) {
                if (result == null) {
                    return;
                }

                dictHistorySummary
                        .setFavoritesFilterType(HistoryDbHelper.FAVORITES_TYPE_DICT);
                dictHistorySummary
                        .setHistoryFilterType(HistoryDbHelper.HISTORY_SEARCH_TYPE_DICT);
                dictHistorySummary.setRecentEntries(result.numAllFavorites,
                        result.recentFavorites, result.numAllHistory,
                        result.recentHistory);
            }

        }.execute();
    }

    private void updateKanjiSummary(View view) {
        final FavoritesAndHistorySummaryView kanjiHistorySummary = (FavoritesAndHistorySummaryView) view
                .findViewById(R.id.kanji_history_summary);
        if (kanjiHistorySummary == null) {
            return;
        }

        new AsyncTask<Void, Void, HistorySummaryParams>() {

            @Override
            protected HistorySummaryParams doInBackground(Void... params) {
                HistorySummaryParams result = new HistorySummaryParams();

                dbHelper.beginTransaction();
                try {
                    result.numAllFavorites = dbHelper.getKanjiFavoritesCount();
                    result.recentFavorites = dbHelper
                            .getRecentKanjiFavorites(NUM_RECENT_HISTORY_ENTRIES);
                    result.numAllHistory = dbHelper.getKanjiHistoryCount();
                    result.recentHistory = dbHelper
                            .getRecentKanjiHistory(NUM_RECENT_HISTORY_ENTRIES);

                    dbHelper.setTransactionSuccessful();
                } finally {
                    dbHelper.endTransaction();
                }

                return result;
            }

            @Override
            protected void onPostExecute(HistorySummaryParams result) {
                if (result == null) {
                    return;
                }

                kanjiHistorySummary
                        .setFavoritesFilterType(HistoryDbHelper.FAVORITES_TYPE_KANJI);
                kanjiHistorySummary
                        .setHistoryFilterType(HistoryDbHelper.HISTORY_SEARCH_TYPE_KANJI);
                kanjiHistorySummary.setRecentEntries(result.numAllFavorites,
                        result.recentFavorites, result.numAllHistory,
                        result.recentHistory);
            }
        }.execute();
    }

    private void updateExamplesSummary(View view) {
        final FavoritesAndHistorySummaryView examplesHistorySummary = view.findViewById(R.id.examples_history_summary);
        if (examplesHistorySummary == null) {
            return;
        }

        new AsyncTask<Void, Void, HistorySummaryParams>() {

            @Override
            protected HistorySummaryParams doInBackground(Void... params) {
                HistorySummaryParams result = new HistorySummaryParams();
                dbHelper.beginTransaction();
                try {
                    result.numAllHistory = dbHelper.getExamplesHistoryCount();
                    result.recentHistory = dbHelper
                            .getRecentExamplesHistory(NUM_RECENT_HISTORY_ENTRIES);
                    dbHelper.setTransactionSuccessful();
                } finally {
                    dbHelper.endTransaction();
                }

                return result;
            }

            @Override
            protected void onPostExecute(HistorySummaryParams result) {
                if (result == null) {
                    return;
                }

                examplesHistorySummary
                        .setHistoryFilterType(HistoryDbHelper.HISTORY_SEARCH_TYPE_EXAMPLES);
                examplesHistorySummary.setRecentEntries(0, null,
                        result.numAllHistory, result.recentHistory);
            }
        }.execute();
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
