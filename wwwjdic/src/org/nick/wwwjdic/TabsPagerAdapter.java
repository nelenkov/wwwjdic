package org.nick.wwwjdic;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.nick.wwwjdic.history.HistoryFragmentBase;

import android.support.v4.app.ActionBar;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

public class TabsPagerAdapter extends FragmentPagerAdapter implements
        ViewPager.OnPageChangeListener, ActionBar.TabListener {

    private final FragmentActivity activity;
    private final ActionBar actionBar;
    private final ViewPager viewPager;
    private final List<HistoryFragmentBase> tabs = new ArrayList<HistoryFragmentBase>();

    private WeakReference<HistoryFragmentBase> lastFragment = null;

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
        tabFragment.setHasOptionsMenu(false);
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
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        viewPager.setCurrentItem(tab.getPosition());
        for (int i = 0; i < tabs.size(); i++) {
            if (i != tab.getPosition()) {
                //                tabs.get(i).setHasOptionsMenu(false);
            } else {
                tabs.get(i).setHasOptionsMenu(true);
                activity.invalidateOptionsMenu();
            }
        }
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    // TODO
    // workaround for https://github.com/JakeWharton/ActionBarSherlock/issues/56
    // Cf. http://code.google.com/p/android/issues/detail?id=20065
    @Override
    public void onItemSelected(int position, Object object) {
        super.onItemSelected(position, object);
        if ((lastFragment != null) && (lastFragment.get() != null)) {
            lastFragment.get().setSelected(false);
        }
        HistoryFragmentBase fragment = (HistoryFragmentBase) object;
        fragment.setSelected(true);
        lastFragment = new WeakReference<HistoryFragmentBase>(fragment);
    }
}
