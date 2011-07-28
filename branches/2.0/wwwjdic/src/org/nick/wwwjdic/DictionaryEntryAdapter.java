package org.nick.wwwjdic;

import java.util.List;

import org.nick.wwwjdic.utils.StringUtils;

import android.content.Context;
import android.text.TextUtils.TruncateAt;
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

        if (convertView == null) {
            return new DictionaryEntryView(context, entry);
        }

        DictionaryEntryView entryView = (DictionaryEntryView) convertView;
        entryView.populate(entry);

        return entryView;
    }

    private final class DictionaryEntryView extends LinearLayout {
        private TextView entryText;
        private TextView readingText;
        private TextView translationText;

        public DictionaryEntryView(Context context, DictionaryEntry entry) {
            super(context);
            setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(5, 3, 5, 0);

            entryText = createTextView(context, R.style.dict_list_heading,
                    entry.getWord());
            addView(entryText, params);

            readingText = createTextView(context, R.style.dict_list_reading, "");
            addView(readingText, params);
            if (entry.getReading() != null) {
                readingText.setText(entry.getReading());
            }

            String translationStr = StringUtils.join(entry.getMeanings(), "/",
                    0);
            translationText = createTextView(context,
                    R.style.dict_list_translation, translationStr);
            addView(translationText, params);
        }

        private TextView createTextView(Context context, int style, String text) {
            TextView result = new TextView(context, null, style);
            result.setSingleLine(true);
            result.setEllipsize(TruncateAt.END);
            result.setText(text);

            return result;
        }

        void populate(DictionaryEntry entry) {
            entryText.setText(entry.getWord());
            if (entry.getReading() != null) {
                readingText.setText(entry.getReading());
            } else {
                readingText.setText("");
            }
            String translationStr = StringUtils.join(entry.getMeanings(), "/",
                    0);
            translationText.setText(translationStr);
        }
    }
}
