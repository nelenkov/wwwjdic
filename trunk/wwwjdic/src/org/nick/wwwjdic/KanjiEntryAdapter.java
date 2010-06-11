package org.nick.wwwjdic;

import java.util.List;

import android.content.Context;
import android.text.TextUtils.TruncateAt;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
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

            LinearLayout.LayoutParams entryParams = new LinearLayout.LayoutParams(params);
            entryParams.gravity = Gravity.CENTER;
            entryText = createTextView(context, R.style.kanji_list_heading,
                    entry.getKanji());
            entryText.setGravity(Gravity.CENTER);
            entryText.setTextSize(42f);
            addView(entryText, entryParams);

            if (entry.getOnyomi() != null) {
                onyomiText = createTextView(context,
                        R.style.kanji_list_reading, entry.getOnyomi());
                readingMeaningsLayout.addView(onyomiText, params);
            }

            if (entry.getKunyomi() != null) {
                kunyomiText = createTextView(context,
                        R.style.kanji_list_reading, entry.getKunyomi());
                readingMeaningsLayout.addView(kunyomiText, params);
            }

            String meaningsStr = StringUtils.join(entry.getMeanings(), "/", 0);
            translationText = createTextView(context,
                    R.style.kanji_list_translation, meaningsStr);
            readingMeaningsLayout.addView(translationText, params);

            addView(readingMeaningsLayout);
        }

        private TextView createTextView(Context context, int style, String text) {
            TextView result = new TextView(context, null, style);
            result.setSingleLine(true);
            result.setEllipsize(TruncateAt.END);
            result.setText(text);

            return result;
        }
    }

}
