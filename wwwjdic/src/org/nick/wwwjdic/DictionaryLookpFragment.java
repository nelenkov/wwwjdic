package org.nick.wwwjdic;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class DictionaryLookpFragment extends Fragment {

    private static final String TAG = DictionaryLookpFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dict_lookup_tab, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentManager fm = getChildFragmentManager();

        DictionaryFragment dictFragment = (DictionaryFragment) fm.findFragmentByTag("dictionaryFragment");
        if (dictFragment == null) {
            Log.d(TAG, "adding DictionaryFragment");
            dictFragment = new DictionaryFragment();
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.dict_container, dictFragment, "dictionaryFragment");
//              FavoritesAndHistorySummaryView summaryView = new FavoritesAndHistorySummaryView(getContext());
//            int heightPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics());
//            summaryView.getLayoutParams().height = heightPx;
//            summaryView.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
//            int paddingLR = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, getResources().getDisplayMetrics());
//            summaryView.setPadding(paddingLR, 0, paddingLR, 0);
//            LinearLayout tabContainer = view.findViewById(R.id.tab_container);
            //summaryView.addView(summaryView);
            ft.commit();
            fm.executePendingTransactions();
        }
    }
}
