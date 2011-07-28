package org.nick.wwwjdic;

import static org.nick.wwwjdic.Constants.DICTIONARY_TAB_IDX;
import static org.nick.wwwjdic.Constants.EXAMPLE_SEARRCH_TAB_IDX;
import static org.nick.wwwjdic.Constants.KANJI_TAB_IDX;
import static org.nick.wwwjdic.Constants.SELECTED_TAB_IDX;

import org.nick.wwwjdic.utils.Analytics;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;

public class Wwwjdic extends TabActivity {

    private static final int WHATS_NEW_DIALOG_ID = 1;
    private static final int DONATION_THANKS_DIALOG_ID = 2;

    private static final String DICTIONARY_TAB = "dictionaryTab";
    private static final String KANJI_TAB = "kanjiTab";
    private static final String EXAMPLE_SEARCH_TAB = "exampleSearchTab";

    private static final String DONATE_VERSION_PACKAGE = "org.nick.wwwjdic.donate";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.main);

        setupTabs();

        if (!isDonateVersion()
                || WwwjdicPreferences.isDonationThanksShown(this)) {
            showWhatsNew();
        }

        showDonationThanks();
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
        TabHost tabHost = getTabHost();
        Bundle extras = getIntent().getExtras();

        Intent dictionaryIntent = new Intent(this, Dictionary.class);
        if (extras != null) {
            dictionaryIntent.putExtras(extras);
        }
        tabHost.addTab(tabHost
                .newTabSpec(DICTIONARY_TAB)
                .setIndicator(getResources().getText(R.string.dictionary),
                        getResources().getDrawable(R.drawable.ic_tab_dict))
                .setContent(dictionaryIntent));

        Intent kanjiLookup = new Intent(this, KanjiLookup.class);
        if (extras != null) {
            kanjiLookup.putExtras(extras);
        }
        tabHost.addTab(tabHost
                .newTabSpec(KANJI_TAB)
                .setIndicator(getResources().getText(R.string.kanji_lookup),
                        getResources().getDrawable(R.drawable.ic_tab_kanji))
                .setContent(kanjiLookup));

        Intent exampleSearch = new Intent(this, ExampleSearch.class);
        if (extras != null) {
            exampleSearch.putExtras(extras);
        }
        tabHost.addTab(tabHost
                .newTabSpec(EXAMPLE_SEARCH_TAB)
                .setIndicator(getResources().getText(R.string.example_search),
                        getResources().getDrawable(R.drawable.ic_tab_example))
                .setContent(exampleSearch));

        tabHost.setCurrentTab(DICTIONARY_TAB_IDX);
        if (extras != null) {
            int selectedTab = extras.getInt(SELECTED_TAB_IDX, -1);
            if (selectedTab != -1) {
                tabHost.setCurrentTab(selectedTab);
            }

            String searchKey = extras.getString(Constants.SEARCH_TEXT_KEY);
            int searchType = extras.getInt(Constants.SEARCH_TYPE);
            if (searchKey != null) {
                switch (searchType) {
                case SearchCriteria.CRITERIA_TYPE_DICT:
                    tabHost.setCurrentTab(DICTIONARY_TAB_IDX);
                    break;
                case SearchCriteria.CRITERIA_TYPE_KANJI:
                    tabHost.setCurrentTab(KANJI_TAB_IDX);
                    break;
                case SearchCriteria.CRITERIA_TYPE_EXAMPLES:
                    tabHost.setCurrentTab(EXAMPLE_SEARRCH_TAB_IDX);
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
