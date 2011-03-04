package org.nick.wwwjdic;

import java.util.List;

import org.nick.wwwjdic.utils.Analytics;
import org.nick.wwwjdic.utils.DictUtils;
import org.nick.wwwjdic.utils.StringUtils;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class DictionaryResultListView extends
        ResultListViewBase<DictionaryEntry> {

    private static final int NUM_EXAMPLE_RESULTS = 20;

    private static final String TAG = DictionaryResultListView.class
            .getSimpleName();

    private static final int MENU_ITEM_DETAILS = 0;
    private static final int MENU_ITEM_COPY = 1;
    private static final int MENU_ITEM_LOOKUP_KANJI = 2;
    private static final int MENU_ITEM_ADD_TO_FAVORITES = 3;
    private static final int MENU_ITEM_EXAMPLES = 4;

    private List<DictionaryEntry> entries;

    public DictionaryResultListView() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search_results);

        getListView().setOnCreateContextMenuListener(this);

        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            String dictionary = WwwjdicPreferences.getDefaultDictionary(this);
            criteria = SearchCriteria.createForDictionary(query, false, false,
                    false, dictionary);
        } else {
            extractSearchCriteria();
        }
        SearchTask<DictionaryEntry> searchTask = new DictionarySearchTask(
                getWwwjdicUrl(), getHttpTimeoutSeconds(), this, criteria);
        submitSearchTask(searchTask);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        DictionaryEntry entry = entries.get(position);

        showDetails(entry);
    }

    private void showDetails(DictionaryEntry entry) {
        Intent intent = new Intent(this, DictionaryEntryDetail.class);
        intent.putExtra(Constants.ENTRY_KEY, entry);
        setFavoriteId(intent, entry);

        startActivity(intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        menu.add(0, MENU_ITEM_DETAILS, 0, R.string.details);
        menu.add(0, MENU_ITEM_COPY, 1, R.string.copy);
        menu.add(0, MENU_ITEM_LOOKUP_KANJI, 2, R.string.lookup_kanji);
        menu.add(0, MENU_ITEM_ADD_TO_FAVORITES, 3, R.string.add_to_favorites);
        menu.add(0, MENU_ITEM_EXAMPLES, 4, R.string.examples);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        DictionaryEntry entry = entries.get(info.position);
        switch (item.getItemId()) {
        case MENU_ITEM_DETAILS:
            showDetails(entry);
            return true;
        case MENU_ITEM_COPY:
            copy(entry);
            return true;
        case MENU_ITEM_LOOKUP_KANJI:
            Activities.lookupKanji(this, db, entry.getHeadword());
            return true;
        case MENU_ITEM_ADD_TO_FAVORITES:
            addToFavorites(entry);
            return true;
        case MENU_ITEM_EXAMPLES:
            searchExamples(entry);
            return true;
        }
        return false;
    }

    private void searchExamples(DictionaryEntry entry) {
        SearchCriteria criteria = SearchCriteria.createForExampleSearch(
                DictUtils.extractSearchKey(entry), false, NUM_EXAMPLE_RESULTS);

        Intent intent = new Intent(this, ExamplesResultListView.class);
        intent.putExtra(Constants.CRITERIA_KEY, criteria);

        if (!StringUtils.isEmpty(criteria.getQueryString())) {
            db.addSearchCriteria(criteria);
        }

        Analytics.event("exampleSearch", this);

        startActivity(intent);
    }

    public void setResult(final List<DictionaryEntry> result) {
        guiThread.post(new Runnable() {
            public void run() {
                entries = (List<DictionaryEntry>) result;
                DictionaryEntryAdapter adapter = new DictionaryEntryAdapter(
                        DictionaryResultListView.this, entries);
                setListAdapter(adapter);
                getListView().setTextFilterEnabled(true);
                String message = getResources().getString(R.string.results_for);
                setTitle(String.format(message, entries.size(),
                        criteria.getQueryString()));
                dismissProgressDialog();
            }
        });
    }

}
