package org.nick.wwwjdic;

import android.os.Bundle;

public class KanjiResultListView extends ResultListViewBase {

    public KanjiResultListView() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.kanji_search_results);
    }

}
