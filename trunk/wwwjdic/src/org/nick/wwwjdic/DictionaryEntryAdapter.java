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

    private static final int SHORT_TRANSLATION_LENGTH = 40;

    private final Context context;
    private final List<DictionaryEntry> entries;

    public DictionaryEntryAdapter(Context context, List<DictionaryEntry> entries) {
        this.context = context;
        this.entries = entries;
    }

    public int getCount() {
        return entries == null ? 0 : entries.size();
    }

    public Object getItem(int position) {
        return entries == null ? null : entries.get(position);
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
        private TextView readingText;
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

            if (entry.getReading() != null) {
                readingText = new TextView(context);
                readingText.setText(entry.getReading());
                readingText.setTextSize(18f);
                readingText.setTextColor(Color.WHITE);
                addView(readingText, params);
            }

            translationText = new TextView(context);
            String translationStr = StringUtils.join(entry.getMeanings(), "/",
                    0);
            translationText.setText(StringUtils.shorten(translationStr,
                    SHORT_TRANSLATION_LENGTH));
            translationText.setTextSize(16f);
            translationText.setTextColor(Color.WHITE);
            addView(translationText, params);
        }
    }
}
