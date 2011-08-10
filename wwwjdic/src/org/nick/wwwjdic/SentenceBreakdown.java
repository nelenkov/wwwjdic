package org.nick.wwwjdic;

import static org.nick.wwwjdic.Constants.EXAMPLE_SEARRCH_TAB_IDX;
import static org.nick.wwwjdic.Constants.SELECTED_TAB_IDX;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;

public class SentenceBreakdown extends ResultListViewBase {

    public static final String EXTRA_SENTENCE = "org.nick.wwwjdic.SENTENCE";
    public static final String EXTRA_SENTENCE_TRANSLATION = "org.nick.wwwjdic.SENTENCE_TRANSLATION";

    private static final int ITEM_ID_HOME = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sentence_breakdown);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ITEM_ID_HOME, 0, R.string.home).setIcon(
                android.R.drawable.ic_menu_compass);

        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
        case ITEM_ID_HOME:
            Intent intent = new Intent(this, Wwwjdic.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(SELECTED_TAB_IDX, EXAMPLE_SEARRCH_TAB_IDX);

            startActivity(intent);
            finish();

            return true;
        default:
            // do nothing
        }

        return super.onMenuItemSelected(featureId, item);
    }

}
