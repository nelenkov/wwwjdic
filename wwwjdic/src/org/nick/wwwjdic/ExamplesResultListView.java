package org.nick.wwwjdic;

import java.util.List;

import android.content.Context;
import android.os.Bundle;
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

        public ExampleSentenceAdapter(Context context,
                List<ExampleSentence> entries) {
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
            ExampleSentence entry = entries.get(position);

            return new ExampleSentenceView(context, entry);
        }

        static class ExampleSentenceView extends LinearLayout {

            private ExampleSentence sentence;

            private TextView japaneseSentenceText;
            private TextView englishSentenceText;

            ExampleSentenceView(Context context, ExampleSentence sentence) {
                super(context);
                this.sentence = sentence;

                LayoutInflater inflater = LayoutInflater.from(context);
                inflater.inflate(R.layout.example_sentence_item, this);

                japaneseSentenceText = (TextView) findViewById(R.id.japaneseSentenceText);
                englishSentenceText = (TextView) findViewById(R.id.englishSentenceText);

                populate();
            }

            private void populate() {
                japaneseSentenceText.setText(sentence.getJapanese());
                englishSentenceText.setText(sentence.getEnglish());
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
                        ExamplesResultListView.this, entries);
                setListAdapter(adapter);
                getListView().setTextFilterEnabled(true);
                setTitle(String.format("%d example(s) for '%s'",
                        entries.size(), criteria.getQueryString()));
                dismissProgressDialog();
            }
        });

    }

}
