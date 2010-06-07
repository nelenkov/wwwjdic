package org.nick.wwwjdic;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SearchHistory extends ListActivity {

    static class SearchHistoryAdapter extends CursorAdapter {

        private LayoutInflater inflater;

        public SearchHistoryAdapter(Context context, Cursor c) {
            super(context, c);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView isKanjiText = (TextView) view.findViewById(R.id.is_kanji);
            TextView searchKeyText = (TextView) view
                    .findViewById(R.id.search_key);
            TextView detailsText = (TextView) view
                    .findViewById(R.id.criteria_details);

            SearchCriteria criteria = HistoryDbHelper.createCriteria(cursor);
            isKanjiText.setText(criteria.isKanjiLookup() ? "Š¿" : "‚ ");
            searchKeyText.setText(criteria.getQueryString());

            String detailStr = buildDetailString(criteria);
            if (detailStr != null && !"".equals(detailStr)) {
                detailsText.setText(detailStr);
            }

        }

        private String buildDetailString(SearchCriteria criteria) {
            StringBuffer buff = new StringBuffer();

            if (criteria.isKanjiLookup()) {
                buff.append(criteria.getKanjiSearchType());
            } else {
                if (criteria.isCommonWordsOnly()) {
                    buff.append("P ");
                }

                if (criteria.isExactMatch()) {
                    buff.append("E ");
                }

                if (criteria.isRomanizedJapanese()) {
                    buff.append("R ");
                }
            }

            String result = buff.toString();
            if (result != null && !"".equals(result)) {
                return "(" + result.trim() + ")";
            }

            return result;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return inflater
                    .inflate(R.layout.search_history_item, parent, false);
        }
    }

    private HistoryDbHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search_history);

        db = new HistoryDbHelper(this);

        try {
            Cursor cursor = db.getAllHistory();
            SearchHistoryAdapter adapter = new SearchHistoryAdapter(this,
                    cursor);
            setListAdapter(adapter);
        } finally {
            db.close();
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        CursorAdapter adapter = (CursorAdapter) getListAdapter();
        Cursor c = adapter.getCursor();

        SearchCriteria criteria = HistoryDbHelper.createCriteria(c);

        Intent intent = null;
        if (criteria.isKanjiLookup()) {
            intent = new Intent(this, KanjiResultListView.class);
        } else {
            intent = new Intent(this, DictionaryResultListView.class);
        }
        intent.putExtra(Constants.CRITERIA_KEY, criteria);

        startActivity(intent);
    }
}
