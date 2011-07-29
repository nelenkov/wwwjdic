package org.nick.wwwjdic;

import android.os.Bundle;

public class DictionaryResultListView extends ResultListViewBase {

    public DictionaryResultListView() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search_results);
    }

}
