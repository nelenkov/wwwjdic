package org.nick.wwwjdic;

import static org.nick.wwwjdic.Constants.DICTIONARY_TAB_IDX;
import static org.nick.wwwjdic.Constants.EXAMPLE_SEARRCH_TAB_IDX;
import static org.nick.wwwjdic.Constants.KANJI_TAB_IDX;
import static org.nick.wwwjdic.Constants.SELECTED_TAB_IDX;

import java.util.ArrayList;
import java.util.List;

import org.nick.wwwjdic.history.FavoritesAndHistory;
import org.nick.wwwjdic.hkr.RecognizeKanjiActivity;
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

public class Wwwjdic extends FragmentActivity {

    private static final String TAG = Wwwjdic.class.getSimpleName();

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
        private final List<Fragment> tabs = new ArrayList<Fragment>();

        private final List<Integer> tabLayouts = new ArrayList<Integer>();
        private final List<View> tabViews = new ArrayList<View>();

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
            viewPager.setCurrentItem(tab.getPosition());
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

            ((ViewPager) container).addView(view, 0);

            return view;
        }

        @Override
        public void destroyItem(View container, int position, Object view) {
            Log.d(TAG, "destroyItem at position " + position);

            if (currentTransaction == null) {
                currentTransaction = fragmentManager.beginTransaction();
            }

            // XXX
            switch (position) {
            case 0:
                Fragment fragment = fragmentManager
                        .findFragmentById(R.id.dictionary_fragment);
                if (fragment != null) {
                    currentTransaction.remove(fragment);
                }
                removeFavoritesHistoryFragments();
                break;
            case 1:
                fragment = fragmentManager
                        .findFragmentById(R.id.kanji_lookup_fragment);
                if (fragment != null) {
                    currentTransaction.remove(fragment);
                }
                removeFavoritesHistoryFragments();
                break;
            case 2:
                fragment = fragmentManager
                        .findFragmentById(R.id.example_search_fragment);
                if (fragment != null) {
                    currentTransaction.remove(fragment);
                }
                removeFavoritesHistoryFragments();
                break;
            }

            ((ViewPager) container).removeView((View) view);
        }

        private void removeFavoritesHistoryFragments() {
            Fragment fragment = fragmentManager
                    .findFragmentById(R.id.history_fragment);
            if (fragment != null) {
                currentTransaction.remove(fragment);
            }
            fragment = fragmentManager
                    .findFragmentById(R.id.favorites_fragment);
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

    private ViewPager viewPager;
    //    private TabsPagerAdapter tabsAdapter;
    private WwwjdicTabsPagerAdapter tabsAdapter;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        setupTabs();

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
        getSupportActionBar().setDisplayShowHomeEnabled(false);
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
        //        tabsAdapter = new TabsPagerAdapter(this, getSupportActionBar(),
        //                viewPager);
        tabsAdapter = new WwwjdicTabsPagerAdapter(this, getSupportActionBar(),
                viewPager);
        //        tabsAdapter.addTab(dictionaryTab, dictionary);
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
        //        tabsAdapter.addTab(kanjiTab, kanjiLookup);
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
        //        tabsAdapter.addTab(examplesTab, exampleSearch);
        tabsAdapter.addTab(examplesTab, R.layout.example_search_tab);

        getSupportActionBar().setSelectedNavigationItem(DICTIONARY_TAB_IDX);
        if (extras != null) {
            int selectedTab = extras.getInt(SELECTED_TAB_IDX, -1);
            if (selectedTab != -1) {
                getSupportActionBar().setSelectedNavigationItem(selectedTab);
            }

            String searchKey = extras.getString(Constants.SEARCH_TEXT_KEY);
            int searchType = extras.getInt(Constants.SEARCH_TYPE);
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

}
