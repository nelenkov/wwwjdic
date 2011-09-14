package org.nick.wwwjdic.hkr;

import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.KanjiResultListView;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.SearchCriteria;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class HkrCandidates extends ListActivity {

    private static final String TAG = HkrCandidates.class.getSimpleName();

    public static final String EXTRA_HKR_CANDIDATES = "org.nick.wwwjdic.hkrCandidates";

    private static final int MENU_ITEM_DETAILS = 0;
    private static final int MENU_ITEM_COPY = 1;
    private static final int MENU_ITEM_APPEND = 2;

    private String[] candidates;
    protected ClipboardManager clipboard;

    public HkrCandidates() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        candidates = extras.getStringArray(EXTRA_HKR_CANDIDATES);
        setListAdapter(new ArrayAdapter<String>(this,
                org.nick.wwwjdic.R.layout.text_list_item, candidates));

        getListView().setOnCreateContextMenuListener(this);
        getListView().setTextFilterEnabled(true);

        String message = getResources().getString(R.string.candidates);
        setTitle(String.format(message, candidates.length));

        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String searchKey = candidates[position];
        showDetails(searchKey);
    }

    private void showDetails(String searchKey) {
        SearchCriteria criteria = SearchCriteria
                .createForKanjiOrReading(searchKey);
        Bundle extras = new Bundle();
        extras.putSerializable(Constants.CRITERIA_KEY, criteria);

        Intent intent = new Intent(this, KanjiResultListView.class);
        intent.putExtras(extras);

        startActivity(intent);
    }

    private void copy(String kanji) {
        clipboard.setText(kanji);
        String messageTemplate = getResources().getString(
                R.string.copied_to_clipboard);
        Toast t = Toast.makeText(this, String.format(messageTemplate, kanji),
                Toast.LENGTH_SHORT);
        t.show();
    }

    private void append(String kanji) {
        CharSequence text = clipboard.getText();
        String clipboardText = text == null ? "" : text.toString();
        clipboardText += kanji;
        clipboard.setText(clipboardText);
        String messageTemplate = getResources().getString(
                R.string.appended_to_clipboard);
        Toast t = Toast.makeText(this, String.format(messageTemplate, kanji),
                Toast.LENGTH_SHORT);
        t.show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        menu.add(0, MENU_ITEM_DETAILS, 0, R.string.kanji_details);
        menu.add(0, MENU_ITEM_COPY, 1, R.string.copy);
        menu.add(0, MENU_ITEM_APPEND, 2, R.string.append);

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        String kanji = candidates[info.position];
        switch (item.getItemId()) {
        case MENU_ITEM_DETAILS:
            showDetails(kanji);
            return true;
        case MENU_ITEM_COPY:
            copy(kanji);
            return true;
        case MENU_ITEM_APPEND:
            append(kanji);
            return true;
        }
        return false;
    }

}
