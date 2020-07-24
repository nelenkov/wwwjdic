package org.nick.wwwjdic;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

import org.nick.wwwjdic.actionprovider.ShareActionProvider;
import org.nick.wwwjdic.model.JlptLevels;
import org.nick.wwwjdic.model.KanjiEntry;
import org.nick.wwwjdic.model.Radical;
import org.nick.wwwjdic.model.Radicals;
import org.nick.wwwjdic.model.SearchCriteria;
import org.nick.wwwjdic.utils.DictUtils;
import org.nick.wwwjdic.utils.Pair;
import org.nick.wwwjdic.utils.StringUtils;
import org.nick.wwwjdic.utils.UIUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;

public class KanjiEntryDetailFragment extends DetailFragment {

    private static final String TAG = KanjiEntryDetailFragment.class
            .getSimpleName();

    private static final List<String> ELEMENTARY_GRADES = Arrays.asList("1", "2", "3", "4", "5", "6" );
    private static final List<String> SECONDARY_GRADES = Arrays.asList("8");
    private static final List<String> JINMEIYOU_GRADES = Arrays.asList("9", "10");

    private LinearLayout translationsLayout;
    private Toolbar toolbar;

    private KanjiEntry entry;

    private ShareActionProvider shareActionProvider;

    public KanjiEntryDetailFragment() {
        setHasOptionsMenu(true);
    }

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

        if (savedInstanceState != null) {
            entry = (KanjiEntry) savedInstanceState
                    .getSerializable(KanjiEntryDetail.EXTRA_KANJI_ENTRY);
            wwwjdicEntry = entry;
            isFavorite = savedInstanceState.getBoolean(
                    KanjiEntryDetail.EXTRA_IS_FAVORITE, false);
        }

        Bundle args = getArguments();
        if (args != null) {
            entry = (KanjiEntry) args
                    .getSerializable(KanjiEntryDetail.EXTRA_KANJI_ENTRY);
            wwwjdicEntry = entry;
            isFavorite = args.getBoolean(KanjiEntryDetail.EXTRA_IS_FAVORITE,
                    false);
        }

        String message = getResources().getString(R.string.details_for);
        getActivity().setTitle(String.format(message, entry.getKanji()));

        View v = getView();
        if (v == null) {
            return;
        }

        TextView entryView = v.findViewById(R.id.kanjiText);
        UIUtils.setJpTextLocale(entryView);
        entryView.setText(entry.getKanji());
        entryView.setOnLongClickListener(this);

        TextView radicalGlyphText = v.findViewById(R.id.radicalGlyphText);
        UIUtils.setJpTextLocale(radicalGlyphText);
        // radicalGlyphText.setTextSize(30f);
        Radicals radicals = Radicals.getInstance();
        Radical radical = radicals.getRadicalByNumber(entry.getRadicalNumber());
        if (radical != null) {
            radicalGlyphText.setText(radical.getGlyph().substring(0, 1));
        }

        TextView radicalNumberView = v.findViewById(R.id.radicalNumberText);
        radicalNumberView.setText(Integer.toString(entry.getRadicalNumber()));

        TextView strokeCountView = v.findViewById(R.id.strokeCountText);
        strokeCountView.setText(Integer.toString(entry.getStrokeCount()));

        TextView compoundsLinkStarting = v.findViewById(R.id.compound_link_starting);
        compoundsLinkStarting.setNextFocusDownId(R.id.compound_link_any);
        Intent intent = createCompoundSearchIntent(
                SearchCriteria.KANJI_COMPOUND_SEARCH_TYPE_STARTING, false);
        makeClickable(compoundsLinkStarting, intent);

        TextView compoundsLinkAny = v.findViewById(R.id.compound_link_any);
        compoundsLinkAny.setNextFocusDownId(R.id.compound_link_common);
        compoundsLinkAny.setNextFocusUpId(R.id.compound_link_starting);
        intent = createCompoundSearchIntent(
                SearchCriteria.KANJI_COMPOUND_SEARCH_TYPE_ANY, false);
        makeClickable(compoundsLinkAny, intent);

        TextView compoundsLinkCommon = v.findViewById(R.id.compound_link_common);
        compoundsLinkCommon.setNextFocusUpId(R.id.compound_link_any);
        intent = createCompoundSearchIntent(
                SearchCriteria.KANJI_COMPOUND_SEARCH_TYPE_NONE, true);
        makeClickable(compoundsLinkCommon, intent);

        ScrollView meaningsScroll = v.findViewById(R.id.meaningsScroll);
        meaningsScroll.setNextFocusUpId(R.id.compound_link_common);

        LinearLayout readingLayout = v.findViewById(R.id.readingLayout);

        if (entry.getReading() != null) {
            TextView onyomiView = v.findViewById(R.id.details_onyomi_text);
            UIUtils.setJpTextLocale(onyomiView);
            onyomiView.setText(entry.getOnyomi());

            TextView kunyomiView = v.findViewById(R.id.details_kunyomi_text);
            UIUtils.setJpTextLocale(kunyomiView);
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
            float textSize = getResources().getDimension(
                    R.dimen.kanji_detail_nanori_label_size);
            labelView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            labelView.setGravity(Gravity.CENTER);
            layout.addView(labelView, lp);

            TextView textView = new TextView(getActivity(), null,
                    R.style.dict_detail_reading);
            UIUtils.setJpTextLocale(textView);
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
            UIUtils.setJpTextLocale(textView);
            textView.setText(entry.getRadicalName());
            layout.addView(textView, lp);

            readingLayout.addView(layout);
        }

        translationsLayout = v.findViewById(R.id.translations_layout);
        LinearLayout codesLayout = v.findViewById(R.id.codes_layout);

        if (entry.getMeanings().isEmpty()) {
            Pair<LinearLayout, TextView> translationViews = createMeaningTextView(
                    getActivity(), getResources().getString(R.string.none),
                    false);
            translationsLayout.addView(translationViews.getFirst());
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

        CheckBox starCb = v.findViewById(R.id.star_kanji);
        starCb.setOnCheckedChangeListener(null);
        starCb.setChecked(isFavorite);
        starCb.setOnCheckedChangeListener(this);

        toolbar = getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.inflateMenu(R.menu.kanji_detail);
            for (int i = 0; i < toolbar.getChildCount(); i++) {
                View child = toolbar.getChildAt(i);
                if (child instanceof ActionMenuView) {
                    child.getLayoutParams().width = ActionMenuView.LayoutParams.MATCH_PARENT;
                }
            }

            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onOptionsItemSelected(item);
                }
            });
            toolbar.setOnCreateContextMenuListener(new Toolbar.OnCreateContextMenuListener() {

                @Override
                public void onCreateContextMenu(ContextMenu contextMenu, View view,
                                                ContextMenu.ContextMenuInfo contextMenuInfo) {

                }
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }

       return inflater.inflate(R.layout.kanji_entry_details_fragment,
                container, false);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menu_kanji_detail_create_flashcard);
        if (item != null) {
            item.setEnabled(canCreateFlashcards());
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (toolbar == null) {
            inflater.inflate(R.menu.kanji_detail, menu);
        }

        MenuItem menuItem = menu.findItem(R.id.menu_kanji_detail_share);
        shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        if (shareActionProvider != null) {
            shareActionProvider.setShareIntent(createShareIntent());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_kanji_detail_stroke_order) {
            Activities.showStrokeOrder(getActivity(), entry);
            return true;
        } else if (item.getItemId() == R.id.menu_kanji_detail_copy) {
            copy();
            return true;
        } else if (item.getItemId() == R.id.menu_kanji_detail_share) {
            if (shareActionProvider != null) {
                return false;
            }

            share();

            return true;
        } else if (item.getItemId() == R.id.menu_kanji_detail_create_flashcard) {
            if (canCreateFlashcards()) {
                startActivity(createFlashcardIntent());
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KanjiEntryDetail.EXTRA_KANJI_ENTRY, entry);
        outState.putBoolean(KanjiEntryDetail.EXTRA_IS_FAVORITE, isFavorite);
    }

    private void addCodesTable(LinearLayout meaningsCodesLayout,
            List<Pair<String, String>> codesData) {
        TableLayout table = new TableLayout(getActivity());
        for (Pair<String, String> codesEntry : codesData) {
            TableRow row = new TableRow(getActivity());

            float textSize = getResources().getDimension(
                    R.dimen.kanji_detail_codes_size);
            TextView labelView = new TextView(getActivity());
            labelView.setText(codesEntry.getFirst());
            labelView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            labelView.setGravity(Gravity.START);
            row.addView(labelView);

            TextView textView = new TextView(getActivity());
            textView.setText(codesEntry.getSecond());
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            textView.setGravity(Gravity.START);
            textView.setPadding(10, 0, 0, 0);
            row.addView(textView);

            table.addView(row);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        meaningsCodesLayout.addView(table, lp);
    }

    private Intent createCrossRefIntent(String kanji) {
        SearchCriteria criteria = SearchCriteria.createForKanjiOrReading(kanji);
        Intent intent = new Intent(getActivity(), KanjiResultList.class);
        intent.putExtra(Wwwjdic.EXTRA_CRITERIA, criteria);
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
        Intent intent = new Intent(getActivity(), DictionaryResultList.class);
        intent.putExtra(Wwwjdic.EXTRA_CRITERIA, criteria);
        return intent;
    }

    private void makeClickable(TextView textView, Intent intent) {
        makeClickable(textView, 0, textView.getText().length(), intent);
    }

    @SuppressLint("DefaultLocale")
    private List<Pair<String, String>> createCodesData(KanjiEntry entry) {
        ArrayList<Pair<String, String>> data = new ArrayList<>();

        if (entry.getUnicodeNumber() != null) {
            data.add(new Pair<>(getStr(R.string.unicode_number),
                    entry.getUnicodeNumber().toUpperCase()));
        }

        if (entry.getJisCode() != null) {
            data.add(new Pair<>(getStr(R.string.jis_code), entry
                    .getJisCode().toUpperCase()));
        }

        String kanji = entry.getHeadword();
        try {
            byte[] sjis = kanji.getBytes("SJIS");
            if (sjis.length < 2) {
                Log.w(TAG, "Unable to encode " + kanji + " as SJIS");
                data.add(new Pair<>(getStr(R.string.sjis_code),
                        "N/A"));
            } else {
                String sjisCode = String.format("%02x%02x", sjis[0], sjis[1])
                        .toUpperCase();
                data.add(new Pair<>(getStr(R.string.sjis_code),
                        sjisCode));
            }
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "SJIS conversion not supported", e);
        }

        if (entry.getClassicalRadicalNumber() != null) {
            data.add(new Pair<>(
                    getStr(R.string.classical_radical), entry
                            .getClassicalRadicalNumber().toString()));
        }

        if (entry.getFrequncyeRank() != null) {
            data.add(new Pair<>(getStr(R.string.freq_rank), entry
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
            data.add(new Pair<>(getStr(R.string.grade), gradeStr));
        }

        if (entry.getJlptLevel() != null) {
            String newLevel = JlptLevels.getInstance().getLevel(
                    entry.getKanji());
            String levelStr = entry.getJlptLevel().toString();
            if (newLevel != null) {
                levelStr += " (" + newLevel + ")";
            }
            data.add(new Pair<>(getStr(R.string.jlpt_level),
                    levelStr));
        }

        if (entry.getSkipCode() != null) {
            data.add(new Pair<>(getStr(R.string.skip_code), entry
                    .getSkipCode()));
        }

        if (entry.getKoreanReading() != null) {
            data.add(new Pair<>(getStr(R.string.korean_reading),
                    entry.getKoreanReading()));
        }

        if (entry.getPinyin() != null) {
            data.add(new Pair<>(getStr(R.string.pinyn), entry
                    .getPinyin()));
        }

        return data;
    }

    private String getStr(int id) {
        return getResources().getText(id).toString();
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
        // XXX -- called to early?
        if (v == null) {
            return;
        }
        Button speakButton = v.findViewById(R.id.jp_speak_button);
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

    public KanjiEntry getEntry() {
        return entry;
    }

}
