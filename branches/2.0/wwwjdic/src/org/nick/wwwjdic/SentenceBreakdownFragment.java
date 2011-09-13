package org.nick.wwwjdic;

import java.util.List;

import org.nick.wwwjdic.utils.StringUtils;

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

public class SentenceBreakdownFragment extends
        ResultListFragmentBase<SentenceBreakdownEntry> {

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

    public static final String EXTRA_SENTENCE = "org.nick.wwwjdic.SENTENCE";
    public static final String EXTRA_SENTENCE_TRANSLATION = "org.nick.wwwjdic.SENTENCE_TRANSLATION";

    private List<SentenceBreakdownEntry> entries;

    private TextView sentenceView;
    private TextView englishSentenceText;
    private SpannableString markedSentence;

    private static final int HILIGHT_COLOR1 = 0xff427ad7;
    private static final int HILIGHT_COLOR2 = 0xfff97600;

    public static SentenceBreakdownFragment newInstance(int index,
            String senteceStr, String sentenceTranslation) {
        SentenceBreakdownFragment f = new SentenceBreakdownFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt("index", index);
        args.putString(EXTRA_SENTENCE, senteceStr);
        args.putString(EXTRA_SENTENCE_TRANSLATION, sentenceTranslation);
        f.setArguments(args);

        return f;
    }

    public int getShownIndex() {
        return getArguments().getInt("index", 0);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
            args.putAll(getActivity().getIntent().getExtras());
        }

        String sentenceStr = args.getString(EXTRA_SENTENCE);
        String sentenceTranslation = args.getString(EXTRA_SENTENCE_TRANSLATION);
        markedSentence = new SpannableString(sentenceStr);

        sentenceView.setText(markedSentence);
        if (!StringUtils.isEmpty(sentenceTranslation)) {
            englishSentenceText.setText(sentenceTranslation);
        } else {
            englishSentenceText.setVisibility(View.GONE);
        }

        WwwjdicQuery query = new WwwjdicQuery(sentenceStr);
        SearchTask<SentenceBreakdownEntry> searchTask = new SentenceBreakdownTask(
                getWwwjdicUrl(), getHttpTimeoutSeconds(), this, query);
        submitSearchTask(searchTask);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.sentence_breakdown_fragment,
                container, false);
        sentenceView = (TextView) v.findViewById(R.id.sentence);
        englishSentenceText = (TextView) v.findViewById(R.id.englishSentence);

        return v;
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
                        getActivity(), entries);
                setListAdapter(adapter);
                getListView().setTextFilterEnabled(true);
                getActivity().setTitle(R.string.sentence_breakdown);

                for (SentenceBreakdownEntry entry : entries) {
                    markString(markedSentence, entry.getInflectedForm());
                }
                sentenceView.setText(markedSentence);
                dismissProgressDialog();
            }
        });
    }
}
