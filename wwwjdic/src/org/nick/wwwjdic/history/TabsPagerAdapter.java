package org.nick.wwwjdic.history;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;

import org.nick.wwwjdic.R;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class TabsPagerAdapter extends FragmentPagerAdapter implements
        ViewPager.OnPageChangeListener, ActionBar.TabListener {

    private static final String TAG = TabsPagerAdapter.class.getSimpleName();

    private Activity activity;
    private final ActionBar actionBar;
    private final ViewPager viewPager;
    private final List<HistoryFragmentBase> tabs = new ArrayList<HistoryFragmentBase>();

    public TabsPagerAdapter(Activity activity, ActionBar actionBar,
                            ViewPager pager) {
        super(activity.getFragmentManager());
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
        // refresh(position);
        setTitle(position);

        // for ABS#240
        // https://github.com/JakeWharton/ActionBarSherlock/issues/240
        selectInSpinnerIfPresent(position, true);
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
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        // fixes ABS #351
        // https://github.com/JakeWharton/ActionBarSherlock/issues/351
        int position = tab.getPosition();
        if (viewPager.getCurrentItem() != position) {
            viewPager.setCurrentItem(position);
            // TODO -- refreshing kills action bar?
            // refresh(position);
            setTitle(position);
        }
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    private void setTitle(int tabIdx) {
        if (tabIdx == 0) {
            activity.setTitle(R.string.favorites);
        } else {
            activity.setTitle(R.string.history);
        }
    }

    /**
     * Hack that takes advantage of interface parity between ActionBarSherlock
     * and the native interface to reach inside the classes to manually select
     * the appropriate tab spinner position if the overflow tab spinner is
     * showing.
     * 
     * Related issues:
     * https://github.com/JakeWharton/ActionBarSherlock/issues/240 and
     * https://android-review.googlesource.com/#/c/32492/
     * 
     * @author toulouse@crunchyroll.com
     */
    private void selectInSpinnerIfPresent(int position, boolean animate) {
        try {
            View actionBarView = null;//activity.findViewById(android.R.id.action_bar);
            if (actionBarView == null) {
                int id = activity.getResources().getIdentifier("action_bar",
                        "id",
                        "android");
                actionBarView = activity.findViewById(id);
            }

            Class<?> actionBarViewClass = actionBarView.getClass();
            Field mTabScrollViewField = actionBarViewClass
                    .getDeclaredField("mTabScrollView");
            mTabScrollViewField.setAccessible(true);

            Object mTabScrollView = mTabScrollViewField.get(actionBarView);
            if (mTabScrollView == null) {
                return;
            }

            Field mTabSpinnerField = mTabScrollView.getClass()
                    .getDeclaredField("mTabSpinner");
            mTabSpinnerField.setAccessible(true);

            Object mTabSpinner = mTabSpinnerField.get(mTabScrollView);
            if (mTabSpinner == null) {
                return;
            }

            Method setSelectionMethod = mTabSpinner
                    .getClass()
                    .getSuperclass()
                    .getDeclaredMethod("setSelection", Integer.TYPE,
                            Boolean.TYPE);
            setSelectionMethod.invoke(mTabSpinner, position, animate);

        } catch (IllegalArgumentException e) {
            Log.w(TAG, "TabsPagerAdapter.selectInSpinnerIfPresent()", e);
        } catch (IllegalAccessException e) {
            Log.w(TAG, "TabsPagerAdapter.selectInSpinnerIfPresent()", e);
        } catch (NoSuchFieldException e) {
            Log.w(TAG, "TabsPagerAdapter.selectInSpinnerIfPresent()", e);
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "TabsPagerAdapter.selectInSpinnerIfPresent()", e);
        } catch (InvocationTargetException e) {
            Log.w(TAG, "TabsPagerAdapter.selectInSpinnerIfPresent()", e);
        }
    }

}
