package org.nick.wwwjdic;

import static org.nick.wwwjdic.Constants.KANJI_TAB_IDX;
import static org.nick.wwwjdic.Constants.SELECTED_TAB_IDX;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.nick.wwwjdic.utils.Analytics;
import org.nick.wwwjdic.utils.Pair;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
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

public class KanjiEntryDetail extends DetailActivity implements OnClickListener {

    private static final String TAG = KanjiEntryDetail.class.getSimpleName();

    private KanjiEntry entry;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kanji_entry_details);

        entry = (KanjiEntry) getIntent().getSerializableExtra(
                Constants.KANJI_ENTRY_KEY);
        wwwjdicEntry = entry;
        isFavorite = getIntent().getBooleanExtra(Constants.IS_FAVORITE, false);
        boolean kodWidgetClicked = getIntent().getBooleanExtra(
                Constants.KOD_WIDGET_CLICK, false);
        if (kodWidgetClicked) {
            Analytics.startSession(this);
            Analytics.event("kodWidgetClicked", this);
        }

        String message = getResources().getString(R.string.details_for);
        setTitle(String.format(message, entry.getKanji()));

        TextView entryView = (TextView) findViewById(R.id.kanjiText);
        entryView.setText(entry.getKanji());
        entryView.setOnLongClickListener(this);

        TextView radicalGlyphText = (TextView) findViewById(R.id.radicalGlyphText);
        // radicalGlyphText.setTextSize(30f);
        Radicals radicals = Radicals.getInstance();
        Radical radical = radicals.getRadicalByNumber(entry.getRadicalNumber());
        radicalGlyphText.setText(radical.getGlyph().substring(0, 1));

        TextView radicalNumberView = (TextView) findViewById(R.id.radicalNumberText);
        radicalNumberView.setText(Integer.toString(entry.getRadicalNumber()));

        TextView strokeCountView = (TextView) findViewById(R.id.strokeCountText);
        strokeCountView.setText(Integer.toString(entry.getStrokeCount()));

        Button sodButton = (Button) findViewById(R.id.sod_button);
        sodButton.setOnClickListener(this);
        sodButton.setNextFocusDownId(R.id.compound_link_starting);

        TextView compoundsLinkStarting = (TextView) findViewById(R.id.compound_link_starting);
        compoundsLinkStarting.setNextFocusDownId(R.id.compound_link_any);
        compoundsLinkStarting.setNextFocusUpId(R.id.sod_button);
        Intent intent = createCompoundSearchIntent(
                SearchCriteria.KANJI_COMPOUND_SEARCH_TYPE_STARTING, false);
        makeClickable(compoundsLinkStarting, intent);

        TextView compoundsLinkAny = (TextView) findViewById(R.id.compound_link_any);
        compoundsLinkAny.setNextFocusDownId(R.id.compound_link_common);
        compoundsLinkAny.setNextFocusUpId(R.id.compound_link_starting);
        intent = createCompoundSearchIntent(
                SearchCriteria.KANJI_COMPOUND_SEARCH_TYPE_ANY, false);
        makeClickable(compoundsLinkAny, intent);

        TextView compoundsLinkCommon = (TextView) findViewById(R.id.compound_link_common);
        compoundsLinkCommon.setNextFocusUpId(R.id.compound_link_any);
        intent = createCompoundSearchIntent(
                SearchCriteria.KANJI_COMPOUND_SEARCH_TYPE_NONE, true);
        makeClickable(compoundsLinkCommon, intent);

        ScrollView meaningsScroll = (ScrollView) findViewById(R.id.meaningsScroll);
        meaningsScroll.setNextFocusUpId(R.id.compound_link_common);

        LinearLayout readingLayout = (LinearLayout) findViewById(R.id.readingLayout);

        if (entry.getReading() != null) {
            TextView onyomiView = new TextView(this, null,
                    R.style.dict_detail_reading);
            onyomiView.setText(entry.getOnyomi());
            readingLayout.addView(onyomiView);

            TextView kunyomiView = new TextView(this, null,
                    R.style.dict_detail_reading);
            kunyomiView.setText(entry.getKunyomi());
            readingLayout.addView(kunyomiView);
        }

        if (!TextUtils.isEmpty(entry.getNanori())) {
            LinearLayout layout = new LinearLayout(KanjiEntryDetail.this);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 5, 0);

            TextView labelView = new TextView(KanjiEntryDetail.this);
            labelView.setText(R.string.nanori_label);
            labelView.setTextSize(10f);
            labelView.setGravity(Gravity.CENTER);
            layout.addView(labelView, lp);

            TextView textView = new TextView(this, null,
                    R.style.dict_detail_reading);
            textView.setText(entry.getNanori());
            layout.addView(textView, lp);

            readingLayout.addView(layout);
        }

        if (!TextUtils.isEmpty(entry.getRadicalName())) {
            LinearLayout layout = new LinearLayout(KanjiEntryDetail.this);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 5, 0);

            TextView labelView = new TextView(KanjiEntryDetail.this);
            labelView.setText(R.string.radical_name_label);
            labelView.setTextSize(10f);
            labelView.setGravity(Gravity.CENTER);
            layout.addView(labelView, lp);

            TextView textView = new TextView(this, null,
                    R.style.dict_detail_reading);
            textView.setText(entry.getRadicalName());
            layout.addView(textView, lp);

            readingLayout.addView(layout);
        }

        LinearLayout meaningsCodesLayout = (LinearLayout) findViewById(R.id.meaningsCodesLayout);

        if (entry.getMeanings().isEmpty()) {
            TextView text = new TextView(this, null,
                    R.style.dict_detail_meaning);
            meaningsCodesLayout.addView(text);
        } else {
            for (String meaning : entry.getMeanings()) {
                TextView text = new TextView(this, null,
                        R.style.dict_detail_meaning);
                text.setText(meaning);
                Matcher m = CROSS_REF_PATTERN.matcher(meaning);
                if (m.matches()) {
                    Intent crossRefIntent = createCrossRefIntent(m.group(1));
                    int start = m.start(1);
                    int end = m.end(1);
                    makeClickable(text, start, end, crossRefIntent);
                }
                meaningsCodesLayout.addView(text);
            }
        }

        TextView moreLabel = new TextView(this);
        moreLabel.setText(R.string.codes_more);
        moreLabel.setTextColor(Color.WHITE);
        moreLabel.setBackgroundColor(Color.GRAY);
        meaningsCodesLayout.addView(moreLabel);

        List<Pair<String, String>> codesData = createCodesData(entry);
        addCodesTable(meaningsCodesLayout, codesData);

        CheckBox starCb = (CheckBox) findViewById(R.id.star_kanji);
        starCb.setOnCheckedChangeListener(null);
        starCb.setChecked(isFavorite);
        starCb.setOnCheckedChangeListener(this);
    }

    private void addCodesTable(LinearLayout meaningsCodesLayout,
            List<Pair<String, String>> codesData) {
        TableLayout table = new TableLayout(this);
        for (Pair<String, String> codesEntry : codesData) {
            TableRow row = new TableRow(this);

            TextView labelView = new TextView(KanjiEntryDetail.this);
            labelView.setText(codesEntry.getFirst());
            labelView.setGravity(Gravity.LEFT);
            row.addView(labelView);

            TextView textView = new TextView(KanjiEntryDetail.this);
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
        Intent intent = new Intent(this, KanjiResultListView.class);
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
        Intent intent = new Intent(KanjiEntryDetail.this,
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
            data.add(new Pair<String, String>(getStr(R.string.grade), entry
                    .getGrade().toString()));
        }

        if (entry.getJlptLevel() != null) {
            data.add(new Pair<String, String>(getStr(R.string.jlpt_level),
                    entry.getJlptLevel().toString()));
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
            Activities.showStrokeOrder(this, entry);
            break;
        default:
            // do nothing
        }
    }

    @Override
    protected void setHomeActivityExtras(Intent homeActivityIntent) {
        homeActivityIntent.putExtra(SELECTED_TAB_IDX, KANJI_TAB_IDX);
    }

}
