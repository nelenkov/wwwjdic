package org.nick.wwwjdic;

import android.os.Bundle;

public class DictionaryResultList extends ResultListBase {

    public DictionaryResultList() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search_results);
    }

}
