package org.nick.wwwjdic.hkr;

import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.KanjiResultListView;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.SearchCriteria;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class HkrCandidates extends ListActivity {

    private String[] candidates;

    public HkrCandidates() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        candidates = extras.getStringArray(Constants.HKR_CANDIDATES_KEY);
        setListAdapter(new ArrayAdapter<String>(this,
                org.nick.wwwjdic.R.layout.text_list_item, candidates));

        getListView().setTextFilterEnabled(true);

        String message = getResources().getString(R.string.results);
        setTitle(String.format(message, candidates.length));

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String searchKey = candidates[position];
        SearchCriteria criteria = SearchCriteria
                .createForKanjiOrReading(searchKey);
        Bundle extras = new Bundle();
        extras.putSerializable(Constants.CRITERIA_KEY, criteria);

        Intent intent = new Intent(this, KanjiResultListView.class);
        intent.putExtras(extras);

        startActivity(intent);
    }

}
