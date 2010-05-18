package org.nick.wwwjdic.hkr;

import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.Wwwjdic;

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
        candidates = extras.getStringArray("hkrCandidates");
        setListAdapter(new ArrayAdapter<String>(this,
                org.nick.wwwjdic.R.layout.text_list_item, candidates));

        getListView().setTextFilterEnabled(true);

        setTitle(String.format("%d result(s)", candidates.length));

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String searchKey = candidates[position];
        Bundle extras = new Bundle();
        extras.putString(Constants.SEARCH_TEXT_KEY, searchKey);
        extras.putBoolean(Constants.SEARCH_TEXT_KANJI_KEY, true);

        Intent intent = new Intent(this, Wwwjdic.class);
        intent.putExtras(extras);

        startActivity(intent);

        finish();
    }

}
