package org.nick.wwwjdic;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class KanjiEntryAdapter extends BaseAdapter {

    private static final int SHORT_MEANING_LENGTH = 35;

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

        return new KanjiEntryListView(context, entry);
    }

    private static final class KanjiEntryListView extends LinearLayout {
        private TextView entryText;
        private TextView onyomiText;
        private TextView kunyomiText;
        private TextView translationText;

        public KanjiEntryListView(Context context, KanjiEntry entry) {
            super(context);
            setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(5, 3, 5, 0);

            LinearLayout readingMeaningsLayout = new LinearLayout(context);
            readingMeaningsLayout.setOrientation(LinearLayout.VERTICAL);
            params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(5, 3, 5, 0);

            entryText = new TextView(context, null, R.style.kanji_list_heading);
            entryText.setTextSize(38f);
            entryText.setText(entry.getKanji());
            addView(entryText, params);

            if (entry.getOnyomi() != null) {
                onyomiText = new TextView(context, null,
                        R.style.kanji_list_reading);
                onyomiText.setText(entry.getOnyomi());
                readingMeaningsLayout.addView(onyomiText, params);
            }

            if (entry.getKunyomi() != null) {
                kunyomiText = new TextView(context, null,
                        R.style.kanji_list_reading);
                kunyomiText.setText(entry.getKunyomi());
                readingMeaningsLayout.addView(kunyomiText, params);
            }

            translationText = new TextView(context, null,
                    R.style.kanji_list_translation);
            String meaningsStr = StringUtils.join(entry.getMeanings(), "/", 0);
            translationText.setText(StringUtils.shorten(meaningsStr,
                    SHORT_MEANING_LENGTH));
            readingMeaningsLayout.addView(translationText, params);

            addView(readingMeaningsLayout);
        }
    }
}
