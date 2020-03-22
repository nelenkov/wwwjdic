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
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.nick.wwwjdic.history.FavoritesAndHistory;
import org.nick.wwwjdic.history.FavoritesAndHistorySummaryView;
import org.nick.wwwjdic.history.HistoryDbHelper;
import org.nick.wwwjdic.history.HistoryFragmentBase;
import org.nick.wwwjdic.hkr.RecognizeKanjiActivity;
import org.nick.wwwjdic.krad.KradChart;
import org.nick.wwwjdic.model.SearchCriteria;
import org.nick.wwwjdic.utils.UIUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.PagerAdapter;
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

    private final static String[] PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    class WwwjdicTabsPagerAdapter extends PagerAdapter implements
            ViewPager.OnPageChangeListener, ActionBar.TabListener {

        private AppCompatActivity activity;
        private FragmentManager fragmentManager;
        private FragmentTransaction currentTransaction = null;

        private final ActionBar actionBar;
        private final ViewPager viewPager;

        private final List<Integer> tabLayouts = new ArrayList<Integer>();

        public WwwjdicTabsPagerAdapter(AppCompatActivity activity,
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
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            // fixes ABS #351
            // https://github.com/JakeWharton/ActionBarSherlock/issues/351
            int position = tab.getPosition();
            if (viewPager.getCurrentItem() != position) {
                viewPager.setCurrentItem(position);
                filterHistoryFragments(position);
                updateHistorySummary(position);
            }
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset,
                int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            actionBar.setSelectedNavigationItem(position);
            filterHistoryFragments(position);
            updateHistorySummary(position);

            // for ABS#240
            // https://github.com/JakeWharton/ActionBarSherlock/issues/240
            selectInSpinnerIfPresent(position, true);
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
            // Log.d(TAG, "instantiateItem at position " + position);

            LayoutInflater inflater = activity.getLayoutInflater();
            View view = inflater.inflate(tabLayouts.get(position), null);

            updateHistorySummary(position, view);

            filterHistoryFragments(position);

            ((ViewPager) container).addView(view, 0);

            return view;
        }

        @Override
        public void destroyItem(View container, int position, Object view) {
            // Log.d(TAG, "destroyItem at position " + position);

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
            if (fragment != null && !fragment.isDetached()
                    && fragment.getActivity() != null) {
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

        /**
         * Hack that takes advantage of interface parity between
         * ActionBarSherlock and the native interface to reach inside the
         * classes to manually select the appropriate tab spinner position if
         * the overflow tab spinner is showing.
         * 
         * Related issues:
         * https://github.com/JakeWharton/ActionBarSherlock/issues/240 and
         * https://android-review.googlesource.com/#/c/32492/
         * 
         * @author toulouse@crunchyroll.com
         */
        private void selectInSpinnerIfPresent(int position, boolean animate) {
            try {
                View actionBarView = null;//findViewById(R.id.abs__action_bar);
                if (actionBarView == null) {
                    int id = getResources().getIdentifier("action_bar", "id",
                            "android");
                    actionBarView = findViewById(id);
                }

                Class<?> actionBarViewClass = actionBarView.getClass();
                Field mTabScrollViewField = actionBarViewClass
                        .getDeclaredField("mTabScrollView");
                mTabScrollViewField.setAccessible(true);

                Object mTabScrollView = mTabScrollViewField.get(actionBarView);
                if (mTabScrollView == null) {
                    return;
                }

                Field mTabSpinnerField = mTabScrollView.getClass()
                        .getDeclaredField("mTabSpinner");
                mTabSpinnerField.setAccessible(true);

                Object mTabSpinner = mTabSpinnerField.get(mTabScrollView);
                if (mTabSpinner == null) {
                    return;
                }

                Method setSelectionMethod = mTabSpinner
                        .getClass()
                        .getSuperclass()
                        .getDeclaredMethod("setSelection", Integer.TYPE,
                                Boolean.TYPE);
                setSelectionMethod.invoke(mTabSpinner, position, animate);

            } catch (IllegalArgumentException e) {
                Log.w(TAG,
                        "WwwjdicTabsPagerAdapter.selectInSpinnerIfPresent()", e);
            } catch (IllegalAccessException e) {
                Log.w(TAG,
                        "WwwjdicTabsPagerAdapter.selectInSpinnerIfPresent()", e);
            } catch (NoSuchFieldException e) {
                Log.w(TAG,
                        "WwwjdicTabsPagerAdapter.selectInSpinnerIfPresent()", e);
            } catch (NoSuchMethodException e) {
                Log.w(TAG,
                        "WwwjdicTabsPagerAdapter.selectInSpinnerIfPresent()", e);
            } catch (InvocationTargetException e) {
                Log.w(TAG,
                        "WwwjdicTabsPagerAdapter.selectInSpinnerIfPresent()", e);
            }
        }

    }

    private static final int NUM_RECENT_HISTORY_ENTRIES = 5;

    private ViewPager viewPager;
    // private TabsPagerAdapter tabsAdapter;
    private WwwjdicTabsPagerAdapter tabsAdapter;

    private HistoryDbHelper dbHelper;

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

        setupTabs();

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

    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermissions() {
        requestPermissions(PERMISSIONS, REQUEST_CODE_PERMISSIONS);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permission, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permission, grantResults);

        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
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

        int position = getSupportActionBar().getSelectedNavigationIndex();
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

    private void setupTabs() {
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        Bundle extras = getIntent().getExtras();

        androidx.appcompat.app.ActionBar.Tab dictionaryTab = getSupportActionBar().newTab();
        dictionaryTab.setIcon(R.drawable.ic_tab_dict);
        if (UIUtils.isHoneycombTablet(this)) {
            dictionaryTab.setText(R.string.dictionary);
        }
        viewPager = (ViewPager) findViewById(R.id.content);
        tabsAdapter = new WwwjdicTabsPagerAdapter(this, getSupportActionBar(),
                viewPager);
        tabsAdapter.addTab(dictionaryTab, R.layout.dict_lookup_tab);

        androidx.appcompat.app.ActionBar.Tab kanjiTab = getSupportActionBar().newTab();
        kanjiTab.setIcon(R.drawable.ic_tab_kanji);
        if (UIUtils.isHoneycombTablet(this)) {
            kanjiTab.setText(R.string.kanji_lookup);
        }
        tabsAdapter.addTab(kanjiTab, R.layout.kanji_lookup_tab);

        androidx.appcompat.app.ActionBar.Tab examplesTab = getSupportActionBar().newTab();
        examplesTab.setIcon(R.drawable.ic_tab_example);
        if (UIUtils.isHoneycombTablet(this)) {
            examplesTab.setText(R.string.example_search);
        }
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

    private static class HistorySummaryParams {
        long numAllFavorites;
        long numAllHistory;
        List<String> recentHistory;
        List<String> recentFavorites;
    }

    private void updateDictSummary(View view) {
        final FavoritesAndHistorySummaryView dictHistorySummary = (FavoritesAndHistorySummaryView) view
                .findViewById(R.id.dict_history_summary);
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
        final FavoritesAndHistorySummaryView examplesHistorySummary = (FavoritesAndHistorySummaryView) view
                .findViewById(R.id.examples_history_summary);
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
