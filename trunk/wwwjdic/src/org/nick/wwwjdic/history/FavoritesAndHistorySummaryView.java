package org.nick.wwwjdic.history;

import java.util.ArrayList;
import java.util.List;

import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.R;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class FavoritesAndHistorySummaryView extends ListView implements
        OnItemClickListener {

    private static final int FAVORITES_ITEM_IDX = 0;
    private static final int HISTORY_ITEM_IDX = 1;

    private static final int FAVORITES_TAB_IDX = 0;
    private static final int HISTORY_TAB_IDX = 1;

    private Context context;
    private HistoryFavoritesSummaryAdapter adapter;

    private int favoritesFilterType;
    private int historyFilterType;

    public FavoritesAndHistorySummaryView(Context context) {
        super(context);
        init(context);
    }

    public FavoritesAndHistorySummaryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        setOnItemClickListener(this);
        adapter = new HistoryFavoritesSummaryAdapter(context);
        setAdapter(adapter);
    }

    public void setRecentEntries(long numAllFavorites,
            List<String> recentFavorites, long numAllHistoryItems,
            List<String> recentHistory) {
        adapter.setRecentEntries(numAllFavorites, recentFavorites,
                numAllHistoryItems, recentHistory);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        boolean isFavoritesTab = position == FAVORITES_ITEM_IDX;
        int tabIdx = isFavoritesTab ? FAVORITES_TAB_IDX : HISTORY_TAB_IDX;
        int filterType = isFavoritesTab ? favoritesFilterType
                : historyFilterType;

        // examples, only history
        if (getAdapter().getCount() == 1) {
            tabIdx = HISTORY_TAB_IDX;
            filterType = historyFilterType;
        }

        Intent intent = new Intent(context, FavoritesAndHistory.class);
        intent.putExtra(Constants.FAVORITES_HISTORY_SELECTED_TAB_IDX, tabIdx);
        intent.putExtra(Constants.FILTER_TYPE, filterType);
        context.startActivity(intent);
    }

    public int getFavoritesFilterType() {
        return favoritesFilterType;
    }

    public void setFavoritesFilterType(int favoritesFilterType) {
        this.favoritesFilterType = favoritesFilterType;
    }

    public int getHistoryFilterType() {
        return historyFilterType;
    }

    public void setHistoryFilterType(int historyFilterType) {
        this.historyFilterType = historyFilterType;
    }

    static class HistoryFavoritesSummaryAdapter extends BaseAdapter {

        private final Context context;

        private long numAllFavorites;
        private List<String> recentFavorites;
        private long numAllHistoryItems;
        private List<String> recentHistory;

        public HistoryFavoritesSummaryAdapter(Context context) {
            this.context = context;
            this.numAllFavorites = 0;
            this.recentFavorites = new ArrayList<String>();
            this.numAllHistoryItems = 0;
            this.recentHistory = new ArrayList<String>();
        }

        public HistoryFavoritesSummaryAdapter(Context context,
                List<String> recentFavorites, List<String> recentHistory) {
            this.context = context;
            this.numAllFavorites = 0;
            this.recentFavorites = recentFavorites;
            this.numAllHistoryItems = 0;
            this.recentHistory = recentHistory;
        }

        public void setRecentEntries(long numAllFavorites,
                List<String> recentFavorites, long numAllHistoryItems,
                List<String> recentHistory) {
            this.numAllFavorites = numAllFavorites;
            this.recentFavorites = recentFavorites;
            this.numAllHistoryItems = numAllHistoryItems;
            this.recentHistory = recentHistory;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (recentFavorites == null) {
                return 1;
            }

            return 2;
        }

        @Override
        public Object getItem(int position) {
            switch (position) {
            case 0:
                if (recentFavorites == null) {
                    return "history";
                }
                return "favorites";
            case 1:
                return "history";
            default:
                throw new IllegalArgumentException("Invalid position: "
                        + position);
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            boolean isFavoritesEntry = position == 0;
            List<String> recent = isFavoritesEntry ? recentFavorites
                    : recentHistory;
            long numAllEntries = isFavoritesEntry ? numAllFavorites
                    : numAllHistoryItems;
            if (recentFavorites == null) {
                isFavoritesEntry = false;
                recent = recentHistory;
                numAllEntries = numAllHistoryItems;
            }

            if (convertView == null) {
                convertView = new SummaryView(context);
            }

            ((SummaryView) convertView).populate(numAllEntries, recent,
                    isFavoritesEntry);

            return convertView;
        }

        static class SummaryView extends LinearLayout {

            private TextView summary;
            private TextView itemList;

            SummaryView(Context context) {
                super(context);

                LayoutInflater inflater = LayoutInflater.from(context);
                inflater.inflate(R.layout.favorites_history_summary_item, this);

                summary = (TextView) findViewById(R.id.summary);
                itemList = (TextView) findViewById(R.id.item_list);
            }

            void populate(long numAllEntries, List<String> recentEntries,
                    boolean isFavoritesItem) {
                if (isFavoritesItem) {
                    String message = getResources().getString(
                            R.string.favorties_summary);
                    summary.setText(String.format(message, numAllEntries));
                } else {
                    String message = getResources().getString(
                            R.string.history_summary);
                    summary.setText(String.format(message, numAllEntries));
                }

                String itemsAsStr = TextUtils.join(", ", recentEntries);
                if (numAllEntries > recentEntries.size()) {
                    itemsAsStr = itemsAsStr + "...";
                }
                itemList.setText(itemsAsStr);
            }
        }

    }

}
