package org.nick.wwwjdic.history;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class TabsPagerAdapter extends FragmentPagerAdapter implements
        ViewPager.OnPageChangeListener, TabLayout.OnTabSelectedListener {

    private static final String TAG = TabsPagerAdapter.class.getSimpleName();

    private final ViewPager viewPager;
    private final List<HistoryFragmentBase> tabs = new ArrayList<>();

    public TabsPagerAdapter(AppCompatActivity activity, ViewPager pager) {
        super(activity.getSupportFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.viewPager = pager;
        this.viewPager.setAdapter(this);
        this.viewPager.addOnPageChangeListener(this);
    }

    public void addTab(HistoryFragmentBase tabFragment) {
        tabFragment.setHasOptionsMenu(true);
        tabs.add(tabFragment);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return tabs.size();
    }

    @Override
    public @NonNull Fragment getItem(int position) {
        return tabs.get(position);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset,
            int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        //actionBar.setSelectedNavigationItem(position);
        // TODO -- refreshing kills action bar?
        // refresh(position);
        //setTitle(position);

        // for ABS#240
        // https://github.com/JakeWharton/ActionBarSherlock/issues/240
        //selectInSpinnerIfPresent(position, true);
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
    public void onTabSelected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }
}
