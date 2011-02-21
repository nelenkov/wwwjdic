package org.nick.wwwjdic;

import static org.nick.wwwjdic.Constants.EXAMPLE_SEARRCH_TAB_IDX;
import static org.nick.wwwjdic.Constants.SELECTED_TAB_IDX;

import java.util.List;

import org.nick.wwwjdic.utils.StringUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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

            if (convertView == null) {
                convertView = new SentenceBreakdownEntryView(context);
            }

            ((SentenceBreakdownEntryView) convertView).populate(entry);

            return convertView;
        }

        static class SentenceBreakdownEntryView extends LinearLayout {

            private TextView explanationText;
            private TextView wordText;
            private TextView readingText;
            private TextView translationText;

            SentenceBreakdownEntryView(Context context) {
                super(context);

                LayoutInflater inflater = LayoutInflater.from(context);
                inflater.inflate(R.layout.breakdown_item, this);

                explanationText = (TextView) findViewById(R.id.explanationText);
                wordText = (TextView) findViewById(R.id.wordText);
                readingText = (TextView) findViewById(R.id.readingText);
                translationText = (TextView) findViewById(R.id.translationText);
            }

            void populate(SentenceBreakdownEntry entry) {
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

    private static final int ITEM_ID_HOME = 0;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ITEM_ID_HOME, 0, R.string.home).setIcon(
                android.R.drawable.ic_menu_compass);

        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
        case ITEM_ID_HOME:
            Intent intent = new Intent(this, Wwwjdic.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(SELECTED_TAB_IDX, EXAMPLE_SEARRCH_TAB_IDX);

            startActivity(intent);
            finish();

            return true;
        default:
            // do nothing
        }

        return super.onMenuItemSelected(featureId, item);
    }

    private void markString(SpannableString spannable, String term) {
        String sentence = spannable.toString();
        String[] terms = term.split(" ");
        if (terms.length > 1) {
            term = terms[0];
        }

        int idx = sentence.indexOf(term);
        while (idx != -1) {
            int color = HILIGHT_COLOR1;
            ForegroundColorSpan[] spans = spannable.getSpans(0, idx,
                    ForegroundColorSpan.class);
            if (spans.length > 0) {
                ForegroundColorSpan lastSpan = spans[spans.length - 1];
                if (lastSpan.getForegroundColor() == HILIGHT_COLOR1) {
                    color = HILIGHT_COLOR2;
                } else {
                    color = HILIGHT_COLOR1;
                }
            }

            int spanStart = idx;
            int spanEnd = idx + term.length();
            ForegroundColorSpan[] thisTermSpans = spannable.getSpans(spanStart,
                    spanEnd, ForegroundColorSpan.class);
            if (thisTermSpans.length == 0) {
                spannable.setSpan(new ForegroundColorSpan(color), spanStart,
                        spanEnd, 0);
                break;
            } else {
                int startIdx = idx + term.length() + 1;
                if (startIdx <= spannable.length() - 1) {
                    idx = sentence.indexOf(term, idx + 1);
                }
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

                for (SentenceBreakdownEntry entry : entries) {
                    markString(markedSentence, entry.getInflectedForm());
                }
                sentenceView.setText(markedSentence);
                dismissProgressDialog();
            }
        });
    }
}
