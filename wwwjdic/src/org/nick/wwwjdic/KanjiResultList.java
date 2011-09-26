package org.nick.wwwjdic;

import android.os.Bundle;

public class KanjiResultList extends ResultListBase {

    public KanjiResultList() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.kanji_search_results);
    }

}
