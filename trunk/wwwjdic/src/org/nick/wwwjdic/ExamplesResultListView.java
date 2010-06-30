package org.nick.wwwjdic;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ExamplesResultListView extends ResultListViewBase<ExampleSentence> {

    private static final String TAG = ExamplesResultListView.class
            .getSimpleName();

    private static final String EXAMPLE_SEARCH_QUERY_STR = "?11";

    private static final int MENU_ITEM_BREAK_DOWN = 0;
    private static final int MENU_ITEM_LOOKUP_ALL_KANJI = 1;
    private static final int MENU_ITEM_COPY_JP = 2;
    private static final int MENU_ITEM_COPY_ENG = 3;

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

            private static final int HILIGHT_COLOR = 0xff427ad7;
            private ExampleSentence sentence;

            private TextView japaneseSentenceText;
            private TextView englishSentenceText;

            ExampleSentenceView(Context context, ExampleSentence sentence,
                    String queryString) {
                super(context);
                this.sentence = sentence;

                LayoutInflater inflater = LayoutInflater.from(context);
                inflater.inflate(R.layout.example_sentence_item, this);

                japaneseSentenceText = (TextView) findViewById(R.id.japaneseSentenceText);
                englishSentenceText = (TextView) findViewById(R.id.englishSentenceText);

                populate(queryString);
            }

            private void populate(String queryString) {
                SpannableString english = markQueryString(
                        sentence.getEnglish(), queryString, true);
                SpannableString japanese = markQueryString(sentence
                        .getJapanese(), queryString, false);

                japaneseSentenceText.setText(japanese);
                englishSentenceText.setText(english);
            }

            private SpannableString markQueryString(String sentenceStr,
                    String queryString, boolean italicize) {
                SpannableString result = new SpannableString(sentenceStr);

                String sentenceUpper = sentenceStr.toUpperCase();
                String queryUpper = queryString.toUpperCase();

                int idx = sentenceUpper.indexOf(queryUpper);
                while (idx != -1) {
                    result.setSpan(new ForegroundColorSpan(HILIGHT_COLOR), idx,
                            idx + queryString.length(), 0);
                    if (italicize) {
                        result.setSpan(new StyleSpan(Typeface.ITALIC), idx, idx
                                + queryString.length(), 0);
                    }

                    int startIdx = idx + queryString.length() + 1;
                    if (startIdx <= sentenceStr.length() - 1) {
                        idx = sentenceUpper.indexOf(queryUpper, idx + 1);
                    } else {
                        break;
                    }
                }

                return result;
            }
        }

    }

    private List<ExampleSentence> sentences;

    private ClipboardManager clipboardManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search_results);
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        getListView().setOnCreateContextMenuListener(this);

        extractSearchCriteria();
        SearchTask<ExampleSentence> searchTask = new ExampleSearchTask(
                getWwwjdicUrl() + EXAMPLE_SEARCH_QUERY_STR,
                getHttpTimeoutSeconds(), this, criteria, criteria
                        .getNumMaxResults());
        submitSearchTask(searchTask);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        menu.add(0, MENU_ITEM_BREAK_DOWN, 0, R.string.break_down_jap);
        menu.add(0, MENU_ITEM_LOOKUP_ALL_KANJI, 1, R.string.look_up_all_kanji);
        menu.add(0, MENU_ITEM_COPY_JP, 2, R.string.copy_jp);
        menu.add(0, MENU_ITEM_COPY_ENG, 3, R.string.copy_eng);

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        switch (item.getItemId()) {
        case MENU_ITEM_BREAK_DOWN:
            breakDown(info.id);
            return true;
        case MENU_ITEM_LOOKUP_ALL_KANJI:
            lookupAllKanji(info.id);
            return true;
        case MENU_ITEM_COPY_JP:
            copyJapanese(info.id);
            return true;
        case MENU_ITEM_COPY_ENG:
            copyEnglish(info.id);
            return true;
        }
        return false;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        breakDown(id);
    }

    private void breakDown(long id) {
        Intent intent = new Intent(this, SentenceBreakdown.class);
        intent.putExtra(Constants.SENTENCE, getCurrentSentence(id)
                .getJapanese());
        startActivity(intent);
    }

    private void copyEnglish(long id) {
        ExampleSentence sentence = getCurrentSentence(id);
        clipboardManager.setText(sentence.getEnglish());
    }

    private ExampleSentence getCurrentSentence(long id) {
        return sentences.get((int) id);
    }

    private void copyJapanese(long id) {
        ExampleSentence sentence = getCurrentSentence(id);
        clipboardManager.setText(sentence.getJapanese());
    }

    private void lookupAllKanji(long id) {
        ExampleSentence sentence = getCurrentSentence(id);
        SearchCriteria criteria = SearchCriteria
                .createForKanjiOrReading(sentence.getJapanese());
        Intent intent = new Intent(this, KanjiResultListView.class);
        intent.putExtra(Constants.CRITERIA_KEY, criteria);
        startActivity(intent);
    }

    @Override
    public void setResult(final List<ExampleSentence> result) {
        guiThread.post(new Runnable() {
            public void run() {
                sentences = (List<ExampleSentence>) result;
                ExampleSentenceAdapter adapter = new ExampleSentenceAdapter(
                        ExamplesResultListView.this, sentences, criteria
                                .getQueryString());
                setListAdapter(adapter);
                getListView().setTextFilterEnabled(true);
                setTitle(String.format("%d example(s) for '%s'", sentences
                        .size(), criteria.getQueryString()));
                dismissProgressDialog();
            }
        });

    }

}
