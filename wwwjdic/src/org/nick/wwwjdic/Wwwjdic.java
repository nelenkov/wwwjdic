package org.nick.wwwjdic;

import static org.nick.wwwjdic.Constants.DICTIONARY_TAB_IDX;
import static org.nick.wwwjdic.Constants.EXAMPLE_SEARRCH_TAB_IDX;
import static org.nick.wwwjdic.Constants.KANJI_TAB_IDX;
import static org.nick.wwwjdic.Constants.SELECTED_TAB_IDX;

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
import android.support.v4.app.ActionBar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.ViewPager;
import android.view.MenuInflater;

public class Wwwjdic extends FragmentActivity {

    private static final int WHATS_NEW_DIALOG_ID = 1;
    private static final int DONATION_THANKS_DIALOG_ID = 2;

    private static final String DONATE_VERSION_PACKAGE = "org.nick.wwwjdic.donate";

    private static final boolean IS_HONEYCOMB = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

    private ViewPager viewPager;
    private TabsPagerAdapter tabsAdapter;

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
        tabsAdapter = new TabsPagerAdapter(this, getSupportActionBar(),
                viewPager);
        tabsAdapter.addTab(dictionaryTab, dictionary);

        ActionBar.Tab kanjiTab = getSupportActionBar().newTab();
        WwwjdicFragmentBase kanjiLookup = new KanjiLookupFragment();
        if (extras != null) {
            kanjiLookup.setArguments(extras);
        }

        kanjiTab.setIcon(R.drawable.ic_tab_kanji);
        if (IS_HONEYCOMB) {
            kanjiTab.setText(R.string.kanji_lookup);
        }
        tabsAdapter.addTab(kanjiTab, kanjiLookup);

        ActionBar.Tab examplesTab = getSupportActionBar().newTab();
        ExampleSearchFragment exampleSearch = new ExampleSearchFragment();
        if (extras != null) {
            exampleSearch.setArguments(extras);
        }

        examplesTab.setIcon(R.drawable.ic_tab_example);
        if (IS_HONEYCOMB) {
            examplesTab.setText(R.string.example_search);
        }
        tabsAdapter.addTab(examplesTab, exampleSearch);

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
