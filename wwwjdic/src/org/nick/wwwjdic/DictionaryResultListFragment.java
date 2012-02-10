package org.nick.wwwjdic;

import java.util.List;

import org.nick.wwwjdic.history.HistoryUtils;
import org.nick.wwwjdic.model.DictionaryEntry;
import org.nick.wwwjdic.model.SearchCriteria;
import org.nick.wwwjdic.utils.Analytics;
import org.nick.wwwjdic.utils.DictUtils;
import org.nick.wwwjdic.utils.StringUtils;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class DictionaryResultListFragment extends
        ResultListFragmentBase<DictionaryEntry> {

    private static final int NUM_EXAMPLE_RESULTS = 20;

    private static final String TAG = DictionaryResultListFragment.class
            .getSimpleName();

    private static final int MENU_ITEM_DETAILS = 0;
    private static final int MENU_ITEM_COPY = 1;
    private static final int MENU_ITEM_LOOKUP_KANJI = 2;
    private static final int MENU_ITEM_ADD_TO_FAVORITES = 3;
    private static final int MENU_ITEM_EXAMPLES = 4;

    private List<DictionaryEntry> entries;

    private boolean dualPane;
    private int currentCheckPosition = 0;

    public DictionaryResultListFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnCreateContextMenuListener(this);

        View detailsFrame = getActivity().findViewById(R.id.details);
        dualPane = detailsFrame != null
                && detailsFrame.getVisibility() == View.VISIBLE;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.search_results_fragment, container,
                false);

        return v;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        DictionaryEntry entry = entries.get(position);

        showDetails(entry, position);
    }

    private void showDetails(DictionaryEntry entry, int index) {
        if (dualPane) {
            if (!isVisible()) {
                return;
            }

            getListView().setItemChecked(index, true);

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
            setFavoriteId(intent, entry);

            startActivity(intent);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        menu.add(0, MENU_ITEM_DETAILS, 0, R.string.details);
        menu.add(0, MENU_ITEM_COPY, 1, R.string.copy);
        menu.add(0, MENU_ITEM_LOOKUP_KANJI, 2, R.string.lookup_kanji);
        menu.add(0, MENU_ITEM_ADD_TO_FAVORITES, 3, R.string.add_to_favorites);
        menu.add(0, MENU_ITEM_EXAMPLES, 4, R.string.examples);
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

        DictionaryEntry entry = entries.get(info.position);
        switch (item.getItemId()) {
        case MENU_ITEM_DETAILS:
            showDetails(entry, info.position);
            return true;
        case MENU_ITEM_COPY:
            copy(entry);
            return true;
        case MENU_ITEM_LOOKUP_KANJI:
            Activities.lookupKanji(getActivity(), db, entry.getHeadword());
            return true;
        case MENU_ITEM_ADD_TO_FAVORITES:
            addToFavorites(entry);
            return true;
        case MENU_ITEM_EXAMPLES:
            searchExamples(entry);
            return true;
        }
        return false;
    }

    private void searchExamples(DictionaryEntry entry) {
        SearchCriteria criteria = SearchCriteria.createForExampleSearch(
                DictUtils.extractSearchKey(entry), false, NUM_EXAMPLE_RESULTS);

        Intent intent = new Intent(getActivity(), ExamplesResultList.class);
        intent.putExtra(Wwwjdic.EXTRA_CRITERIA, criteria);

        if (!StringUtils.isEmpty(criteria.getQueryString())) {
            db.addSearchCriteria(criteria);
        }

        Analytics.event("exampleSearch", getActivity());

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
                getListView().setTextFilterEnabled(true);
                setTitleAndCurrentItem();
                dismissProgressDialog();
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
            if (!entries.isEmpty()) {
                showDetails(entries.get(currentCheckPosition),
                        currentCheckPosition);
            }
        }
    }
}
