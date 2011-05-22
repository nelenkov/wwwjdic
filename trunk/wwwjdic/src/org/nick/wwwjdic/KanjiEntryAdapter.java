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

        if (convertView == null) {
            return new KanjiEntryView(context, entry);
        }

        KanjiEntryView entryView = (KanjiEntryView) convertView;
        entryView.populate(entry);

        return entryView;
    }

    private static final class KanjiEntryView extends LinearLayout {
        private TextView entryText;
        private TextView onyomiText;
        private TextView kunyomiText;
        private TextView translationText;

        public KanjiEntryView(Context context, KanjiEntry entry) {
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

            LinearLayout.LayoutParams entryParams = new LinearLayout.LayoutParams(
                    params);
            entryParams.gravity = Gravity.CENTER;
            entryText = createTextView(context, R.style.kanji_list_heading,
                    entry.getKanji());
            entryText.setGravity(Gravity.CENTER);
            entryText.setTextSize(42f);
            addView(entryText, entryParams);

            onyomiText = createTextView(context, R.style.kanji_list_reading, "");
            readingMeaningsLayout.addView(onyomiText, params);
            if (entry.getOnyomi() != null) {
                onyomiText.setText(entry.getOnyomi());
            }

            kunyomiText = createTextView(context, R.style.kanji_list_reading,
                    "");
            readingMeaningsLayout.addView(kunyomiText, params);
            if (entry.getKunyomi() != null) {
                kunyomiText.setText(entry.getKunyomi());
            }

            String meaningsStr = entry.getMeaningsAsString();
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
        }
    }

}
