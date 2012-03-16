package org.nick.wwwjdic;

import java.util.List;

import org.nick.wwwjdic.model.DictionaryEntry;
import org.nick.wwwjdic.utils.StringUtils;
import org.nick.wwwjdic.utils.UIUtils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Checkable;
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

        DictionaryEntryView result = null;
        if (convertView == null) {
            result = new DictionaryEntryView(context, entry);
        } else {
            result = (DictionaryEntryView) convertView;
        }
        result.populate(entry);

        return result;
    }

    private final class DictionaryEntryView extends LinearLayout implements
            Checkable {

        private TextView entryText;
        private TextView readingText;
        private TextView translationText;

        private boolean checked;

        public DictionaryEntryView(Context context, DictionaryEntry entry) {
            super(context);

            LayoutInflater inflater = LayoutInflater.from(context);
            inflater.inflate(R.layout.dict_item, this);

            entryText = (TextView) findViewById(R.id.entry_text);
            readingText = (TextView) findViewById(R.id.reading_text);
            translationText = (TextView) findViewById(R.id.translation_text);
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

        @Override
        public boolean isChecked() {
            return checked;
        }

        @Override
        public void setChecked(boolean checked) {
            this.checked = checked;
            setBackgroundResource(checked ? UIUtils
                    .getListActivatedResource(context) : 0);
        }

        @Override
        public void toggle() {
            setChecked(!checked);
        }
    }
}
