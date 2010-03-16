package org.nick.wwwjdic;

import java.util.List;

import android.content.Context;
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

            entryText = new TextView(context, null, R.style.dict_list_heading);
            entryText.setText(entry.getWord());
            addView(entryText, params);

            if (entry.getReading() != null) {
                readingText = new TextView(context, null,
                        R.style.dict_list_reading);
                readingText.setText(entry.getReading());
                addView(readingText, params);
            }

            translationText = new TextView(context, null,
                    R.style.dict_list_translation);
            String translationStr = StringUtils.join(entry.getMeanings(), "/",
                    0);
            translationText.setText(StringUtils.shorten(translationStr,
                    SHORT_TRANSLATION_LENGTH));
            addView(translationText, params);
        }
    }
}
