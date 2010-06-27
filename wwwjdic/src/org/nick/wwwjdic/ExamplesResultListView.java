package org.nick.wwwjdic;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ExamplesResultListView extends ResultListViewBase<ExampleSentence> {

    private static final String EXAMPLE_SEARCH_QUERY_STR = "?11";

    static class ExampleSentenceAdapter extends BaseAdapter {

        private final Context context;
        private final List<ExampleSentence> entries;
        private final String queryString;

        public ExampleSentenceAdapter(Context context,
                List<ExampleSentence> entries, String queryString) {
            this.context = context;
            this.entries = entries;
            this.queryString = queryString;
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
            ExampleSentence entry = entries.get(position);

            return new ExampleSentenceView(context, entry, queryString);
        }

        static class ExampleSentenceView extends LinearLayout {

            private ExampleSentence sentence;
            private String queryString;

            private TextView japaneseSentenceText;
            private TextView englishSentenceText;

            ExampleSentenceView(Context context, ExampleSentence sentence,
                    String queryString) {
                super(context);
                this.sentence = sentence;
                this.queryString = queryString;

                LayoutInflater inflater = LayoutInflater.from(context);
                inflater.inflate(R.layout.example_sentence_item, this);

                japaneseSentenceText = (TextView) findViewById(R.id.japaneseSentenceText);
                englishSentenceText = (TextView) findViewById(R.id.englishSentenceText);

                populate(queryString);
            }

            private void populate(String queryString) {
                SpannableString japanese = new SpannableString(sentence
                        .getJapanese());
                SpannableString english = new SpannableString(sentence
                        .getEnglish());
                int idx = sentence.getJapanese().indexOf(queryString);
                if (idx != -1) {
                    japanese.setSpan(new ForegroundColorSpan(Color.RED), idx,
                            idx + queryString.length(), 0);
                }

                idx = sentence.getEnglish().indexOf(queryString);
                if (idx != -1) {
                    english.setSpan(new ForegroundColorSpan(Color.RED), idx,
                            idx + queryString.length(), 0);
                }

                japaneseSentenceText.setText(japanese);
                englishSentenceText.setText(english);
            }
        }

    }

    private List<ExampleSentence> entries;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        extractSearchCriteria();
        TranslateTask<ExampleSentence> translateTask = new ExampleSearchTask(
                getWwwjdicUrl() + EXAMPLE_SEARCH_QUERY_STR,
                getHttpTimeoutSeconds(), this, criteria, criteria
                        .getNumMaxResults());
        submitTranslateTask(translateTask);
    }

    @Override
    public void setResult(final List<ExampleSentence> result) {
        guiThread.post(new Runnable() {
            public void run() {
                entries = (List<ExampleSentence>) result;
                ExampleSentenceAdapter adapter = new ExampleSentenceAdapter(
                        ExamplesResultListView.this, entries, criteria
                                .getQueryString());
                setListAdapter(adapter);
                getListView().setTextFilterEnabled(true);
                setTitle(String.format("%d example(s) for '%s'",
                        entries.size(), criteria.getQueryString()));
                dismissProgressDialog();
            }
        });

    }

}
