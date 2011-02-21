package org.nick.wwwjdic;

import java.util.List;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class DictionaryResultListView extends
        ResultListViewBase<DictionaryEntry> {

    private List<DictionaryEntry> entries;

    public DictionaryResultListView() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search_results);

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
        Intent intent = new Intent(this, DictionaryEntryDetail.class);
        DictionaryEntry entry = entries.get(position);
        intent.putExtra(Constants.ENTRY_KEY, entry);
        setFavoriteId(intent, entry);

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
