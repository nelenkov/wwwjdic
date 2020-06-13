package org.nick.wwwjdic;

import android.os.Bundle;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

public class ExampleLookupFragment extends Fragment {

    private static View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // hack: avoid IllegalArgumentException: Binary XML file line #9: Duplicate id
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }

        try {
            view = inflater.inflate(R.layout.example_search_tab, container, false);
        }  catch (InflateException ignored) {
            /* map is already there, just return view as it is */
        }

        return view;
    }
}
