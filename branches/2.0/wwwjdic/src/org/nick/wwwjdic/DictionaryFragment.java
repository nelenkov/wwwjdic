package org.nick.wwwjdic;

import java.util.concurrent.RejectedExecutionException;

import org.nick.wwwjdic.history.HistoryDbHelper;
import org.nick.wwwjdic.model.SearchCriteria;
import org.nick.wwwjdic.utils.Analytics;
import org.nick.wwwjdic.utils.StringUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.util.SparseArrayCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class DictionaryFragment extends WwwjdicFragmentBase implements
        OnClickListener, OnCheckedChangeListener, OnItemSelectedListener {

    private static final String TAG = DictionaryFragment.class.getSimpleName();

    private static final String SELECTED_DICTIONARY_IDX = "org.nick.wwwjdic.selectedDict";

    private static final SparseArrayCompat<String> IDX_TO_DICT_CODE = new SparseArrayCompat<String>();

    private EditText inputText;
    private CheckBox exactMatchCb;
    private CheckBox commonWordsCb;
    private CheckBox romanizedJapaneseCb;
    private Spinner dictSpinner;

    private HistoryDbHelper dbHelper;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        populateIdxToDictCode();

        findViews();
        setupListeners();
        setupSpinners();

        inputText.requestFocus();

        Bundle extras = getArguments();
        if (extras == null) {
            extras = getActivity().getIntent().getExtras();
        }

        if (extras != null) {
            String searchKey = extras.getString(Wwwjdic.EXTRA_SEARCH_TEXT);
            int searchType = extras.getInt(Wwwjdic.EXTRA_SEARCH_TYPE);
            if (searchKey != null) {
                switch (searchType) {
                case SearchCriteria.CRITERIA_TYPE_DICT:
                    inputText.setText(searchKey);
                    break;
                default:
                    // do nothing
                }
                inputTextFromBundle = true;
            }
        }

        dbHelper = HistoryDbHelper.getInstance(getActivity());

        // delay focus request a bit, otherwise may fail
        // Cf. http://code.google.com/p/android/issues/detail?id=2705
        inputText.post(new Runnable() {
            public void run() {
                inputText.requestFocus();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dict_lookup, container, false);

        return v;
    }

    private void populateIdxToDictCode() {
        if (IDX_TO_DICT_CODE.size() == 0) {
            String[] dictionaryIdxs = getResources().getStringArray(
                    R.array.dictionary_idxs_array);
            String[] dictionaryCodes = getResources().getStringArray(
                    R.array.dictionary_codes_array);
            for (int i = 0; i < dictionaryIdxs.length; i++) {
                IDX_TO_DICT_CODE.put(Integer.parseInt(dictionaryIdxs[i]),
                        dictionaryCodes[i]);
            }
        }
    }

    private void selectDictionary() {
        int dictIdx = WwwjdicPreferences.getDefaultDictionaryIdx(getActivity());
        if (dictIdx != 0) {
            // if it is not the default, use it
            dictSpinner.setSelection(dictIdx);
        } else {
            // otherwise use saved value
            dictSpinner.setSelection(WwwjdicPreferences
                    .getSelectedDictionaryIdx(getActivity()));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        WwwjdicPreferences.setSelectedDictionaryIdx(getActivity(),
                dictSpinner.getSelectedItemPosition());
    }

    @Override
    public void onResume() {
        super.onResume();

        selectDictionary();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(SELECTED_DICTIONARY_IDX,
                dictSpinner.getSelectedItemPosition());
    }

    private void findViews() {
        inputText = (EditText) getView().findViewById(R.id.inputText);
        inputText
                .setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId,
                            KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                            search();

                            return true;
                        }
                        return false;
                    }
                });
        exactMatchCb = (CheckBox) getView().findViewById(R.id.exactMatchCb);
        commonWordsCb = (CheckBox) getView().findViewById(R.id.commonWordsCb);
        romanizedJapaneseCb = (CheckBox) getView().findViewById(
                R.id.romanizedCb);
        dictSpinner = (Spinner) getView().findViewById(R.id.dictionarySpinner);
    }

    private void setupListeners() {
        View translateButton = getView().findViewById(R.id.translateButton);
        translateButton.setOnClickListener(this);

        // inputText.setOnFocusChangeListener(this);

        romanizedJapaneseCb.setOnCheckedChangeListener(this);
        exactMatchCb.setOnCheckedChangeListener(this);
        commonWordsCb.setOnCheckedChangeListener(this);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.dictionaries_array,
                R.layout.spinner_text);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dictSpinner.setAdapter(adapter);
        dictSpinner.setOnItemSelectedListener(this);
    }

    public void onClick(View v) {
        if (v.getId() == R.id.translateButton) {
            search();
        }
    }

    private void search() {
        hideKeyboard();
        String input = inputText.getText().toString();
        if (TextUtils.isEmpty(input)) {
            return;
        }

        try {
            int dictIdx = dictSpinner.getSelectedItemPosition();
            String dict = getDictionaryFromSelection(dictIdx);

            SearchCriteria criteria = SearchCriteria.createForDictionary(
                    input.trim(), exactMatchCb.isChecked(),
                    romanizedJapaneseCb.isChecked(), commonWordsCb.isChecked(),
                    dict);

            Intent intent = new Intent(getActivity(),
                    DictionaryResultList.class);
            intent.putExtra(Wwwjdic.EXTRA_CRITERIA, criteria);

            if (!StringUtils.isEmpty(criteria.getQueryString())) {
                dbHelper.addSearchCriteria(criteria);
            }

            Analytics.event("dictSearch", getActivity());

            startActivity(intent);
        } catch (RejectedExecutionException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private String getDictionaryFromSelection(int dictIdx) {
        String dict = IDX_TO_DICT_CODE.get(dictIdx);
        Log.i(TAG, "dictionary idx: " + Integer.toString(dictIdx));
        Log.i(TAG, "dictionary: " + dict);
        if (dict == null) {
            // edict
            dict = "1";
        }

        return dict;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.exactMatchCb) {
            toggleRomanizedCb(isChecked);
        } else if (buttonView.getId() == R.id.commonWordsCb) {
            toggleRomanizedCb(isChecked);
        } else if (buttonView.getId() == R.id.romanizedCb) {
            toggleExactCommonCbs(isChecked);
        }
    }

    private void toggleExactCommonCbs(boolean isChecked) {
        if (isChecked) {
            exactMatchCb.setEnabled(false);
            commonWordsCb.setEnabled(false);
        } else {
            exactMatchCb.setEnabled(true);
            commonWordsCb.setEnabled(true);
        }
    }

    private void toggleRomanizedCb(boolean isChecked) {
        if (isChecked) {
            romanizedJapaneseCb.setEnabled(false);
        } else {
            if (!exactMatchCb.isChecked() && !commonWordsCb.isChecked()) {
                romanizedJapaneseCb.setEnabled(true);
            }
        }
    }

    private void hideKeyboard() {
        if (getActivity() == null) {
            return;
        }

        InputMethodManager mgr = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(inputText.getWindowToken(), 0);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos,
            long id) {
        String dict = getDictionaryFromSelection(pos);
        String dictName = (String) parent.getSelectedItem();
        getApp().setCurrentDictionary(dict);
        getApp().setCurrentDictionaryName(dictName);
        Log.d(TAG, String.format("current dictionary: %s(%s)", dictName, dict));
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

}
