package org.nick.wwwjdic.history;

import java.util.HashMap;

import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.R;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.Window;
import android.widget.TabHost;

public class FavoritesAndHistory extends FragmentActivity {

    private TabHost tabHost;
    private TabManager tabManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.favorites_history);

        tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();

        tabManager = new TabManager(this, tabHost, R.id.realtabcontent);

        tabManager.addTab(
                tabHost.newTabSpec("favorites")
                        .setIndicator(
                                getResources().getString(R.string.favorites),
                                getResources().getDrawable(
                                        R.drawable.ic_tab_favorites)),
                FavoritesFragment.class, null);
        tabManager.addTab(
                tabHost.newTabSpec("history").setIndicator(
                        getResources().getString(R.string.search_history),
                        getResources().getDrawable(R.drawable.ic_tab_history)),
                SearchHistoryFragment.class, null);

        if (savedInstanceState != null) {
            tabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }

        //        Intent intent = getIntent();
        //        int filterType = intent.getIntExtra(Constants.FILTER_TYPE,
        //                HistoryBase.FILTER_ALL);
        //        int tabIdx = intent.getIntExtra(
        //                Constants.FAVORITES_HISTORY_SELECTED_TAB_IDX, 0);
        //
        //        setupTabs(tabIdx, filterType);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tab", tabHost.getCurrentTabTag());
    }

    private void setupTabs(int tabIdx, int filterType) {
        //        TabHost tabHost = getTabHost();

        Intent favoritesIntent = new Intent(this, Favorites.class);
        if (tabIdx == 0) {
            favoritesIntent.putExtra(Constants.FILTER_TYPE, filterType);
        }
        tabHost.addTab(tabHost
                .newTabSpec("favorites")
                .setIndicator(getResources().getString(R.string.favorites),
                        getResources().getDrawable(R.drawable.ic_tab_favorites))
                .setContent(favoritesIntent));

        Intent historyIntent = new Intent(this, SearchHistory.class);
        if (tabIdx == 1) {
            historyIntent.putExtra(Constants.FILTER_TYPE, filterType);
        }
        tabHost.addTab(tabHost
                .newTabSpec("history")
                .setIndicator(
                        getResources().getString(R.string.search_history),
                        getResources().getDrawable(R.drawable.ic_tab_history))
                .setContent(historyIntent));

        //        tabHost.setOnTabChangedListener(this);

        tabHost.setCurrentTab(tabIdx);
    }

    //    @Override
    //    public void onTabChanged(String tabId) {
    //        // XXX
    //        //        HistoryBase history = (HistoryBase) getLocalActivityManager()
    //        //                .getActivity(tabId);
    //        //        if (history != null) {
    //        //            history.refresh();
    //        //        }
    //    }


    @Override
    protected void onStart() {
        super.onStart();
    }


    public static class TabManager implements TabHost.OnTabChangeListener {
        private final FragmentActivity mActivity;
        private final TabHost mTabHost;
        private final int mContainerId;
        private final HashMap<String, TabInfo> mTabs = new HashMap<String, TabInfo>();
        TabInfo mLastTab;

        static final class TabInfo {
            private final String tag;
            private final Class<?> clss;
            private final Bundle args;
            private Fragment fragment;

            TabInfo(String _tag, Class<?> _class, Bundle _args) {
                tag = _tag;
                clss = _class;
                args = _args;
            }
        }

        static class DummyTabFactory implements TabHost.TabContentFactory {
            private final Context mContext;

            public DummyTabFactory(Context context) {
                mContext = context;
            }

            @Override
            public View createTabContent(String tag) {
                View v = new View(mContext);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        }

        public TabManager(FragmentActivity activity, TabHost tabHost,
                int containerId) {
            mActivity = activity;
            mTabHost = tabHost;
            mContainerId = containerId;
            mTabHost.setOnTabChangedListener(this);
        }

        public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
            tabSpec.setContent(new DummyTabFactory(mActivity));
            String tag = tabSpec.getTag();

            TabInfo info = new TabInfo(tag, clss, args);

            // Check to see if we already have a fragment for this tab, probably
            // from a previously saved state.  If so, deactivate it, because our
            // initial state is that a tab isn't shown.
            info.fragment = mActivity.getSupportFragmentManager()
                    .findFragmentByTag(tag);
            if (info.fragment != null && !info.fragment.isDetached()) {
                FragmentTransaction ft = mActivity.getSupportFragmentManager()
                        .beginTransaction();
                ft.detach(info.fragment);
                ft.commit();
            }

            mTabs.put(tag, info);
            mTabHost.addTab(tabSpec);
        }

        @Override
        public void onTabChanged(String tabId) {
            TabInfo newTab = mTabs.get(tabId);
            if (mLastTab != newTab) {
                FragmentTransaction ft = mActivity.getSupportFragmentManager()
                        .beginTransaction();
                if (mLastTab != null) {
                    if (mLastTab.fragment != null) {
                        ft.detach(mLastTab.fragment);
                    }
                }
                if (newTab != null) {
                    if (newTab.fragment == null) {
                        newTab.fragment = Fragment.instantiate(mActivity,
                                newTab.clss.getName(), newTab.args);
                        ft.add(mContainerId, newTab.fragment, newTab.tag);
                    } else {
                        ft.attach(newTab.fragment);
                    }
                }

                mLastTab = newTab;
                ft.commit();
                mActivity.getSupportFragmentManager()
                        .executePendingTransactions();
            }
        }
    }

}
