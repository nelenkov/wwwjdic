package org.nick.wwwjdic;

import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SentenceBreakdown extends
        ResultListViewBase<SentenceBreakdownEntry> {

    static class SentenceBreakdownAdapter extends BaseAdapter {

        private final Context context;
        private final List<SentenceBreakdownEntry> entries;

        public SentenceBreakdownAdapter(Context context,
                List<SentenceBreakdownEntry> entries) {
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
            SentenceBreakdownEntry entry = entries.get(position);

            return new SentenceBreakdownEntryView(context, entry);
        }

        static class SentenceBreakdownEntryView extends LinearLayout {

            private SentenceBreakdownEntry entry;

            private TextView explanationText;
            private TextView wordText;
            private TextView readingText;
            private TextView translationText;

            SentenceBreakdownEntryView(Context context,
                    SentenceBreakdownEntry entry) {
                super(context);
                this.entry = entry;

                LayoutInflater inflater = LayoutInflater.from(context);
                inflater.inflate(R.layout.breakdown_item, this);

                explanationText = (TextView) findViewById(R.id.explanationText);
                wordText = (TextView) findViewById(R.id.wordText);
                readingText = (TextView) findViewById(R.id.readingText);
                translationText = (TextView) findViewById(R.id.translationText);

                populate();
            }

            private void populate() {
                if (!StringUtils.isEmpty(entry.getExplanation())) {
                    explanationText.setText(entry.getExplanation());
                } else {
                    explanationText.setVisibility(GONE);
                }
                wordText.setText(entry.getWord());
                if (!StringUtils.isEmpty(entry.getReading())) {
                    readingText.setText(entry.getReading());
                } else {
                    readingText.setVisibility(GONE);
                }
                translationText.setText(entry.getTranslation());
            }

        }

    }

    private List<SentenceBreakdownEntry> entries;

    private TextView sentenceView;
    private SpannableString markedSentence;

    private static final int HILIGHT_COLOR1 = 0xff427ad7;
    private static final int HILIGHT_COLOR2 = 0xfff97600;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sentence_breakdown);

        Bundle extras = getIntent().getExtras();
        String sentenceStr = extras.getString(Constants.SENTENCE);
        markedSentence = new SpannableString(sentenceStr);

        sentenceView = (TextView) findViewById(R.id.sentence);
        sentenceView.setText(markedSentence);

        WwwjdicQuery query = new WwwjdicQuery(sentenceStr);
        SearchTask<SentenceBreakdownEntry> searchTask = new SentenceBreakdownTask(
                getWwwjdicUrl(), getHttpTimeoutSeconds(), this, query);
        submitSearchTask(searchTask);
    }

    private void markString(SpannableString spannable, String term, int color) {
        String sentence = spannable.toString();
        String[] terms = term.split(" ");
        if (terms.length > 1) {
            term = terms[0];
        }

        int idx = sentence.indexOf(term);
        while (idx != -1) {
            spannable.setSpan(new ForegroundColorSpan(color), idx, idx
                    + term.length(), 0);

            int startIdx = idx + term.length() + 1;
            if (startIdx <= spannable.length() - 1) {
                idx = sentence.indexOf(term, idx + 1);
            } else {
                break;
            }
        }
    }

    @Override
    public void setResult(final List<SentenceBreakdownEntry> result) {
        guiThread.post(new Runnable() {
            public void run() {
                entries = (List<SentenceBreakdownEntry>) result;
                SentenceBreakdownAdapter adapter = new SentenceBreakdownAdapter(
                        SentenceBreakdown.this, entries);
                setListAdapter(adapter);
                getListView().setTextFilterEnabled(true);
                setTitle(R.string.sentence_breakdown);

                int i = 0;
                for (SentenceBreakdownEntry entry : entries) {
                    markString(markedSentence, entry.getWord(),
                            i % 2 == 0 ? HILIGHT_COLOR1 : HILIGHT_COLOR2);
                    i++;
                }
                sentenceView.setText(markedSentence);
                dismissProgressDialog();
            }
        });
    }
}
