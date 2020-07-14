package org.nick.wwwjdic;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.app.FragmentTransaction;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.nick.wwwjdic.model.KanjiEntry;
import org.nick.wwwjdic.model.SearchCriteria;

import java.util.List;

public class KanjiResultListFragment extends ResultListFragmentBase<KanjiEntry>
        implements OnItemLongClickListener {

    private static final String TAG = KanjiResultListFragment.class
            .getSimpleName();

    private List<KanjiEntry> entries;

    private ActionMode currentActionMode;

    public KanjiResultListFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnItemLongClickListener(this);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        View detailsFrame = getActivity().findViewById(R.id.details);
        dualPane = detailsFrame != null
                && detailsFrame.getVisibility() == View.VISIBLE;
        if (detailsFrame != null) {
            detailsFrame.setVisibility(View.GONE);
        }

        if (entries != null) {
            // we are being re-created after rotation, use existing data
            setTitleAndCurrentItem();

            return;
        }

        extractSearchCriteria();

        SearchTask<KanjiEntry> searchTask = new KanjiSearchTask(
                getWwwjdicUrl(), getHttpTimeoutSeconds(), this, criteria);
        submitSearchTask(searchTask);
    }

    @Override
    public void onResume() {
        super.onResume();

        checkOrClearCurrentItem();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.search_results_fragment, container,
                false);
        progressSpinner = (ProgressBar) v.findViewById(R.id.progress_spinner);
        emptyText = (TextView) v.findViewById(android.R.id.empty);

        return v;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        getListView().setItemChecked(position, false);

        KanjiEntry entry = entries.get(position);
        showDetails(entry, position);
    }

    private void showDetails(KanjiEntry entry, int index) {
        if (dualPane) {
            getListView().setItemChecked(index, true);
            currentCheckPosition = index;

            KanjiEntryDetailFragment details = (KanjiEntryDetailFragment) getFragmentManager()
                    .findFragmentById(R.id.details);
            if (details == null || details.getShownIndex() != index) {
                details = KanjiEntryDetailFragment.newInstance(index, entry);

                FragmentTransaction ft = getFragmentManager()
                        .beginTransaction();
                ft.replace(R.id.details, details);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }

        } else {
            Intent intent = new Intent(getActivity(), KanjiEntryDetail.class);
            intent.putExtra(KanjiEntryDetail.EXTRA_KANJI_ENTRY, entry);
            intent.putExtra(DetailActivity.EXTRA_DETAILS_PARENT,
                    DetailActivity.Parent.EXAMPLE_CANDIDATES.ordinal());
            setFavoriteId(intent, entry);

            startActivity(intent);
        }
    }

    private void showCompounds(KanjiEntry entry) {
        String dictionary = getApp().getCurrentDictionary();
        Log.d(TAG, String.format(
                "Will look for compounds in dictionary: %s(%s)", getApp()
                        .getCurrentDictionaryName(), dictionary));
        SearchCriteria criteria = SearchCriteria.createForKanjiCompounds(
                entry.getKanji(),
                SearchCriteria.KANJI_COMPOUND_SEARCH_TYPE_ANY, false,
                dictionary);
        Intent intent = new Intent(getActivity(), DictionaryResultList.class);
        intent.putExtra(Wwwjdic.EXTRA_CRITERIA, criteria);

        startActivity(intent);
    }

    public void setResult(final List<KanjiEntry> result) {
        guiThread.post(new Runnable() {
            public void run() {
                if (getView() == null) {
                    return;
                }

                entries = (List<KanjiEntry>) result;
                KanjiEntryAdapter adapter = new KanjiEntryAdapter(
                        getActivity(), entries);
                setListAdapter(adapter);
                setTitleAndCurrentItem();
                dismissProgress();
            }
        });
    }

    private void setTitleAndCurrentItem() {
        String message = getResources().getString(R.string.results_for,
                entries.size(), criteria.getQueryString());
        getActivity().setTitle(message);
        if (dualPane) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            View detailsFrame = getActivity().findViewById(R.id.details);
            detailsFrame.setVisibility(View.VISIBLE);
            if (!entries.isEmpty()) {
                showDetails(entries.get(currentCheckPosition),
                        currentCheckPosition);
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        if (currentActionMode != null) {
            return false;
        }

        currentActionMode = getActivity().startActionMode(
                new ContextCallback(position));
        getListView().setItemChecked(position, true);

        return true;
    }

    @SuppressLint("NewApi")
    class ContextCallback implements ActionMode.Callback {

        private int position;

        ContextCallback(int position) {
            this.position = position;
        }

        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.kanji_list_context, menu);
            return true;
        }

        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        public boolean onActionItemClicked(ActionMode actionMode,
                MenuItem menuItem) {

            KanjiEntry entry = entries.get(position);
            if (menuItem.getItemId() == R.id.menu_context_kanji_list_copy) {
                copy(entry);
                actionMode.finish();
                return true;
            } else if (menuItem.getItemId() == R.id.menu_context_kanji_list_favorite) {
                addToFavorites(entry);
                actionMode.finish();
                return true;
            } else if (menuItem.getItemId() == R.id.menu_context_kanji_list_stroke_order) {
                Activities.showStrokeOrder(getActivity(), entry);
                actionMode.finish();
                return true;
            } else if (menuItem.getItemId() == R.id.menu_context_kanji_list_compounds) {
                showCompounds(entry);
                actionMode.finish();
                return true;
            }

            return false;
        }

        public void onDestroyActionMode(ActionMode actionMode) {
            getListView().setItemChecked(position, false);
            currentActionMode = null;
        }
    };

}
