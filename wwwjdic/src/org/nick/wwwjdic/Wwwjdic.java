package org.nick.wwwjdic;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Window;
import android.widget.TabHost;

public class Wwwjdic extends TabActivity {

    private static final int WHATS_NEW_DIALOG_ID = 1;

    private static final int DICTIONARY_TAB_IDX = 0;
    private static final String DICTIONARY_TAB = "dictionaryTab";
    private static final int KANJI_TAB_IDX = 1;
    private static final String KANJI_TAB = "kanjiTab";
    private static final int EXAMPLE_SEARRCH_TAB_IDX = 2;
    private static final String EXAMPLE_SEARCH_TAB = "exampleSearchTab";

    private static final String PREF_WHATS_NEW_SHOWN = "pref_whats_new_shown";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.main);

        setupTabs();

        showWhatsNew();
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
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        String key = PREF_WHATS_NEW_SHOWN + "_" + getVersionName();
        boolean whatsNewShown = prefs.getBoolean(key, false);
        if (!whatsNewShown) {
            prefs.edit().putBoolean(key, true).commit();
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
        tabHost.addTab(tabHost.newTabSpec(DICTIONARY_TAB).setIndicator(
                getResources().getText(R.string.dictionary),
                getResources().getDrawable(R.drawable.ic_tab_dict)).setContent(
                dictionaryIntent));

        Intent kanjiLookup = new Intent(this, KanjiLookup.class);
        if (extras != null) {
            kanjiLookup.putExtras(extras);
        }
        tabHost.addTab(tabHost.newTabSpec(KANJI_TAB).setIndicator(
                getResources().getText(R.string.kanji_lookup),
                getResources().getDrawable(R.drawable.ic_tab_kanji))
                .setContent(kanjiLookup));

        Intent exampleSearch = new Intent(this, ExampleSearch.class);
        if (extras != null) {
            exampleSearch.putExtras(extras);
        }
        tabHost.addTab(tabHost.newTabSpec(EXAMPLE_SEARCH_TAB).setIndicator(
                getResources().getText(R.string.example_search),
                getResources().getDrawable(R.drawable.ic_tab_example))
                .setContent(exampleSearch));

        tabHost.setCurrentTab(DICTIONARY_TAB_IDX);
        if (extras != null) {
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
        default:
            dialog = null;
        }

        return dialog;
    }

    private Dialog createWhatsNewDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.whats_new);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                    }
                });
        String titleTemplate = getResources().getString(
                R.string.whats_new_title);
        String title = String.format(titleTemplate, getVersionName());
        builder.setTitle(title);
        builder.setIcon(android.R.drawable.ic_dialog_info);

        return builder.create();
    }

    private String getVersionName() {
        return WwwjdicApplication.getVersion();
    }

}
