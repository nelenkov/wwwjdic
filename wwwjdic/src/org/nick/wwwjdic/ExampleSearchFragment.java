package org.nick.wwwjdic;

import org.nick.wwwjdic.history.HistoryDbHelper;
import org.nick.wwwjdic.model.SearchCriteria;
import org.nick.wwwjdic.utils.StringUtils;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

public class ExampleSearchFragment extends WwwjdicFragmentBase implements
        OnClickListener, OnItemSelectedListener {

    private TextInputLayout inputLayout;
    private EditText exampleSearchInputText;
    private EditText maxNumExamplesText;
    private CheckBox exampleExactMatchCb;
    private Spinner sentenceModeSpinner;
    private Button exampleSearchButton;

    private HistoryDbHelper dbHelper;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        findViews();
        setupListeners();
        setupSpinners();

        exampleSearchInputText.requestFocus();

        Bundle extras = getArguments();
        if (extras == null) {
            extras = getActivity().getIntent().getExtras();
        }

        if (extras != null) {
            String searchKey = extras.getString(Wwwjdic.EXTRA_SEARCH_TEXT);
            int searchType = extras.getInt(Wwwjdic.EXTRA_SEARCH_TYPE);
            if (searchKey != null) {
                switch (searchType) {
                case SearchCriteria.CRITERIA_TYPE_EXAMPLES:
                    exampleSearchInputText.setText(searchKey);
                    break;
                default:
                    // do nothing
                }
                inputTextFromBundle = true;
            }
        }

        dbHelper = HistoryDbHelper.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.example_search, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();

        WwwjdicPreferences.setSentenceModeIdx(getActivity(),
                sentenceModeSpinner.getSelectedItemPosition());
    }

    @Override
    public void onResume() {
        super.onResume();

        sentenceModeSpinner.setSelection(WwwjdicPreferences
                .getSentenceModeIdx(getActivity()));
    }

    private void setupListeners() {
        View exampleSearchButton = getView().findViewById(
                R.id.exampleSearchButton);
        exampleSearchButton.setOnClickListener(this);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> sentenceModeAdapter = ArrayAdapter
                .createFromResource(getActivity(), R.array.sentence_modes,
                        R.layout.spinner_text);
        sentenceModeAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sentenceModeSpinner.setAdapter(sentenceModeAdapter);
        sentenceModeSpinner.setOnItemSelectedListener(this);
    }

    public void onClick(View v) {
        if (v.getId() == R.id.exampleSearchButton) {
            search();
        }
    }

    private void search() {
        if (getActivity() == null) {
            return;
        }

        String queryString = exampleSearchInputText.getText().toString();
        if (TextUtils.isEmpty(queryString)) {
            return;
        }

        if (sentenceModeSpinner.getSelectedItemPosition() == 0) {
            int numMaxResults = 20;
            try {
                numMaxResults = Integer.parseInt(maxNumExamplesText
                        .getText().toString());
            } catch (NumberFormatException e) {
            }
            SearchCriteria criteria = SearchCriteria
                    .createForExampleSearch(queryString.trim(),
                            exampleExactMatchCb.isChecked(), numMaxResults);

            Intent intent = new Intent(getActivity(),
                    ExamplesResultList.class);
            intent.putExtra(Wwwjdic.EXTRA_CRITERIA, criteria);

            if (!StringUtils.isEmpty(criteria.getQueryString())) {
                dbHelper.addSearchCriteria(criteria);
            }

            startActivity(intent);
        } else {
            Intent intent = new Intent(getActivity(),
                    SentenceBreakdown.class);
            intent.putExtra(SentenceBreakdown.EXTRA_SENTENCE, queryString);

            startActivity(intent);
        }
    }

    private void findViews() {
        inputLayout = getView().findViewById(R.id.inputTextLayout);
        inputLayout.setEndIconMode(TextInputLayout.END_ICON_CLEAR_TEXT);
        exampleSearchInputText = getView().findViewById(R.id.exampleInputText);
        exampleSearchInputText
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
        maxNumExamplesText = getView().findViewById(R.id.maxExamplesInput);
        exampleExactMatchCb = getView().findViewById(R.id.exampleExactMatchCb);
        sentenceModeSpinner = getView().findViewById(R.id.modeSpinner);
        exampleSearchButton = getView().findViewById(R.id.exampleSearchButton);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position,
            long id) {
        if (inputTextFromBundle) {
            inputTextFromBundle = false;

            return;
        }

        if (parent.getId() == R.id.modeSpinner) {
            boolean isExampleSearch = position == 0;
            boolean clear = getExtrasSearchKey() == null;
            toggleExampleOptions(isExampleSearch, clear);
        }
    }

    private String getExtrasSearchKey() {
        Bundle extras = getActivity().getIntent().getExtras();
        if (extras == null) {
            return null;
        }

        return extras.getString(Wwwjdic.EXTRA_SEARCH_TEXT);
    }

    private void toggleExampleOptions(boolean isEnabled, boolean clear) {
        maxNumExamplesText.setEnabled(isEnabled);
        exampleExactMatchCb.setEnabled(isEnabled);
        maxNumExamplesText.setFocusableInTouchMode(isEnabled);

        if (clear) {
            exampleSearchInputText.setText("");
        }
        exampleSearchInputText.requestFocus();

        if (!isEnabled) {
            if (clear) {
                inputLayout.setHint(getResources().getString(R.string.enter_japanese_text));
            }
            exampleSearchButton.setText(R.string.translate);
        } else {
            if (clear) {
                inputLayout.setHint(getResources().getString(R.string.enter_eng_or_jap));
            }
            exampleSearchButton.setText(R.string.search);
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }

}
