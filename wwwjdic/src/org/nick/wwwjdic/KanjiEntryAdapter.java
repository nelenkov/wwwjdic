package org.nick.wwwjdic;

import java.util.List;

import org.nick.wwwjdic.model.KanjiEntry;
import org.nick.wwwjdic.utils.CheckableLinearLayout;
import org.nick.wwwjdic.utils.UIUtils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class KanjiEntryAdapter extends BaseAdapter {

    private final Context context;
    private final List<KanjiEntry> entries;

    public KanjiEntryAdapter(Context context, List<KanjiEntry> entries) {
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
        KanjiEntry entry = entries.get(position);

        KanjiEntryView result = null;
        if (convertView == null) {
            result = new KanjiEntryView(context, entry);
        } else {
            result = (KanjiEntryView) convertView;
        }
        result.populate(entry);

        return result;
    }

    private static final class KanjiEntryView extends CheckableLinearLayout {

        private final TextView entryText;
        private final TextView onyomiText;
        private final TextView kunyomiText;
        private final TextView translationText;

        public KanjiEntryView(Context context, KanjiEntry entry) {
            super(context);

            LayoutInflater inflater = LayoutInflater.from(context);
            inflater.inflate(R.layout.kanji_item, this);

            entryText = findViewById(R.id.kanji_text);
            UIUtils.setJpTextLocale(entryText);
            onyomiText = findViewById(R.id.onyomi_text);
            UIUtils.setJpTextLocale(onyomiText);
            kunyomiText = findViewById(R.id.kunyomi_text);
            UIUtils.setJpTextLocale(kunyomiText);
            translationText = findViewById(R.id.translation_text);
        }

        void populate(KanjiEntry entry) {
            entryText.setText(entry.getKanji());
            if (entry.getOnyomi() != null) {
                onyomiText.setText(entry.getOnyomi());
            } else {
                onyomiText.setText("");
            }
            if (entry.getKunyomi() != null) {
                kunyomiText.setText(entry.getKunyomi());
            } else {
                kunyomiText.setText("");
            }
            String meaningsStr = entry.getMeaningsAsString();
            translationText.setText(meaningsStr);
            setChecked(false);
        }

    }

}
