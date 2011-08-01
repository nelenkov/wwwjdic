package org.nick.wwwjdic.history;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.utils.Analytics;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.widget.CursorAdapter;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;

public abstract class HistoryFragmentBase extends ListFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private static final int IMPORT_ITEM_IDX = 0;
	private static final int EXPORT_ITEM_IDX = 1;
	private static final int FILTER_ITEM_IDX = 2;
	private static final int DELETE_ALL_ITEM_IDX = 3;

	private static final String TAG = HistoryFragmentBase.class.getSimpleName();

	private static final int MENU_ITEM_DELETE_ALL = 0;
	private static final int MENU_ITEM_LOOKUP = 1;
	private static final int MENU_ITEM_COPY = 2;
	private static final int MENU_ITEM_DELETE = 3;
	private static final int MENU_ITEM_EXPORT = 4;
	private static final int MENU_ITEM_IMPORT = 5;
	private static final int MENU_ITEM_FILTER = 6;

	protected static final int CONFIRM_DELETE_DIALOG_ID = 0;

	public static final int FILTER_ALL = -1;
	public static final int FILTER_DICT = 0;
	public static final int FILTER_KANJI = 1;
	public static final int FILTER_EXAMPLES = 2;

	private static final byte[] UTF8_BOM = { (byte) 0xef, (byte) 0xbb,
			(byte) 0xbf };

	protected HistoryDbHelper db;

	protected ClipboardManager clipboardManager;

	protected int selectedFilter = -1;

	protected HistoryFragmentBase() {
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		db = HistoryDbHelper.getInstance(getActivity());

		// getActivity().requestWindowFeature(
		// Window.FEATURE_INDETERMINATE_PROGRESS);

		clipboardManager = (ClipboardManager) getActivity().getSystemService(
				Context.CLIPBOARD_SERVICE);

		// setContentView(getContentView());
		getListView().setOnCreateContextMenuListener(this);

		Intent intent = getActivity().getIntent();
		selectedFilter = intent.getIntExtra(Constants.FILTER_TYPE, FILTER_ALL);

		setupAdapter();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(getContentView(), container, false);

		return v;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onStart() {
		super.onStart();

		Analytics.startSession(getActivity());
	}

	@Override
	public void onStop() {
		super.onStop();

		Analytics.endSession(getActivity());
	}

	protected abstract int getContentView();

	protected abstract void setupAdapter();

	protected abstract void resetAdapter(Cursor c);

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		lookupCurrentItem();
	}

	protected abstract void lookupCurrentItem();

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		menu.add(0, MENU_ITEM_IMPORT, IMPORT_ITEM_IDX, R.string.import_items)
				.setIcon(R.drawable.ic_menu_import);
		menu.add(0, MENU_ITEM_EXPORT, EXPORT_ITEM_IDX, R.string.export_items)
				.setIcon(R.drawable.ic_menu_export);
		menu.add(0, MENU_ITEM_FILTER, FILTER_ITEM_IDX, R.string.filter)
				.setIcon(R.drawable.ic_menu_filter);
		menu.add(0, MENU_ITEM_DELETE_ALL, DELETE_ALL_ITEM_IDX,
				R.string.delete_all).setIcon(android.R.drawable.ic_menu_delete);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		ListAdapter adapter = getListAdapter();
		final boolean hasItems = adapter == null ? false
				: adapter.getCount() > 0;
		File backupFile = new File(getImportExportFilename());

		menu.getItem(IMPORT_ITEM_IDX).setEnabled(backupFile.exists());
		menu.getItem(EXPORT_ITEM_IDX).setEnabled(hasItems);
		menu.getItem(DELETE_ALL_ITEM_IDX).setEnabled(hasItems);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_IMPORT:
			importItems();
			break;
		case MENU_ITEM_EXPORT:
			exportItems();
			break;
		case MENU_ITEM_FILTER:
			showFilterDialog();
			break;
		case MENU_ITEM_DELETE_ALL:
			DialogFragment newFragment = ConfirmDeleteDialog.newInstance(this);
			newFragment.show(getFragmentManager(), "dialog");

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void showFilterDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.select_filter_type);
		builder.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();

					}
				});
		builder.setSingleChoiceItems(getFilterTypes(), selectedFilter + 1,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						selectedFilter = item - 1;
						filter();
						dialog.dismiss();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	protected abstract String[] getFilterTypes();

	protected void importItems() {
		String importFile = getImportExportFilename();

		confirmOverwriteAndImport(importFile);

		showAll();
	}

	private void confirmOverwriteAndImport(final String filename) {
		if (getListAdapter().isEmpty()) {
			doImport(filename);
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(R.string.import_and_overwrite)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								doImport(filename);
							}
						})
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		AlertDialog dialog = builder.create();
		dialog.show();

	}

	protected abstract void doImport(String filename);

	protected void exportItems() {
		createWwwjdicDirIfNecessary();

		String exportFile = getImportExportFilename();

		confirmOverwriteAndExport(exportFile);
	}

	protected abstract String getImportExportFilename();

	protected void confirmOverwriteAndExport(final String filename) {
		File file = new File(filename);
		if (!file.exists()) {
			doExport(filename);
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		String message = getResources().getString(R.string.overwrite_file);
		builder.setMessage(String.format(message, filename))
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								doExport(filename);
							}
						})
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	protected abstract void doExport(String filename);

	private void filter() {
		getLoaderManager().restartLoader(0, null, this);

		// Cursor c = filterCursor();
		// CursorAdapter adapter = (CursorAdapter) getListAdapter();
		// adapter.changeCursor(c);
		// // XXX
		// //startManagingCursor(c);
		// refresh();
	}

	protected abstract Cursor filterCursor();

	protected void showAll() {
		selectedFilter = FILTER_ALL;
		filter();
	}

	protected abstract void deleteAll();

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			Log.e(TAG, "bad menuInfo", e);
			return;
		}

		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		if (cursor == null) {
			return;
		}

		// menu.setHeaderTitle(cursor.getString(cursor
		// .getColumnIndex("query_string")));

		menu.add(0, MENU_ITEM_LOOKUP, 0, R.string.look_up);
		menu.add(0, MENU_ITEM_COPY, 1, R.string.copy);
		menu.add(0, MENU_ITEM_DELETE, 2, R.string.delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_LOOKUP:
			lookupCurrentItem();
			return true;
		case MENU_ITEM_COPY:
			copyCurrentItem();
			return true;
		case MENU_ITEM_DELETE: {
			deleteCurrentItem();
			return true;
		}
		}
		return false;
	}

	protected abstract void copyCurrentItem();

	protected abstract void deleteCurrentItem();

	protected Cursor getCursor() {
		CursorAdapter adapter = (CursorAdapter) getListAdapter();
		Cursor c = adapter.getCursor();
		return c;
	}

	@Override
	public void onResume() {
		super.onResume();

		// XXX
		// refresh();
	}

	// XXX
	// @Override
	// protected Dialog onCreateDialog(int id) {
	// Dialog dialog = null;
	//
	// switch (id) {
	// case CONFIRM_DELETE_DIALOG_ID:
	// dialog = createConfirmDeleteDialog();
	// break;
	// default:
	// dialog = null;
	// }
	//
	// return dialog;
	// }

	static class ConfirmDeleteDialog extends DialogFragment {

		private HistoryFragmentBase historyFragment;

		ConfirmDeleteDialog(HistoryFragmentBase historyFragment) {
			this.historyFragment = historyFragment;
		}

		public static ConfirmDeleteDialog newInstance(
				HistoryFragmentBase historyFragment) {
			return new ConfirmDeleteDialog(historyFragment);
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return createConfirmDeleteDialog();
		}

		private Dialog createConfirmDeleteDialog() {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(R.string.delete_all_iteims)
					.setCancelable(false)
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									historyFragment.deleteAll();
								}
							})
					.setNegativeButton(R.string.no,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog dialog = builder.create();

			return dialog;
		}
	}

	// since the adapter is already bound, refresh() has to unfortunately be
	// called on the UI thread
	protected void refresh() {
		Cursor cursor = getCursor();
		cursor.requery();
		CursorAdapter adapter = (CursorAdapter) getListAdapter();
		adapter.notifyDataSetChanged();
	}

	protected void createWwwjdicDirIfNecessary() {
		File sdDir = Environment.getExternalStorageDirectory();
		File wwwjdicDir = new File(sdDir.getAbsolutePath() + "/wwwjdic");
		if (!wwwjdicDir.exists()) {
			wwwjdicDir.mkdir();
		}

		if (!wwwjdicDir.canWrite()) {
			return;
		}
	}

	protected CSVReader openImportFile(String importFile)
			throws FileNotFoundException {
		File file = new File(importFile);
		if (!file.exists()) {
			String message = getResources().getString(R.string.file_not_found);
			Toast.makeText(getActivity(), String.format(message, importFile),
					Toast.LENGTH_SHORT).show();

			return null;
		}

		return new CSVReader(new FileReader(importFile));
	}

	protected void writeBom(File exportFile) throws FileNotFoundException,
			IOException {
		OutputStream out = new FileOutputStream(exportFile);
		out.write(UTF8_BOM);
		out.flush();
		out.close();
	}

	protected void showCopiedToast(String headword) {
		String messageTemplate = getResources().getString(
				R.string.copied_to_clipboard);
		Toast t = Toast.makeText(getActivity(),
				String.format(messageTemplate, headword), Toast.LENGTH_SHORT);
		t.show();
	}

}
