package org.nick.wwwjdic;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class KanjiEntryDetail extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.kanji_entry_details);

		KanjiEntry entry = (KanjiEntry) getIntent().getSerializableExtra(
				Constants.KANJI_ENTRY_KEY);

		setTitle(String.format("Details for '%s'", entry.getKanji()));

		LinearLayout detailLayout = (LinearLayout) findViewById(R.id.kanjiDetailLayout);

		TextView entryView = (TextView) findViewById(R.id.kanjiText);
		entryView.setText(entry.getKanji());

		TextView radicalGlyphText = (TextView) findViewById(R.id.radicalGlyphText);
		radicalGlyphText.setTextSize(24f);
		Radicals radicals = Radicals.getInstance();
		Radical radical = radicals.getRadicalByNumber(entry.getRadicalNumber());
		radicalGlyphText.setText(radical.getGlyph().substring(0, 1));

		TextView radicalNumberView = (TextView) findViewById(R.id.radicalNumberText);
		radicalNumberView.setText(Integer.toString(entry.getRadicalNumber()));

		TextView strokeCountView = (TextView) findViewById(R.id.strokeCountText);
		strokeCountView.setText(Integer.toString(entry.getStrokeCount()));

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

		for (String meaning : entry.getMeanings()) {
			TextView text = new TextView(this, null,
					R.style.dict_detail_meaning);
			text.setText(meaning);
			detailLayout.addView(text);
		}
		TextView moreLabel = new TextView(this);
		moreLabel.setText(R.string.codes_more);
		moreLabel.setTextColor(Color.WHITE);
		moreLabel.setBackgroundColor(Color.GRAY);
		detailLayout.addView(moreLabel);

		ExpandableListView expandableList = new ExpandableListView(this);
		KanjiCodesAdapter kanjiCodesAdapter = new KanjiCodesAdapter(entry);
		expandableList.setAdapter(kanjiCodesAdapter);
		detailLayout.addView(expandableList);

	}

	private class KanjiCodesAdapter extends BaseExpandableListAdapter {

		private KanjiEntry entry;
		private List<Pair<String, String>> data = null;

		public KanjiCodesAdapter(KanjiEntry entry) {
			this.entry = entry;
		}

		private List<Pair<String, String>> getData() {
			if (data == null) {
				data = new ArrayList<Pair<String, String>>();
				if (entry.getJisCode() != null) {
					data.add(new Pair<String, String>(
							getStr(R.string.jis_code), entry.getJisCode()));
				}

				if (entry.getUnicodeNumber() != null) {
					data.add(new Pair<String, String>(
							getStr(R.string.unicode_number), entry
									.getUnicodeNumber()));
				}

				if (entry.getClassicalRadicalNumber() != null) {
					data.add(new Pair<String, String>(
							getStr(R.string.unicode_number), entry
									.getClassicalRadicalNumber().toString()));
				}

				if (entry.getFrequncyeRank() != null) {
					data.add(new Pair<String, String>(
							getStr(R.string.freq_rank), entry
									.getFrequncyeRank().toString()));
				}

				if (entry.getGrade() != null) {
					data.add(new Pair<String, String>(getStr(R.string.grade),
							entry.getGrade().toString()));
				}

				if (entry.getJlptLevel() != null) {
					data.add(new Pair<String, String>(
							getStr(R.string.jlpt_level), entry.getJlptLevel()
									.toString()));
				}

				if (entry.getSkipCode() != null) {
					data.add(new Pair<String, String>(
							getStr(R.string.skip_code), entry.getSkipCode()));
				}

				if (entry.getKoreanReading() != null) {
					data.add(new Pair<String, String>(
							getStr(R.string.korean_reading), entry
									.getKoreanReading()));
				}

				if (entry.getPinyin() != null) {
					data.add(new Pair<String, String>(getStr(R.string.pinyn),
							entry.getPinyin()));
				}
			}

			return data;
		}

		public Object getChild(int groupPosition, int childPosition) {
			return getData().get(childPosition);
		}

		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			Pair<String, String> childData = getData().get(childPosition);

			return createLabelTextView(childData);
		}

		private View createLabelTextView(Pair<String, String> data) {
			LinearLayout layout = new LinearLayout(KanjiEntryDetail.this);
			layout.setOrientation(LinearLayout.HORIZONTAL);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			lp.setMargins(5, 0, 5, 0);

			TextView labelView = new TextView(KanjiEntryDetail.this);
			labelView.setText(data.getFirst());
			labelView.setGravity(Gravity.LEFT);
			layout.addView(labelView, lp);

			TextView textView = new TextView(KanjiEntryDetail.this);
			textView.setText(data.getSecond());
			textView.setGravity(Gravity.RIGHT);
			layout.addView(textView, lp);

			return layout;
		}

		public int getChildrenCount(int groupPosition) {
			return getData().size();
		}

		public Object getGroup(int groupPosition) {
			return getStr(R.string.more);
		}

		public int getGroupCount() {
			return 1;
		}

		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT, 64);

			TextView textView = new TextView(KanjiEntryDetail.this);
			textView.setLayoutParams(lp);
			textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			textView.setPadding(36, 0, 0, 0);
			textView.setText(getStr(R.string.more));

			return textView;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
		}
	}

	private String getStr(int id) {
		return getResources().getText(id).toString();
	}
}
