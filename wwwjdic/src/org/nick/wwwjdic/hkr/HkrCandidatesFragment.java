package org.nick.wwwjdic.hkr;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;

import org.nick.wwwjdic.R;
import org.nick.wwwjdic.client.WwwjdicClient;
import org.nick.wwwjdic.model.KanjiEntry;
import org.nick.wwwjdic.utils.LoaderBase;
import org.nick.wwwjdic.utils.LoaderResult;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;

@SuppressWarnings("deprecation")
public class HkrCandidatesFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<LoaderResult<KanjiEntry>> {

    interface HkrCandidateSelectedListener {
        void onHkrCandidateSelected(KanjiEntry entry, int position);
    }

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

    private static final String INDEX_KEY = "index";

    public static final String EXTRA_HKR_CANDIDATES = "org.nick.wwwjdic.hkrCandidates";

    private static final int MENU_ITEM_DETAILS = 0;
    private static final int MENU_ITEM_COPY = 1;
    private static final int MENU_ITEM_APPEND = 2;

    protected ClipboardManager clipboard;

    private String[] candidates;
    private int index = 0;

    private HkrCandidateSelectedListener candidateSelectedListener;

    public HkrCandidatesFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle extras = getActivity().getIntent().getExtras();
        candidates = extras.getStringArray(EXTRA_HKR_CANDIDATES);
        setListAdapter(new ArrayAdapter<String>(getActivity(),
                org.nick.wwwjdic.R.layout.text_list_item, R.id.item_text,
                candidates));

        getListView().setOnCreateContextMenuListener(this);
        getListView().setTextFilterEnabled(true);

        getActivity().setTitle(
                getResources()
                        .getString(R.string.candidates, candidates.length));

        clipboard = (ClipboardManager) getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE);

        if (savedInstanceState != null) {
            index = savedInstanceState.getInt(INDEX_KEY, 0);
        }

        // just init don't try to load
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(INDEX_KEY, index);
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            candidateSelectedListener = (HkrCandidateSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnArticleSelectedListener");
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        getListView().setItemChecked(index, true);
        loadDetails(candidates[position], position);
    }

    private void loadDetails(String searchKey, int position) {
        index = position;
        Bundle args = new Bundle();
        args.putString("kanji", searchKey);
        getSherlockActivity()
                .setSupportProgressBarIndeterminateVisibility(true);
        getLoaderManager().restartLoader(0, args, this);
    }

    private void copy(String kanji) {
        clipboard.setText(kanji);
        String messageTemplate = getResources().getString(
                R.string.copied_to_clipboard);
        Toast.makeText(getActivity(), String.format(messageTemplate, kanji),
                Toast.LENGTH_SHORT).show();
    }

    private void append(String kanji) {
        CharSequence text = clipboard.getText();
        String clipboardText = text == null ? "" : text.toString();
        clipboardText += kanji;
        clipboard.setText(clipboardText);
        String messageTemplate = getResources().getString(
                R.string.appended_to_clipboard);
        Toast.makeText(getActivity(), String.format(messageTemplate, kanji),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        menu.add(0, MENU_ITEM_DETAILS, 0, R.string.kanji_details);
        menu.add(0, MENU_ITEM_COPY, 1, R.string.copy);
        menu.add(0, MENU_ITEM_APPEND, 2, R.string.append);

    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
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
            loadDetails(kanji, info.position);
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
        getSherlockActivity().setSupportProgressBarIndeterminateVisibility(
                false);

        if (result.isFailed()) {
            String message = selectErrorMessage(result.getError());
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();

            return;
        }

        if (result.getData() == null) {
            return;
        }

        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        getListView().setItemChecked(index, true);

        candidateSelectedListener.onHkrCandidateSelected(result.getData(),
                index);
    }

    private String selectErrorMessage(Exception ex) {
        String message = getResources().getString(R.string.error);
        if (ex instanceof SocketTimeoutException
                || ex.getCause() instanceof SocketTimeoutException) {
            message = getResources().getString(R.string.timeout_error_message);
        } else if (ex instanceof SocketException
                || ex.getCause() instanceof SocketException) {
            message = getResources().getString(R.string.socket_error_message);
        } else {
            message = getResources().getString(R.string.generic_error_message)
                    + "(" + ex.getMessage() + ")";
        }
        return message;
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult<KanjiEntry>> loader) {
    }

    public void loadCurrentKanji() {
        if (candidates == null || index >= candidates.length) {
            return;
        }

        loadDetails(candidates[index], index);
    }

}
