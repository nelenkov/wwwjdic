package org.nick.wwwjdic;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import org.nick.wwwjdic.model.DictionaryEntry;
import org.nick.wwwjdic.model.SearchCriteria;
import org.nick.wwwjdic.utils.DictUtils;
import org.nick.wwwjdic.utils.Pair;
import org.nick.wwwjdic.utils.StringUtils;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DictionaryEntryDetailFragment extends DetailFragment implements
        OnClickListener {

    private static final String TAG = DictionaryEntryDetailFragment.class
            .getSimpleName();

    private static final int DEFAULT_MAX_NUM_EXAMPLES = 20;

    private LinearLayout translationsLayout;
    private TextView entryView;
    private CheckBox starCb;
    private Button exampleSearchButton;

    private DictionaryEntry entry;
    private String exampleSearchKey;

    public DictionaryEntryDetailFragment() {
        setHasOptionsMenu(true);
    }

    public static DictionaryEntryDetailFragment newInstance(int index,
            DictionaryEntry entry) {
        DictionaryEntryDetailFragment f = new DictionaryEntryDetailFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt("index", index);
        args.putSerializable(DictionaryEntryDetail.EXTRA_DICTIONARY_ENTRY,
                entry);
        f.setArguments(args);

        return f;
    }

    public int getShownIndex() {
        return getArguments().getInt("index", 0);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        checkTtsAvailability();

        entry = (DictionaryEntry) getArguments().getSerializable(
                DictionaryEntryDetail.EXTRA_DICTIONARY_ENTRY);
        wwwjdicEntry = entry;
        isFavorite = getActivity().getIntent().getBooleanExtra(
                DictionaryEntryDetail.EXTRA_IS_FAVORITE, false);

        String message = getResources().getString(R.string.details_for);
        getActivity().setTitle(String.format(message, entry.getWord()));

        View v = getView();
        if (v == null) {
            return;
        }

        entryView = (TextView) v.findViewById(R.id.details_word_text);
        entryView.setText(entry.getWord());
        entryView.setOnLongClickListener(this);

        if (entry.getReading() != null) {
            TextView readingView = (TextView) v
                    .findViewById(R.id.details_word_reading_text);
            readingView.setText(entry.getReading());
        }

        translationsLayout = (LinearLayout) v
                .findViewById(R.id.translations_layout);

        if (entry.getMeanings().isEmpty()) {
            Pair<LinearLayout, TextView> translationViews = createMeaningTextView(
                    getActivity(), getResources().getString(R.string.none),
                    false);
            translationsLayout.addView(translationViews.getFirst());
        } else {
            for (String meaning : entry.getMeanings()) {
                Pair<LinearLayout, TextView> translationViews = createMeaningTextView(
                        getActivity(), meaning);
                Matcher m = CROSS_REF_PATTERN.matcher(meaning);
                if (m.matches()) {
                    Intent intent = createCrossRefIntent(m.group(1));
                    int start = m.start(1);
                    int end = m.end(1);
                    makeClickable(translationViews.getSecond(), start, end,
                            intent);
                }
                translationsLayout.addView(translationViews.getFirst());
            }
        }

        starCb = (CheckBox) v.findViewById(R.id.star_word);
        starCb.setOnCheckedChangeListener(null);
        starCb.setChecked(isFavorite);
        starCb.setOnCheckedChangeListener(this);

        exampleSearchButton = (Button) v.findViewById(R.id.examples_button);
        exampleSearchButton.setOnClickListener(this);

        exampleSearchKey = DictUtils.extractSearchKey(wwwjdicEntry);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }

        View v = inflater.inflate(R.layout.entry_details, container, false);

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.dict_detail, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_dict_detail_lookup_kanji) {
            Activities.lookupKanji(getActivity(), db,
                    wwwjdicEntry.getHeadword());
            return true;
        } else if (item.getItemId() == R.id.menu_dict_detail_copy) {
            copy();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private Intent createCrossRefIntent(String word) {
        String dictionary = getApp().getCurrentDictionary();
        Log.d(TAG, String.format(
                "Will look for compounds in dictionary: %s(%s)", getApp()
                        .getCurrentDictionaryName(), dictionary));
        SearchCriteria criteria = SearchCriteria.createForDictionary(word,
                true, false, false, dictionary);
        Intent intent = new Intent(getActivity(), DictionaryResultList.class);
        intent.putExtra(Wwwjdic.EXTRA_CRITERIA, criteria);
        return intent;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.examples_button) {
            Intent intent = new Intent(getActivity(), ExamplesResultList.class);
            SearchCriteria criteria = SearchCriteria.createForExampleSearch(
                    exampleSearchKey, false, DEFAULT_MAX_NUM_EXAMPLES);
            intent.putExtra(Wwwjdic.EXTRA_CRITERIA, criteria);
            intent.putExtra(
                    ExamplesResultListFragment.EXTRA_EXAMPLES_BACKDOOR_SEARCH,
                    true);
            startActivity(intent);
        }
    }

    @Override
    protected Locale getSpeechLocale() {
        String entryDictionary = entry.getDictionary();
        // make English the default
        if (entryDictionary == null) {
            return Locale.ENGLISH;
        }

        String[] engDictsArr = { "1", "3", "4", "5", "6", "7", "8", "A", "B",
                "C", "D" };
        List<String> engDicts = Arrays.asList(engDictsArr);
        if (engDicts.contains(entryDictionary)) {
            return Locale.ENGLISH;
        } else {
            if ("G".equals(entryDictionary)) {
                return Locale.GERMAN;
            } else if ("H".equals(entryDictionary)) {
                return Locale.FRENCH;
            } else if ("I".equals(entryDictionary)) {
                return new Locale("RU");
            } else if ("J".equals(entryDictionary)) {
                return new Locale("SE");
            } else if ("K".equals(entryDictionary)) {
                return new Locale("HU");
            } else if ("L".equals(entryDictionary)) {
                return new Locale("ES");
            } else if ("M".equals(entryDictionary)) {
                return new Locale("NL");
            } else if ("N".equals(entryDictionary)) {
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
        if (translationsLayout == null) {
            return;
        }

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

    @Override
    protected void toggleJpTtsButtons(boolean show) {
        View v = getView();
        if (v == null) {
            return;
        }
        Button speakButton = (Button) v.findViewById(R.id.jp_speak_button);
        speakButton.setVisibility(show ? View.VISIBLE : View.INVISIBLE);

        if (show) {
            if (jpTts == null) {
                return;
            }

            Locale jp = Locale.JAPAN;
            if (jpTts.isLanguageAvailable(jp) == TextToSpeech.LANG_MISSING_DATA
                    && jpTts.isLanguageAvailable(jp) == TextToSpeech.LANG_NOT_SUPPORTED) {
                speakButton.setVisibility(View.INVISIBLE);
                return;
            }

            speakButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (jpTts == null) {
                        return;
                    }

                    pronounce(entry.getReading() != null ? entry.getReading()
                            : entry.getHeadword());
                }
            });
        }
    }

    private void pronounce(String text) {
        if (StringUtils.isEmpty(text)) {
            return;
        }

        String toSpeak = DictUtils.stripWwwjdicTags(getActivity(), text);
        if (toSpeak.contains(".")) {
            toSpeak = toSpeak.replaceAll("\\.", "");
        }

        String[] words = toSpeak.split(";");
        for (String word : words) {
            jpTts.speak(word, TextToSpeech.QUEUE_ADD, null);
        }
    }
}
