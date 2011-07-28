package org.nick.wwwjdic;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class KanjiResultListView extends ResultListViewBase<KanjiEntry> {

    private static final String TAG = KanjiResultListView.class.getSimpleName();

    private static final int MENU_ITEM_DETAILS = 0;
    private static final int MENU_ITEM_COPY = 1;
    private static final int MENU_ITEM_ADD_TO_FAVORITES = 2;
    private static final int MENU_ITEM_STROKE_ORDER = 3;
    private static final int MENU_ITEM_COMPOUNDS = 4;

    private List<KanjiEntry> entries;

    public KanjiResultListView() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search_results);

        getListView().setOnCreateContextMenuListener(this);

        extractSearchCriteria();

        SearchTask<KanjiEntry> searchTask = new KanjiSearchTask(
                getWwwjdicUrl(), getHttpTimeoutSeconds(), this, criteria);
        submitSearchTask(searchTask);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        KanjiEntry entry = entries.get(position);
        showDetails(entry);
    }

    private void showDetails(KanjiEntry entry) {
        Intent intent = new Intent(this, KanjiEntryDetail.class);
        intent.putExtra(Constants.KANJI_ENTRY_KEY, entry);
        setFavoriteId(intent, entry);

        startActivity(intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        menu.add(0, MENU_ITEM_DETAILS, 0, R.string.details);
        menu.add(0, MENU_ITEM_COPY, 1, R.string.copy);
        menu.add(0, MENU_ITEM_ADD_TO_FAVORITES, 2, R.string.add_to_favorites);
        menu.add(0, MENU_ITEM_STROKE_ORDER, 3, R.string.stroke_order);
        menu.add(0, MENU_ITEM_COMPOUNDS, 4, R.string.compounds);
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

        KanjiEntry entry = entries.get(info.position);
        switch (item.getItemId()) {
        case MENU_ITEM_DETAILS:
            showDetails(entry);
            return true;
        case MENU_ITEM_COPY:
            copy(entry);
            return true;
        case MENU_ITEM_ADD_TO_FAVORITES:
            addToFavorites(entry);
            return true;
        case MENU_ITEM_STROKE_ORDER:
            Activities.showStrokeOrder(this, entry);
            return true;
        case MENU_ITEM_COMPOUNDS:
            showCompounds(entry);
            return true;
        }
        return false;
    }

    private void showCompounds(KanjiEntry entry) {
        String dictionary = getApp().getCurrentDictionary();
        Log.d(TAG, String.format(
                "Will look for compounds in dictionary: %s(%s)", getApp()
                        .getCurrentDictionaryName(), dictionary));
        SearchCriteria criteria = SearchCriteria.createForKanjiCompounds(
                entry.getKanji(),
                SearchCriteria.KANJI_COMPOUND_SEARCH_TYPE_ANY, false,
                dictionary);
        Intent intent = new Intent(this, DictionaryResultListView.class);
        intent.putExtra(Constants.CRITERIA_KEY, criteria);

        startActivity(intent);
    }

    public void setResult(final List<KanjiEntry> result) {
        guiThread.post(new Runnable() {
            public void run() {
                entries = (List<KanjiEntry>) result;
                KanjiEntryAdapter adapter = new KanjiEntryAdapter(
                        KanjiResultListView.this, entries);
                setListAdapter(adapter);
                getListView().setTextFilterEnabled(true);
                String message = getResources().getString(R.string.results_for,
                        entries.size(), criteria.getQueryString());
                setTitle(message);
                dismissProgressDialog();
            }
        });
    }

}
