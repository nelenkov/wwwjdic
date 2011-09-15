package org.nick.wwwjdic.hkr;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;

import org.nick.wwwjdic.KanjiEntryDetail;
import org.nick.wwwjdic.KanjiEntryDetailFragment;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.client.WwwjdicClient;
import org.nick.wwwjdic.model.KanjiEntry;
import org.nick.wwwjdic.utils.LoaderBase;
import org.nick.wwwjdic.utils.LoaderResult;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class HkrCandidatesFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<LoaderResult<KanjiEntry>> {

    static class KanjiLoader extends LoaderBase<KanjiEntry> {

        private String kanji;

        private WwwjdicClient client;

        public KanjiLoader(Context context, String kanji) {
            super(context);
            this.kanji = kanji;
            this.client = new WwwjdicClient(context);
        }

        @Override
        protected KanjiEntry load() throws Exception {
            if (kanji == null) {
                return null;
            }

            List<KanjiEntry> entries = client.findKanji(kanji);
            if (entries.isEmpty()) {
                return null;
            }

            return entries.get(0);
        }

        @Override
        protected void releaseResult(LoaderResult<KanjiEntry> result) {
            // just a string, nothing to do
        }

        @Override
        protected boolean isActive(LoaderResult<KanjiEntry> result) {
            return false;
        }
    }

    private static final String TAG = HkrCandidatesFragment.class
            .getSimpleName();

    public static final String EXTRA_HKR_CANDIDATES = "org.nick.wwwjdic.hkrCandidates";

    private static final int MENU_ITEM_DETAILS = 0;
    private static final int MENU_ITEM_COPY = 1;
    private static final int MENU_ITEM_APPEND = 2;

    private String[] candidates;
    protected ClipboardManager clipboard;

    private boolean dualPane;
    private int indexLoading = 0;

    public HkrCandidatesFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle extras = getActivity().getIntent().getExtras();
        candidates = extras.getStringArray(EXTRA_HKR_CANDIDATES);
        setListAdapter(new ArrayAdapter<String>(getActivity(),
                org.nick.wwwjdic.R.layout.text_list_item, candidates));

        getListView().setOnCreateContextMenuListener(this);
        getListView().setTextFilterEnabled(true);

        String message = getResources().getString(R.string.candidates);
        getActivity().setTitle(String.format(message, candidates.length));

        clipboard = (ClipboardManager) getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE);

        View detailsFrame = getActivity().findViewById(R.id.details);
        dualPane = detailsFrame != null
                && detailsFrame.getVisibility() == View.VISIBLE;

        Bundle args = new Bundle();
        if (dualPane) {
            String kanji = (String) getListAdapter().getItem(0);
            args.putString("kanji", kanji);
        }

        getActivity().setProgressBarIndeterminateVisibility(Boolean.TRUE);
        getLoaderManager().initLoader(0, args, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.search_results_fragment, container,
                false);

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // The CursorLoader example doesn't do this, but if we get an update
        // while the UI is
        // destroyed, it will crash. Why is this necessary?
        getLoaderManager().destroyLoader(0);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        String searchKey = candidates[position];
        showDetails(searchKey, position);
    }

    private void showDetails(String searchKey, int index) {
        indexLoading = index;
        Bundle args = new Bundle();
        args.putString("kanji", searchKey);
        Loader<LoaderResult<KanjiEntry>> loader = getLoaderManager()
                .restartLoader(0, args, this);
        if (loader.isStarted()) {
            getActivity().setProgressBarIndeterminateVisibility(Boolean.TRUE);
        }
    }

    private void copy(String kanji) {
        clipboard.setText(kanji);
        String messageTemplate = getResources().getString(
                R.string.copied_to_clipboard);
        Toast t = Toast.makeText(getActivity(),
                String.format(messageTemplate, kanji), Toast.LENGTH_SHORT);
        t.show();
    }

    private void append(String kanji) {
        CharSequence text = clipboard.getText();
        String clipboardText = text == null ? "" : text.toString();
        clipboardText += kanji;
        clipboard.setText(clipboardText);
        String messageTemplate = getResources().getString(
                R.string.appended_to_clipboard);
        Toast t = Toast.makeText(getActivity(),
                String.format(messageTemplate, kanji), Toast.LENGTH_SHORT);
        t.show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        menu.add(0, MENU_ITEM_DETAILS, 0, R.string.kanji_details);
        menu.add(0, MENU_ITEM_COPY, 1, R.string.copy);
        menu.add(0, MENU_ITEM_APPEND, 2, R.string.append);

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        String kanji = candidates[info.position];
        switch (item.getItemId()) {
        case MENU_ITEM_DETAILS:
            showDetails(kanji, info.position);
            return true;
        case MENU_ITEM_COPY:
            copy(kanji);
            return true;
        case MENU_ITEM_APPEND:
            append(kanji);
            return true;
        }
        return false;
    }

    @Override
    public Loader<LoaderResult<KanjiEntry>> onCreateLoader(int id, Bundle args) {
        String kanji = null;
        if (args != null) {
            kanji = args.getString("kanji");
        }

        return new KanjiLoader(getActivity(), kanji);
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult<KanjiEntry>> loader,
            LoaderResult<KanjiEntry> result) {
        getActivity().setProgressBarIndeterminateVisibility(Boolean.FALSE);

        if (result.isFailed()) {
            String message = getResources().getString(R.string.error);
            Exception ex = result.getError();
            if (ex instanceof SocketTimeoutException
                    || ex.getCause() instanceof SocketTimeoutException) {
                message = getResources().getString(
                        R.string.timeout_error_message);
            } else if (ex instanceof SocketException
                    || ex.getCause() instanceof SocketException) {
                message = getResources().getString(
                        R.string.socket_error_message);
            } else {
                message = getResources().getString(
                        R.string.generic_error_message)
                        + "(" + ex.getMessage() + ")";
            }

            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();

            return;
        }

        if (dualPane) {
            if (result.getData() == null) {
                return;
            }
            getListView().setItemChecked(indexLoading, true);

            KanjiEntryDetailFragment details = (KanjiEntryDetailFragment) getFragmentManager()
                    .findFragmentById(R.id.details);
            KanjiEntry entry = result.getData();
            if (details == null || details.getShownIndex() != indexLoading) {
                details = KanjiEntryDetailFragment.newInstance(indexLoading,
                        entry);

                FragmentTransaction ft = getFragmentManager()
                        .beginTransaction();

                ft.replace(R.id.details, details);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commitAllowingStateLoss();
            }
        } else {
            if (result.getData() == null) {
                return;
            }

            Bundle extras = new Bundle();
            extras.putSerializable(KanjiEntryDetail.EXTRA_KANJI_ENTRY,
                    result.getData());

            Intent intent = new Intent(getActivity(), KanjiEntryDetail.class);
            intent.putExtras(extras);

            startActivity(intent);
        }
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult<KanjiEntry>> loader) {
        indexLoading = 0;
    }

}
