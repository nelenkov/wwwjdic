package org.nick.wwwjdic;

import static org.nick.wwwjdic.Constants.DICTIONARY_TAB_IDX;
import static org.nick.wwwjdic.Constants.SELECTED_TAB_IDX;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import org.nick.wwwjdic.utils.DictUtils;
import org.nick.wwwjdic.utils.Pair;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DictionaryEntryDetail extends DetailActivity implements
        OnClickListener {

    private static final String TAG = DictionaryEntryDetail.class
            .getSimpleName();

    private static final int ITEM_ID_LOOKUP_KANJI = 1;

    private static final int DEFAULT_MAX_NUM_EXAMPLES = 20;

    private LinearLayout translationsLayout;
    private TextView entryView;
    private CheckBox starCb;
    private Button exampleSearchButton;

    private DictionaryEntry entry;
    private String exampleSearchKey;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entry_details);

        checkTtsAvailability();

        entry = (DictionaryEntry) getIntent().getSerializableExtra(
                Constants.ENTRY_KEY);
        wwwjdicEntry = entry;
        isFavorite = getIntent().getBooleanExtra(Constants.IS_FAVORITE, false);

        String message = getResources().getString(R.string.details_for);
        setTitle(String.format(message, entry.getWord()));

        LinearLayout wordReadingLayout = (LinearLayout) findViewById(R.id.word_reading_layout);

        entryView = (TextView) findViewById(R.id.wordText);
        entryView.setText(entry.getWord());
        entryView.setOnLongClickListener(this);

        if (entry.getReading() != null) {
            TextView readingView = new TextView(this, null,
                    R.style.dict_detail_reading);
            readingView.setText(entry.getReading());
            wordReadingLayout.addView(readingView);
        }

        translationsLayout = (LinearLayout) findViewById(R.id.translations_layout);

        for (String meaning : entry.getMeanings()) {
            final Pair<LinearLayout, TextView> translationViews = createMeaningTextView(
                    DictionaryEntryDetail.this, meaning);
            Matcher m = CROSS_REF_PATTERN.matcher(meaning);
            if (m.matches()) {
                Intent intent = createCrossRefIntent(m.group(1));
                int start = m.start(1);
                int end = m.end(1);
                makeClickable(translationViews.getSecond(), start, end, intent);
            }
            translationsLayout.addView(translationViews.getFirst());
        }

        starCb = (CheckBox) findViewById(R.id.star_word);
        starCb.setOnCheckedChangeListener(null);
        starCb.setChecked(isFavorite);
        starCb.setOnCheckedChangeListener(this);

        exampleSearchButton = (Button) findViewById(R.id.examples_button);
        exampleSearchButton.setOnClickListener(this);

        exampleSearchKey = DictUtils.extractSearchKey(wwwjdicEntry);
        disableExampleSearchIfSingleKanji();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (tts != null) {
            tts.shutdown();
        }
    }

    private Intent createCrossRefIntent(String word) {
        String dictionary = getApp().getCurrentDictionary();
        Log.d(TAG, String.format(
                "Will look for compounds in dictionary: %s(%s)", getApp()
                        .getCurrentDictionaryName(), dictionary));
        SearchCriteria criteria = SearchCriteria.createForDictionary(word,
                true, false, false, dictionary);
        Intent intent = new Intent(DictionaryEntryDetail.this,
                DictionaryResultListView.class);
        intent.putExtra(Constants.CRITERIA_KEY, criteria);
        return intent;
    }

    private void disableExampleSearchIfSingleKanji() {
        if (exampleSearchKey.length() == 1) {
            exampleSearchButton.setEnabled(false);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.examples_button:
            Intent intent = new Intent(this, ExamplesResultListView.class);
            SearchCriteria criteria = SearchCriteria.createForExampleSearch(
                    exampleSearchKey, false, DEFAULT_MAX_NUM_EXAMPLES);
            intent.putExtra(Constants.CRITERIA_KEY, criteria);

            startActivity(intent);
            break;
        default:
            // do nothing
        }
    }

    @Override
    protected void setHomeActivityExtras(Intent homeActivityIntent) {
        homeActivityIntent.putExtra(SELECTED_TAB_IDX, DICTIONARY_TAB_IDX);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ITEM_ID_LOOKUP_KANJI, 0, R.string.lookup_kanji).setIcon(
                android.R.drawable.ic_menu_search);

        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
        case ITEM_ID_HOME:
            Intent intent = new Intent(this, Wwwjdic.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            setHomeActivityExtras(intent);

            startActivity(intent);
            finish();

            return true;
        case ITEM_ID_LOOKUP_KANJI:
            Activities.lookupKanji(this, db, wwwjdicEntry.getHeadword());
            return true;
        default:
            // do nothing
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected Locale getSpeechLocale() {
        WwwjdicApplication app = getApp();
        String currentDictionary = app.getCurrentDictionary();
        String[] engDictsArr = { "1", "3", "4", "5", "6", "7", "8", "A", "B",
                "C", "D" };
        List<String> engDicts = Arrays.asList(engDictsArr);
        if (engDicts.contains(currentDictionary)) {
            return Locale.ENGLISH;
        } else {
            if ("F".equals(currentDictionary)) {
                return Locale.GERMAN;
            } else if ("G".equals(currentDictionary)) {
                return Locale.FRENCH;
            } else if ("H".equals(currentDictionary)) {
                return new Locale("RU");
            } else if ("I".equals(currentDictionary)) {
                return new Locale("SE");
            } else if ("J".equals(currentDictionary)) {
                return new Locale("HU");
            } else if ("K".equals(currentDictionary)) {
                return new Locale("ES");
            } else if ("L".equals(currentDictionary)) {
                return new Locale("NL");
            } else if ("M".equals(currentDictionary)) {
                return new Locale("SL");
            }
        }

        return null;
    }

    protected void showTtsButtons() {
        toggleTtsButtons(true);
    }

    @Override
    protected void hideTtsButtons() {
        toggleTtsButtons(false);
    }

    private void toggleTtsButtons(boolean show) {
        int childCount = translationsLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = translationsLayout.getChildAt(i);
            if (view instanceof Button) {
                if (show) {
                    view.setVisibility(View.VISIBLE);
                } else {
                    view.setVisibility(View.INVISIBLE);
                }
            } else if (view instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view;
                int count = vg.getChildCount();
                for (int j = 0; j < count; j++) {
                    view = vg.getChildAt(j);
                    if (view instanceof Button) {
                        if (show) {
                            view.setVisibility(View.VISIBLE);
                        } else {
                            view.setVisibility(View.INVISIBLE);
                        }
                    }
                }
            }
        }
    }
}
