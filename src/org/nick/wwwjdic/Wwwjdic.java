package org.nick.wwwjdic;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

public class Wwwjdic extends Activity implements OnClickListener,
        OnFocusChangeListener {

    private EditText inputText;
    private CheckBox exactMatchCb;
    private CheckBox kanjiLookupCb;
    private Spinner dictSpinner;

    private static final Map<Integer, String> IDX_TO_DICT = new HashMap<Integer, String>();

    static {
        IDX_TO_DICT.put(0, "1");
        IDX_TO_DICT.put(1, "2");
        IDX_TO_DICT.put(2, "3");
        IDX_TO_DICT.put(3, "4");
        IDX_TO_DICT.put(4, "5");
        IDX_TO_DICT.put(5, "6");
        IDX_TO_DICT.put(6, "7");
        IDX_TO_DICT.put(7, "8");
        IDX_TO_DICT.put(8, "A");
        IDX_TO_DICT.put(9, "B");
        IDX_TO_DICT.put(10, "C");
        IDX_TO_DICT.put(11, "D");
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        findViews();
        View translateButton = findViewById(R.id.translateButton);
        translateButton.setOnClickListener(this);

        inputText.setOnFocusChangeListener(this);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.dictinaries_array, R.layout.spinner_text);
        adapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dictSpinner.setAdapter(adapter);
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.translateButton:
            hideKeyboard();

            String input = inputText.getText().toString();

            try {
                int dictIdx = dictSpinner.getSelectedItemPosition();
                String dict = IDX_TO_DICT.get(dictIdx);
                Log.i("WWWJDIC", Integer.toString(dictIdx));
                Log.i("WWWJDIC", dict);
                if (dict == null) {
                    // edict
                    dict = "1";
                }

                SearchCriteria criteria = new SearchCriteria(input,
                        exactMatchCb.isChecked(), kanjiLookupCb.isChecked(),
                        dict);

                Intent intent = new Intent("org.nick.hello.VIEW_LIST");
                intent.putExtra("org.nick.hello.searchCriteria", criteria);

                startActivity(intent);
            } catch (RejectedExecutionException e) {
                Log.e("WWWJDIC", "RejectedExecutionException", e);
            }
            break;
        default:
            // do nothing
        }
    }

    public void onFocusChange(View v, boolean hasFocus) {
        switch (v.getId()) {
        case R.id.inputText:
            if (hasFocus) {
                showKeyboard();
            } else {
                hideKeyboard();
            }
            break;
        default:
            // do nothing
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 1, 0, "About").setIcon(
                android.R.drawable.ic_menu_info_details);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
        case 1:
            showDialog(0);
            return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;

        switch (id) {
        case 0:
            dialog = createAboutDialog();
            break;
        default:
            dialog = null;
        }

        return dialog;
    }

    private Dialog createAboutDialog() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.about_dialog,
                (ViewGroup) findViewById(R.id.layout_root));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        AlertDialog alertDialog = builder.create();

        return alertDialog;
        //
        // Dialog dialog = new Dialog(this);
        //
        // dialog.setContentView(R.layout.about_dialog);
        // dialog.setTitle("About");
        //
        // return dialog;
    }

    private void hideKeyboard() {
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(inputText.getWindowToken(), 0);
    }

    private void showKeyboard() {
        EditText editText = (EditText) findViewById(R.id.inputText);
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    private void findViews() {
        inputText = (EditText) findViewById(R.id.inputText);
        exactMatchCb = (CheckBox) findViewById(R.id.exactMatchCb);
        kanjiLookupCb = (CheckBox) findViewById(R.id.kanjiLookupCb);
        dictSpinner = (Spinner) findViewById(R.id.dictionarySpinner);
    }

}
