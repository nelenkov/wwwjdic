package org.nick.wwwjdic;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class KanjiResultListView extends ResultListViewBase<KanjiEntry> {

    private List<KanjiEntry> entries;

    public KanjiResultListView() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search_results);

        extractSearchCriteria();

        SearchTask<KanjiEntry> searchTask = new KanjiSearchTask(
                getWwwjdicUrl(), getHttpTimeoutSeconds(), this, criteria);
        submitSearchTask(searchTask);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(this, KanjiEntryDetail.class);
        KanjiEntry entry = entries.get(position);
        intent.putExtra(Constants.KANJI_ENTRY_KEY, entry);
        setFavoriteId(intent, entry);

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
                String message = getResources().getString(R.string.results_for);
                setTitle(String.format(message, entries.size(), criteria
                        .getQueryString()));
                dismissProgressDialog();
            }
        });
    }

}
