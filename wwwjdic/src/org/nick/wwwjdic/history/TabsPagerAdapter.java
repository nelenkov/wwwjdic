package org.nick.wwwjdic.history;

import java.util.ArrayList;
import java.util.List;

import org.nick.wwwjdic.R;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;

public class TabsPagerAdapter extends FragmentPagerAdapter implements
        ViewPager.OnPageChangeListener, ActionBar.TabListener {

    private FragmentActivity activity;
    private final ActionBar actionBar;
    private final ViewPager viewPager;
    private final List<HistoryFragmentBase> tabs = new ArrayList<HistoryFragmentBase>();

    public TabsPagerAdapter(FragmentActivity activity, ActionBar actionBar,
            ViewPager pager) {
        super(activity.getSupportFragmentManager());
        this.activity = activity;
        this.actionBar = actionBar;
        this.viewPager = pager;
        this.viewPager.setAdapter(this);
        this.viewPager.setOnPageChangeListener(this);
    }

    public void addTab(ActionBar.Tab tab, HistoryFragmentBase tabFragment) {
        tabFragment.setHasOptionsMenu(true);
        tabs.add(tabFragment);
        actionBar.addTab(tab.setTabListener(this));
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return tabs.size();
    }

    @Override
    public Fragment getItem(int position) {
        return tabs.get(position);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset,
            int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        actionBar.setSelectedNavigationItem(position);
        // TODO -- refreshing kills action bar?
        //        refresh(position);
        setTitle(position);
    }

    @SuppressWarnings("unused")
    private void refresh(int position) {
        HistoryFragmentBase fragment = (HistoryFragmentBase) getItem(position);
        fragment.refresh();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        int position = tab.getPosition();
        viewPager.setCurrentItem(position);
        // TODO -- refreshing kills action bar?
        //        refresh(position);
        setTitle(position);
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    private void setTitle(int tabIdx) {
        if (tabIdx == 0) {
            activity.setTitle(R.string.favorites);
        } else {
            activity.setTitle(R.string.history);
        }
    }

}
