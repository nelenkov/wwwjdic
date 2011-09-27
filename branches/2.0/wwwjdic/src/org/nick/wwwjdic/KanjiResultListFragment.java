package org.nick.wwwjdic;

import java.util.List;

import org.nick.wwwjdic.model.KanjiEntry;
import org.nick.wwwjdic.model.SearchCriteria;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class KanjiResultListFragment extends ResultListFragmentBase<KanjiEntry> {

    private static final String TAG = KanjiResultListFragment.class
            .getSimpleName();

    private static final int MENU_ITEM_DETAILS = 0;
    private static final int MENU_ITEM_COPY = 1;
    private static final int MENU_ITEM_ADD_TO_FAVORITES = 2;
    private static final int MENU_ITEM_STROKE_ORDER = 3;
    private static final int MENU_ITEM_COMPOUNDS = 4;

    private List<KanjiEntry> entries;

    private boolean dualPane;
    private int currentCheckPosition = 0;

    public KanjiResultListFragment() {
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

        extractSearchCriteria();

        SearchTask<KanjiEntry> searchTask = new KanjiSearchTask(
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
        KanjiEntry entry = entries.get(position);
        showDetails(entry, position);
    }

    private void showDetails(KanjiEntry entry, int index) {
        if (dualPane) {
            getListView().setItemChecked(index, true);

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
            setFavoriteId(intent, entry);

            startActivity(intent);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        menu.add(0, MENU_ITEM_DETAILS, 0, R.string.details);
        menu.add(0, MENU_ITEM_COPY, 1, R.string.copy);
        menu.add(0, MENU_ITEM_ADD_TO_FAVORITES, 2, R.string.add_to_favorites);
        menu.add(0, MENU_ITEM_STROKE_ORDER, 3, R.string.stroke_order);
        menu.add(0, MENU_ITEM_COMPOUNDS, 4, R.string.compounds);
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

        KanjiEntry entry = entries.get(info.position);
        switch (item.getItemId()) {
        case MENU_ITEM_DETAILS:
            showDetails(entry, info.position);
            return true;
        case MENU_ITEM_COPY:
            copy(entry);
            return true;
        case MENU_ITEM_ADD_TO_FAVORITES:
            addToFavorites(entry);
            return true;
        case MENU_ITEM_STROKE_ORDER:
            Activities.showStrokeOrder(getActivity(), entry);
            return true;
        case MENU_ITEM_COMPOUNDS:
            showCompounds(entry);
            return true;
        }
        return false;
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
                entries = (List<KanjiEntry>) result;
                KanjiEntryAdapter adapter = new KanjiEntryAdapter(
                        getActivity(), entries);
                setListAdapter(adapter);
                getListView().setTextFilterEnabled(true);
                setTitleAndCurrentItem();
                dismissProgressDialog();
            }
        });
    }

    private void setTitleAndCurrentItem() {
        String message = getResources().getString(R.string.results_for,
                entries.size(), criteria.getQueryString());
        getActivity().setTitle(message);
        if (dualPane) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            showDetails(entries.get(currentCheckPosition), currentCheckPosition);
        }
    }

}
