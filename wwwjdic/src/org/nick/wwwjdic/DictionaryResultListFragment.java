package org.nick.wwwjdic;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.app.FragmentTransaction;
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

import org.nick.wwwjdic.history.HistoryUtils;
import org.nick.wwwjdic.model.DictionaryEntry;
import org.nick.wwwjdic.model.SearchCriteria;
import org.nick.wwwjdic.utils.DictUtils;
import org.nick.wwwjdic.utils.StringUtils;

import java.util.List;

public class DictionaryResultListFragment extends
        ResultListFragmentBase<DictionaryEntry> implements
        OnItemLongClickListener {

    private static final int NUM_EXAMPLE_RESULTS = 20;

    private List<DictionaryEntry> entries;

    private ActionMode currentActionMode;

    public DictionaryResultListFragment() {
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

        Intent intent = getActivity().getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            String dictionary = WwwjdicPreferences
                    .getDefaultDictionary(getActivity());
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
                    getActivity(), SearchSuggestionProvider.AUTHORITY,
                    SearchSuggestionProvider.MODE);
            suggestions.saveRecentQuery(query, null);
            criteria = SearchCriteria.createForDictionary(query, false, false,
                    false, dictionary);
        } else {
            extractSearchCriteria();
        }
        SearchTask<DictionaryEntry> searchTask = new DictionarySearchTask(
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

        DictionaryEntry entry = entries.get(position);
        showDetails(entry, position);
    }

    private void showDetails(DictionaryEntry entry, int index) {
        if (dualPane) {
            if (!isVisible()) {
                return;
            }

            getListView().setItemChecked(index, true);
            currentCheckPosition = index;

            DictionaryEntryDetailFragment details = (DictionaryEntryDetailFragment) getFragmentManager()
                    .findFragmentById(R.id.details);
            if (details == null || details.getShownIndex() != index) {
                details = DictionaryEntryDetailFragment.newInstance(index,
                        entry);

                FragmentTransaction ft = getFragmentManager()
                        .beginTransaction();
                ft.replace(R.id.details, details);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }

        } else {
            Intent intent = new Intent(getActivity(),
                    DictionaryEntryDetail.class);
            intent.putExtra(DictionaryEntryDetail.EXTRA_DICTIONARY_ENTRY, entry);
            intent.putExtra(DetailActivity.EXTRA_DETAILS_PARENT,
                    DetailActivity.Parent.DICT_CANDIDATES.ordinal());
            setFavoriteId(intent, entry);

            startActivity(intent);
        }
    }

    private void searchExamples(DictionaryEntry entry) {
        SearchCriteria criteria = SearchCriteria.createForExampleSearch(
                DictUtils.extractSearchKey(entry), false, NUM_EXAMPLE_RESULTS);

        Intent intent = new Intent(getActivity(), ExamplesResultList.class);
        intent.putExtra(Wwwjdic.EXTRA_CRITERIA, criteria);

        if (!StringUtils.isEmpty(criteria.getQueryString())) {
            db.addSearchCriteria(criteria);
        }

        startActivity(intent);
    }

    public void setResult(final List<DictionaryEntry> result) {
        guiThread.post(new Runnable() {
            public void run() {
                if (getView() == null) {
                    return;
                }

                entries = (List<DictionaryEntry>) result;
                DictionaryEntryAdapter adapter = new DictionaryEntryAdapter(
                        getActivity(), entries);
                setListAdapter(adapter);
                setTitleAndCurrentItem();
                dismissProgress();
            }
        });
    }

    private void setTitleAndCurrentItem() {
        String message = getResources().getString(
                R.string.results_for_in_dict,
                entries.size(),
                criteria.getQueryString(),
                HistoryUtils.lookupDictionaryName(criteria.getDictionaryCode(),
                        getActivity()));
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
            MenuInflater inflater = getActivity()
                    .getMenuInflater();
            inflater.inflate(R.menu.dict_list_context, menu);
            return true;
        }

        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        public boolean onActionItemClicked(ActionMode actionMode,
                MenuItem menuItem) {

            DictionaryEntry entry = entries.get(position);
            if (menuItem.getItemId() == R.id.menu_context_dict_list_copy) {
                copy(entry);
                actionMode.finish();
                return true;
            } else if (menuItem.getItemId() == R.id.menu_context_dict_list_lookup_kanji) {
                Activities.lookupKanji(getActivity(), db, entry.getHeadword());
                actionMode.finish();
                return true;
            } else if (menuItem.getItemId() == R.id.menu_context_dict_list_favorite) {
                addToFavorites(entry);
                actionMode.finish();
                return true;
            } else if (menuItem.getItemId() == R.id.menu_context_dict_list_examples) {
                searchExamples(entry);
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
