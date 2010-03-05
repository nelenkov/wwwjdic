package org.nick.wwwjdic;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DictionaryEntryAdapter extends BaseAdapter {

    private final Context context;
    private final List<DictionaryEntry> entries;

    public DictionaryEntryAdapter(Context context, List<DictionaryEntry> entries) {
        this.context = context;
        this.entries = entries;
    }

    public int getCount() {
        return entries.size();
    }

    public Object getItem(int position) {
        return entries.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        DictionaryEntry entry = entries.get(position);

        return new DictionaryEntryListView(context, entry);
    }

    private final class DictionaryEntryListView extends LinearLayout {
        private TextView entryText;
        private TextView translationText;

        public DictionaryEntryListView(Context context, DictionaryEntry entry) {
            super(context);
            setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(5, 3, 5, 0);

            entryText = new TextView(context);
            entryText.setText(entry.getWord());
            entryText.setTextSize(20f);
            entryText.setTextColor(Color.WHITE);
            addView(entryText, params);

            translationText = new TextView(context);
            translationText.setText(entry.getTranslationString());
            translationText.setTextSize(16f);
            translationText.setTextColor(Color.WHITE);
            addView(translationText, params);
        }
    }
}
