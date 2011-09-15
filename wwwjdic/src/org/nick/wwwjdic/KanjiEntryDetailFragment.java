package org.nick.wwwjdic;

import static org.nick.wwwjdic.Constants.KANJI_TAB_IDX;
import static org.nick.wwwjdic.Constants.SELECTED_TAB_IDX;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import org.nick.wwwjdic.model.JlptLevels;
import org.nick.wwwjdic.model.KanjiEntry;
import org.nick.wwwjdic.model.Radical;
import org.nick.wwwjdic.model.Radicals;
import org.nick.wwwjdic.model.SearchCriteria;
import org.nick.wwwjdic.utils.Analytics;
import org.nick.wwwjdic.utils.DictUtils;
import org.nick.wwwjdic.utils.Pair;
import org.nick.wwwjdic.utils.StringUtils;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class KanjiEntryDetailFragment extends DetailFragment implements
        OnClickListener {

    private static final String TAG = KanjiEntryDetailFragment.class
            .getSimpleName();

    private static final List<String> ELEMENTARY_GRADES = Arrays
            .asList(new String[] { "1", "2", "3", "4", "5", "6" });
    private static final List<String> SECONDARY_GRADES = Arrays
            .asList(new String[] { "8" });
    private static final List<String> JINMEIYOU_GRADES = Arrays
            .asList(new String[] { "9", "10" });

    private LinearLayout translationsLayout;
    private LinearLayout codesLayout;

    private KanjiEntry entry;

    public static KanjiEntryDetailFragment newInstance(int index,
            KanjiEntry entry) {
        KanjiEntryDetailFragment f = new KanjiEntryDetailFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt("index", index);
        args.putSerializable(KanjiEntryDetail.EXTRA_KANJI_ENTRY, entry);
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

        entry = (KanjiEntry) getArguments().getSerializable(
                KanjiEntryDetail.EXTRA_KANJI_ENTRY);
        wwwjdicEntry = entry;
        isFavorite = getArguments().getBoolean(
                KanjiEntryDetail.EXTRA_IS_FAVORITE, false);
        boolean kodWidgetClicked = getArguments().getBoolean(
                Constants.KOD_WIDGET_CLICK, false);
        if (kodWidgetClicked) {
            Analytics.startSession(getActivity());
            Analytics.event("kodWidgetClicked", getActivity());
        }

        String message = getResources().getString(R.string.details_for);
        getActivity().setTitle(String.format(message, entry.getKanji()));

        TextView entryView = (TextView) getActivity().findViewById(
                R.id.kanjiText);
        entryView.setText(entry.getKanji());
        entryView.setOnLongClickListener(this);

        TextView radicalGlyphText = (TextView) getActivity().findViewById(
                R.id.radicalGlyphText);
        // radicalGlyphText.setTextSize(30f);
        Radicals radicals = Radicals.getInstance();
        Radical radical = radicals.getRadicalByNumber(entry.getRadicalNumber());
        if (radical != null) {
            radicalGlyphText.setText(radical.getGlyph().substring(0, 1));
        }

        TextView radicalNumberView = (TextView) getActivity().findViewById(
                R.id.radicalNumberText);
        radicalNumberView.setText(Integer.toString(entry.getRadicalNumber()));

        TextView strokeCountView = (TextView) getActivity().findViewById(
                R.id.strokeCountText);
        strokeCountView.setText(Integer.toString(entry.getStrokeCount()));

        Button sodButton = (Button) getActivity().findViewById(R.id.sod_button);
        sodButton.setOnClickListener(this);
        sodButton.setNextFocusDownId(R.id.compound_link_starting);

        TextView compoundsLinkStarting = (TextView) getActivity().findViewById(
                R.id.compound_link_starting);
        compoundsLinkStarting.setNextFocusDownId(R.id.compound_link_any);
        compoundsLinkStarting.setNextFocusUpId(R.id.sod_button);
        Intent intent = createCompoundSearchIntent(
                SearchCriteria.KANJI_COMPOUND_SEARCH_TYPE_STARTING, false);
        makeClickable(compoundsLinkStarting, intent);

        TextView compoundsLinkAny = (TextView) getActivity().findViewById(
                R.id.compound_link_any);
        compoundsLinkAny.setNextFocusDownId(R.id.compound_link_common);
        compoundsLinkAny.setNextFocusUpId(R.id.compound_link_starting);
        intent = createCompoundSearchIntent(
                SearchCriteria.KANJI_COMPOUND_SEARCH_TYPE_ANY, false);
        makeClickable(compoundsLinkAny, intent);

        TextView compoundsLinkCommon = (TextView) getActivity().findViewById(
                R.id.compound_link_common);
        compoundsLinkCommon.setNextFocusUpId(R.id.compound_link_any);
        intent = createCompoundSearchIntent(
                SearchCriteria.KANJI_COMPOUND_SEARCH_TYPE_NONE, true);
        makeClickable(compoundsLinkCommon, intent);

        ScrollView meaningsScroll = (ScrollView) getActivity().findViewById(
                R.id.meaningsScroll);
        meaningsScroll.setNextFocusUpId(R.id.compound_link_common);

        LinearLayout readingLayout = (LinearLayout) getActivity().findViewById(
                R.id.readingLayout);

        if (entry.getReading() != null) {
            TextView onyomiView = (TextView) getActivity().findViewById(
                    R.id.details_onyomi_text);
            onyomiView.setText(entry.getOnyomi());

            TextView kunyomiView = (TextView) getActivity().findViewById(
                    R.id.details_kunyomi_text);
            kunyomiView.setText(entry.getKunyomi());
        }

        if (!TextUtils.isEmpty(entry.getNanori())) {
            LinearLayout layout = new LinearLayout(getActivity());
            layout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 5, 0);

            TextView labelView = new TextView(getActivity());
            labelView.setText(R.string.nanori_label);
            labelView.setTextSize(10f);
            labelView.setGravity(Gravity.CENTER);
            layout.addView(labelView, lp);

            TextView textView = new TextView(getActivity(), null,
                    R.style.dict_detail_reading);
            textView.setText(entry.getNanori());
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources()
                    .getDimension(R.dimen.details_reading_size));
            layout.addView(textView, lp);

            readingLayout.addView(layout);
        }

        if (!TextUtils.isEmpty(entry.getRadicalName())) {
            LinearLayout layout = new LinearLayout(getActivity());
            layout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 5, 0);

            TextView labelView = new TextView(getActivity());
            labelView.setText(R.string.radical_name_label);
            labelView.setTextSize(10f);
            labelView.setGravity(Gravity.CENTER);
            layout.addView(labelView, lp);

            TextView textView = new TextView(getActivity(), null,
                    R.style.dict_detail_reading);
            textView.setText(entry.getRadicalName());
            layout.addView(textView, lp);

            readingLayout.addView(layout);
        }

        translationsLayout = (LinearLayout) getActivity().findViewById(
                R.id.translations_layout);
        codesLayout = (LinearLayout) getActivity().findViewById(
                R.id.codes_layout);

        if (entry.getMeanings().isEmpty()) {
            TextView text = new TextView(getActivity(), null,
                    R.style.dict_detail_meaning);
            translationsLayout.addView(text);
        } else {
            for (String meaning : entry.getMeanings()) {
                final Pair<LinearLayout, TextView> translationViews = createMeaningTextView(
                        getActivity(), meaning);
                Matcher m = CROSS_REF_PATTERN.matcher(meaning);
                if (m.matches()) {
                    Intent crossRefIntent = createCrossRefIntent(m.group(1));
                    int start = m.start(1);
                    int end = m.end(1);
                    makeClickable(translationViews.getSecond(), start, end,
                            crossRefIntent);
                }
                translationsLayout.addView(translationViews.getFirst());
            }
        }

        List<Pair<String, String>> codesData = createCodesData(entry);
        addCodesTable(codesLayout, codesData);

        CheckBox starCb = (CheckBox) getActivity()
                .findViewById(R.id.star_kanji);
        starCb.setOnCheckedChangeListener(null);
        starCb.setChecked(isFavorite);
        starCb.setOnCheckedChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.kanji_entry_details, container,
                false);

        return v;
    }

    private void addCodesTable(LinearLayout meaningsCodesLayout,
            List<Pair<String, String>> codesData) {
        TableLayout table = new TableLayout(getActivity());
        for (Pair<String, String> codesEntry : codesData) {
            TableRow row = new TableRow(getActivity());

            TextView labelView = new TextView(getActivity());
            labelView.setText(codesEntry.getFirst());
            labelView.setGravity(Gravity.LEFT);
            row.addView(labelView);

            TextView textView = new TextView(getActivity());
            textView.setText(codesEntry.getSecond());
            textView.setGravity(Gravity.LEFT);
            textView.setPadding(10, 0, 0, 0);
            row.addView(textView);

            table.addView(row);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT);
        meaningsCodesLayout.addView(table, lp);
    }

    private Intent createCrossRefIntent(String kanji) {
        SearchCriteria criteria = SearchCriteria.createForKanjiOrReading(kanji);
        Intent intent = new Intent(getActivity(), KanjiResultListView.class);
        intent.putExtra(Constants.CRITERIA_KEY, criteria);
        return intent;
    }

    private Intent createCompoundSearchIntent(int searchType,
            boolean commonWordsOnly) {
        String dictionary = getApp().getCurrentDictionary();
        Log.d(TAG, String.format(
                "Will look for compounds in dictionary: %s(%s)", getApp()
                        .getCurrentDictionaryName(), dictionary));
        SearchCriteria criteria = SearchCriteria.createForKanjiCompounds(
                entry.getKanji(), searchType, commonWordsOnly, dictionary);
        Intent intent = new Intent(getActivity(),
                DictionaryResultListView.class);
        intent.putExtra(Constants.CRITERIA_KEY, criteria);
        return intent;
    }

    private void makeClickable(TextView textView, Intent intent) {
        makeClickable(textView, 0, textView.getText().length(), intent);
    }

    private List<Pair<String, String>> createCodesData(KanjiEntry entry) {
        ArrayList<Pair<String, String>> data = new ArrayList<Pair<String, String>>();

        if (entry.getUnicodeNumber() != null) {
            data.add(new Pair<String, String>(getStr(R.string.unicode_number),
                    entry.getUnicodeNumber().toUpperCase()));
        }

        if (entry.getJisCode() != null) {
            data.add(new Pair<String, String>(getStr(R.string.jis_code), entry
                    .getJisCode().toUpperCase()));
        }

        String kanji = entry.getHeadword();
        try {
            byte[] sjis = kanji.getBytes("SJIS");
            if (sjis.length < 2) {
                Log.w(TAG, "Unable to encode " + kanji + " as SJIS");
                data.add(new Pair<String, String>(getStr(R.string.sjis_code),
                        "N/A"));
            } else {
                String sjisCode = String.format("%02x%02x", sjis[0], sjis[1])
                        .toUpperCase();
                data.add(new Pair<String, String>(getStr(R.string.sjis_code),
                        sjisCode));
            }
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "SJIS conversion not supported", e);
        }

        if (entry.getClassicalRadicalNumber() != null) {
            data.add(new Pair<String, String>(
                    getStr(R.string.classical_radical), entry
                            .getClassicalRadicalNumber().toString()));
        }

        if (entry.getFrequncyeRank() != null) {
            data.add(new Pair<String, String>(getStr(R.string.freq_rank), entry
                    .getFrequncyeRank().toString()));
        }

        if (entry.getGrade() != null) {
            String rawGrade = entry.getGrade().toString();
            String gradeStr = rawGrade;
            if (ELEMENTARY_GRADES.contains(rawGrade)) {
                gradeStr = getResources().getString(R.string.grade_elementary,
                        rawGrade);
            } else if (SECONDARY_GRADES.contains(rawGrade)) {
                gradeStr = getResources().getString(R.string.grade_secondary);
            } else if (JINMEIYOU_GRADES.contains(rawGrade)) {
                gradeStr = getResources().getString(R.string.grade_jinmeiyou);
            }
            data.add(new Pair<String, String>(getStr(R.string.grade), gradeStr));
        }

        if (entry.getJlptLevel() != null) {
            String newLevel = JlptLevels.getInstance().getLevel(
                    entry.getKanji());
            String levelStr = entry.getJlptLevel().toString();
            if (newLevel != null) {
                levelStr += " (" + newLevel + ")";
            }
            data.add(new Pair<String, String>(getStr(R.string.jlpt_level),
                    levelStr));
        }

        if (entry.getSkipCode() != null) {
            data.add(new Pair<String, String>(getStr(R.string.skip_code), entry
                    .getSkipCode()));
        }

        if (entry.getKoreanReading() != null) {
            data.add(new Pair<String, String>(getStr(R.string.korean_reading),
                    entry.getKoreanReading()));
        }

        if (entry.getPinyin() != null) {
            data.add(new Pair<String, String>(getStr(R.string.pinyn), entry
                    .getPinyin()));
        }

        return data;
    }

    private String getStr(int id) {
        return getResources().getText(id).toString();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.sod_button:
            Activities.showStrokeOrder(getActivity(), entry);
            break;
        default:
            // do nothing
        }
    }

    @Override
    protected void setHomeActivityExtras(Intent homeActivityIntent) {
        homeActivityIntent.putExtra(SELECTED_TAB_IDX, KANJI_TAB_IDX);
    }

    @Override
    protected Locale getSpeechLocale() {
        return Locale.ENGLISH;
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

    @Override
    protected void toggleJpTtsButtons(boolean show) {
        View v = getView();
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

                    pronounce(entry.getOnyomi());
                    pronounce(entry.getKunyomi());
                    pronounce(entry.getNanori());
                }
            });
        }
    }

    private void pronounce(String text) {
        if (StringUtils.isEmpty(text)) {
            return;
        }

        String toSpeak = DictUtils.stripWwwjdicTags(getActivity(), text);
        if (toSpeak.contains(";")) {
            toSpeak = toSpeak.split(";")[0];
        }
        if (toSpeak.contains(".")) {
            toSpeak = toSpeak.replaceAll("\\.", "");
        }

        String[] words = toSpeak.split(" ");
        for (String word : words) {
            jpTts.speak(word, TextToSpeech.QUEUE_ADD, null);
        }
    }

}
