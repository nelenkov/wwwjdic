package org.nick.wwwjdic;

import java.util.List;

import org.nick.wwwjdic.model.ExampleSentence;
import org.nick.wwwjdic.model.SearchCriteria;
import org.nick.wwwjdic.utils.Analytics;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.ClipboardManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class ExamplesResultListFragment extends
        ResultListFragmentBase<ExampleSentence> {

    private static final String TAG = ExamplesResultListFragment.class
            .getSimpleName();

    public static String EXTRA_EXAMPLES_BACKDOOR_SEARCH = "org.nick.wwwjdic.EXAMPLES_BACKDOOR_SEARCH";

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

            if (convertView == null) {
                convertView = new ExampleSentenceView(context);
            }

            ((ExampleSentenceView) convertView).populate(entry, queryString);

            return convertView;
        }

        static class ExampleSentenceView extends LinearLayout {

            private static final int HILIGHT_COLOR = 0xff427ad7;

            private TextView japaneseSentenceText;
            private TextView englishSentenceText;

            ExampleSentenceView(Context context) {
                super(context);

                LayoutInflater inflater = LayoutInflater.from(context);
                inflater.inflate(R.layout.example_sentence_item, this);

                japaneseSentenceText = (TextView) findViewById(R.id.japaneseSentenceText);
                englishSentenceText = (TextView) findViewById(R.id.englishSentenceText);
            }

            void populate(ExampleSentence sentence, String queryString) {
                SpannableString english = markQueryString(
                        sentence.getEnglish(), queryString, true);
                SpannableString japanese = null;
                if (sentence.getMatches().isEmpty()) {
                    japanese = markQueryString(sentence.getJapanese(),
                            queryString, false);
                } else {
                    japanese = new SpannableString(sentence.getJapanese());
                    for (String match : sentence.getMatches()) {
                        markQueryString(japanese, sentence.getJapanese(),
                                match, false);
                    }
                }

                japaneseSentenceText.setText(japanese);
                englishSentenceText.setText(english);
            }

            private SpannableString markQueryString(String sentenceStr,
                    String queryString, boolean italicize) {
                SpannableString result = new SpannableString(sentenceStr);
                markQueryString(result, sentenceStr, queryString, italicize);

                return result;
            }

            private void markQueryString(SpannableString result,
                    String sentenceStr, String queryString, boolean italicize) {
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
            }
        }

    }

    private List<ExampleSentence> sentences;

    private ClipboardManager clipboardManager;

    private boolean dualPane;
    private int currentCheckPosition = 0;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        clipboardManager = (ClipboardManager) getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE);

        getListView().setOnCreateContextMenuListener(this);

        View detailsFrame = getActivity().findViewById(R.id.details);
        dualPane = detailsFrame != null
                && detailsFrame.getVisibility() == View.VISIBLE;

        if (sentences != null) {
            // we are being re-created after rotation, use existing data
            setTitleAndCurrentItem();

            return;
        }

        extractSearchCriteria();

        boolean useBackdoor = getActivity().getIntent().getBooleanExtra(
                EXTRA_EXAMPLES_BACKDOOR_SEARCH, false);
        SearchTask<ExampleSentence> searchTask = null;
        if (useBackdoor) {
            searchTask = new ExampleSearchTaskBackdoor(getWwwjdicUrl(),
                    getHttpTimeoutSeconds(), this, criteria,
                    WwwjdicPreferences.isReturnRandomExamples(getActivity()));
        } else {
            searchTask = new ExampleSearchTask(getWwwjdicUrl()
                    + EXAMPLE_SEARCH_QUERY_STR, getHttpTimeoutSeconds(), this,
                    criteria, criteria.getNumMaxResults());
        }

        submitSearchTask(searchTask);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.search_results_fragment, container,
                false);

        return v;
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
            breakDown(getCurrentSentence(info.position), info.position);
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
    public void onListItemClick(ListView l, View v, int position, long id) {
        ExampleSentence sentence = getCurrentSentence(id);
        breakDown(sentence, position);
    }

    private void breakDown(ExampleSentence sentence, int index) {
        Analytics.event("sentenceBreakdown", getActivity());

        if (dualPane) {
            getListView().setItemChecked(index, true);

            SentenceBreakdownFragment breakdown = (SentenceBreakdownFragment) getFragmentManager()
                    .findFragmentById(R.id.details);
            if (breakdown == null || breakdown.getShownIndex() != index) {
                breakdown = SentenceBreakdownFragment.newInstance(index,
                        sentence.getJapanese(), sentence.getEnglish());

                FragmentTransaction ft = getFragmentManager()
                        .beginTransaction();
                ft.replace(R.id.details, breakdown);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
        } else {
            Intent intent = new Intent(getActivity(), SentenceBreakdown.class);
            intent.putExtra(SentenceBreakdown.EXTRA_SENTENCE,
                    sentence.getJapanese());
            intent.putExtra(SentenceBreakdown.EXTRA_SENTENCE_TRANSLATION,
                    sentence.getEnglish());
            startActivity(intent);
        }
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
        Intent intent = new Intent(getActivity(), KanjiResultList.class);
        intent.putExtra(Wwwjdic.EXTRA_CRITERIA, criteria);
        startActivity(intent);
    }

    @Override
    public void setResult(final List<ExampleSentence> result) {
        guiThread.post(new Runnable() {
            public void run() {
                sentences = (List<ExampleSentence>) result;
                ExampleSentenceAdapter adapter = new ExampleSentenceAdapter(
                        getActivity(), sentences, criteria.getQueryString());
                setListAdapter(adapter);
                getListView().setTextFilterEnabled(true);
                setTitleAndCurrentItem();
                dismissProgressDialog();
            }
        });

    }

    private void setTitleAndCurrentItem() {
        String message = getResources().getString(R.string.examples_for);
        getActivity().setTitle(
                String.format(message, sentences.size(),
                        criteria.getQueryString()));

        if (dualPane) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            if (!sentences.isEmpty()) {
                breakDown(sentences.get(currentCheckPosition),
                        currentCheckPosition);
            }
        }
    }

}
