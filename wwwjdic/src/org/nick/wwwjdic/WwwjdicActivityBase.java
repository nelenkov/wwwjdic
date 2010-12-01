package org.nick.wwwjdic;

import org.nick.wwwjdic.history.FavoritesAndHistory;
import org.nick.wwwjdic.hkr.RecognizeKanjiActivity;
import org.nick.wwwjdic.ocr.OcrActivity;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

public class WwwjdicActivityBase extends Activity {

    protected static final int NUM_RECENT_HISTORY_ENTRIES = 5;

    private static final int ITEM_ID_ABOUT = 1;
    private static final int ITEM_ID_OCR = 2;
    private static final int ITEM_ID_SETTINGS = 3;
    private static final int ITEM_ID_DRAW = 4;
    private static final int ITEM_ID_HISTORY = 5;

    protected boolean inputTextFromBundle;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ITEM_ID_OCR, 0, R.string.ocr).setIcon(
                android.R.drawable.ic_menu_camera);
        menu.add(0, ITEM_ID_DRAW, 1, R.string.write_kanji).setIcon(
                android.R.drawable.ic_menu_edit);
        menu.add(0, ITEM_ID_HISTORY, 2, R.string.favorites_hist).setIcon(
                android.R.drawable.ic_menu_recent_history);
        menu.add(0, ITEM_ID_SETTINGS, 3, R.string.settings).setIcon(
                android.R.drawable.ic_menu_preferences);
        menu.add(0, ITEM_ID_ABOUT, 4, R.string.about).setIcon(
                android.R.drawable.ic_menu_info_details);

        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
        case ITEM_ID_ABOUT:
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        case ITEM_ID_OCR:
            intent = new Intent(this, OcrActivity.class);

            startActivity(intent);
            return true;
        case ITEM_ID_SETTINGS:
            intent = new Intent(this, WwwjdicPreferences.class);

            startActivity(intent);
            return true;
        case ITEM_ID_DRAW:
            intent = new Intent(this, RecognizeKanjiActivity.class);

            startActivity(intent);
            return true;
        case ITEM_ID_HISTORY:
            intent = new Intent(this, FavoritesAndHistory.class);

            startActivity(intent);
            return true;
        default:
            // do nothing
        }

        return super.onMenuItemSelected(featureId, item);
    }

}
