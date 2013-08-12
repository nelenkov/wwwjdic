package org.nick.wwwjdic;

import java.util.List;

import org.nick.wwwjdic.utils.UIUtils;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CandidatesAdapter extends ArrayAdapter<String> {

    public CandidatesAdapter(Context context, int resource, String[] objects) {
        super(context, resource, objects);
    }

    public CandidatesAdapter(Context context, int textViewResourceId,
            List<String> objects) {
        super(context, textViewResourceId, objects);
    }

    public CandidatesAdapter(Context context, int resource,
            int textViewResourceId, String[] objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public CandidatesAdapter(Context context, int resource,
            int textViewResourceId, List<String> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public CandidatesAdapter(Context context, int resource,
            int textViewResourceId) {
        super(context, resource, textViewResourceId);
    }

    public CandidatesAdapter(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        if (view instanceof TextView) {
            UIUtils.setJpTextLocale((TextView) view);
        } else {
            TextView tv = (TextView) view.findViewById(R.id.item_text);
            UIUtils.setJpTextLocale(tv);
        }

        return view;
    }

}