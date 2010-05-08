package org.nick.wwwjdic;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class KanjiResultListView extends ResultListViewBase {

	private List<KanjiEntry> entries;

	public KanjiResultListView() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		extractSearchCriteria();

		TranslateTask translateTask = new KanjiTranslateTask(getWwwjdicUrl(),
				getHttpTimeoutSeconds(), this, criteria);
		submitTranslateTask(translateTask);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(this, KanjiEntryDetail.class);
		KanjiEntry entry = entries.get(position);
		intent.putExtra(Constants.KANJI_ENTRY_KEY, entry);
		startActivity(intent);
	}

	public void setResult(final List<?> result) {
		guiThread.post(new Runnable() {
			@SuppressWarnings("unchecked")
			public void run() {
				entries = (List<KanjiEntry>) result;
				KanjiEntryAdapter adapter = new KanjiEntryAdapter(
						KanjiResultListView.this, entries);
				setListAdapter(adapter);
				getListView().setTextFilterEnabled(true);
				setTitle(String.format("%d result(s) for '%s'", entries.size(),
						criteria.getQueryString()));
				progressDialog.dismiss();
			}
		});
	}

}
