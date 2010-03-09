package org.nick.wwwjdic;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ListView;

public class ResultListView extends ListActivity implements ResultView {

	private Handler guiThread;
	private ExecutorService transThread;
	private Future transPending;
	private List<DictionaryEntry> entries;
	private SearchCriteria criteria;

	private ProgressDialog progressDialog;

	public ResultListView() {
		initThreading();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		criteria = (SearchCriteria) getIntent().getSerializableExtra(
				"org.nick.hello.searchCriteria");

		TranslateTask translateTask = new DictionaryTranslateTask(this,
				criteria);
		progressDialog = ProgressDialog.show(this, "",
				"Loading. Please wait...", true);
		transPending = transThread.submit(translateTask);
	}

	@Override
	protected void onDestroy() {
		transThread.shutdownNow();
		super.onDestroy();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(this, DictionaryEntryDetail.class);
		DictionaryEntry entry = entries.get(position);
		intent.putExtra("org.nick.hello.entry", entry);
		startActivity(intent);
	}

	private void initThreading() {
		guiThread = new Handler();
		transThread = Executors.newSingleThreadExecutor();
	}

	public void setResult(final List<?> result) {
		guiThread.post(new Runnable() {
			public void run() {
				entries = (List<DictionaryEntry>) result;
				DictionaryEntryAdapter adapter = new DictionaryEntryAdapter(
						ResultListView.this, entries);
				// setListAdapter(new ArrayAdapter<String>(ResultListView.this,
				// android.R.layout.simple_list_item_1, result));
				setListAdapter(adapter);
				getListView().setTextFilterEnabled(true);
				setTitle(String.format("%d result(s) for '%s'", entries.size(),
						criteria.getQueryString()));
				progressDialog.dismiss();
			}
		});
	}
}
