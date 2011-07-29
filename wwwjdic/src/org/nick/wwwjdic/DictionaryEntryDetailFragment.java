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
import android.view.LayoutInflater;
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

	public static DictionaryEntryDetailFragment newInstance(int index,
			DictionaryEntry entry) {
		DictionaryEntryDetailFragment f = new DictionaryEntryDetailFragment();

		// Supply index input as an argument.
		Bundle args = new Bundle();
		args.putInt("index", index);
		args.putSerializable(Constants.ENTRY_KEY, entry);
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

		// entry = (DictionaryEntry) getActivity().getIntent()
		// .getSerializableExtra(Constants.ENTRY_KEY);
		entry = (DictionaryEntry) getArguments().getSerializable(
				Constants.ENTRY_KEY);
		wwwjdicEntry = entry;
		isFavorite = getActivity().getIntent().getBooleanExtra(
				Constants.IS_FAVORITE, false);

		String message = getResources().getString(R.string.details_for);
		getActivity().setTitle(String.format(message, entry.getWord()));

		View v = getView();
		LinearLayout wordReadingLayout = (LinearLayout) v
				.findViewById(R.id.word_reading_layout);

		entryView = (TextView) v.findViewById(R.id.wordText);
		entryView.setText(entry.getWord());
		entryView.setOnLongClickListener(this);

		if (entry.getReading() != null) {
			TextView readingView = new TextView(getActivity(), null,
					R.style.dict_detail_reading);
			readingView.setText(entry.getReading());
			wordReadingLayout.addView(readingView);
		}

		translationsLayout = (LinearLayout) v
				.findViewById(R.id.translations_layout);

		for (String meaning : entry.getMeanings()) {
			final Pair<LinearLayout, TextView> translationViews = createMeaningTextView(
					getActivity(), meaning);
			Matcher m = CROSS_REF_PATTERN.matcher(meaning);
			if (m.matches()) {
				Intent intent = createCrossRefIntent(m.group(1));
				int start = m.start(1);
				int end = m.end(1);
				makeClickable(translationViews.getSecond(), start, end, intent);
			}
			translationsLayout.addView(translationViews.getFirst());
		}

		starCb = (CheckBox) v.findViewById(R.id.star_word);
		starCb.setOnCheckedChangeListener(null);
		starCb.setChecked(isFavorite);
		starCb.setOnCheckedChangeListener(this);

		exampleSearchButton = (Button) v.findViewById(R.id.examples_button);
		exampleSearchButton.setOnClickListener(this);

		exampleSearchKey = DictUtils.extractSearchKey(wwwjdicEntry);
		disableExampleSearchIfSingleKanji();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.entry_details, container, false);

		return v;
	}

	@Override
	public void onDestroy() {
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
		Intent intent = new Intent(getActivity(),
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
			Intent intent = new Intent(getActivity(),
					ExamplesResultListView.class);
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
