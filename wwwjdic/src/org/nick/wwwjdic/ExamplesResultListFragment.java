package org.nick.wwwjdic;

import java.util.List;

import org.nick.wwwjdic.model.ExampleSentence;
import org.nick.wwwjdic.model.SearchCriteria;
import org.nick.wwwjdic.utils.Analytics;
import org.nick.wwwjdic.utils.CheckableLinearLayout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.ClipboardManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

@SuppressWarnings("deprecation")
public class ExamplesResultListFragment extends
        ResultListFragmentBase<ExampleSentence> implements
        OnItemLongClickListener {

    public static String EXTRA_EXAMPLES_BACKDOOR_SEARCH = "org.nick.wwwjdic.EXAMPLES_BACKDOOR_SEARCH";

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

            if (convertView == null) {
                convertView = new ExampleSentenceView(context);
            }

            ((ExampleSentenceView) convertView).populate(entry, queryString);

            return convertView;
        }

        static class ExampleSentenceView extends CheckableLinearLayout {

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

    private ActionMode currentActionMode;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        clipboardManager = (ClipboardManager) getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE);

        getListView().setOnItemLongClickListener(this);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

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
    public void onResume() {
        super.onResume();

        checkOrClearCurrentItem();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.search_results_fragment, container,
                false);

        return v;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        getListView().setItemChecked(position, false);

        ExampleSentence sentence = getCurrentSentence(id);
        breakDown(sentence, position);
    }

    private void breakDown(ExampleSentence sentence, int index) {
        Analytics.event("sentenceBreakdown", getActivity());

        if (dualPane) {
            getListView().setItemChecked(index, true);
            currentCheckPosition = index;

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
        Toast.makeText(getActivity(), R.string.copied_eng, Toast.LENGTH_SHORT)
                .show();
    }

    private ExampleSentence getCurrentSentence(long id) {
        return sentences.get((int) id);
    }

    private void copyJapanese(long id) {
        ExampleSentence sentence = getCurrentSentence(id);
        clipboardManager.setText(sentence.getJapanese());
        Toast.makeText(getActivity(), R.string.copied_jp, Toast.LENGTH_SHORT)
                .show();
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
                // backed out before view is created
                if (getView() == null) {
                    return;
                }

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

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        if (currentActionMode != null) {
            return false;
        }

        currentActionMode = getSherlockActivity().startActionMode(
                new ContextCallback(position));
        getListView().setItemChecked(position, true);

        return true;
    }

    @SuppressLint("NewApi")
    class ContextCallback implements ActionMode.Callback {

        private int position;

        ContextCallback(int position) {
            this.position = position;
        }

        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = getSherlockActivity()
                    .getSupportMenuInflater();
            inflater.inflate(R.menu.example_list_context, menu);
            return true;
        }

        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        public boolean onActionItemClicked(ActionMode actionMode,
                MenuItem menuItem) {

            if (menuItem.getItemId() == R.id.menu_context_example_list_lookup_kanji) {
                lookupAllKanji(position);
                actionMode.finish();
                return true;
            } else if (menuItem.getItemId() == R.id.menu_context_example_list_copy_jp) {
                copyJapanese(position);
                actionMode.finish();
                return true;
            } else if (menuItem.getItemId() == R.id.menu_context_example_list_copy_eng) {
                copyEnglish(position);
                actionMode.finish();
                return true;
            }

            return false;
        }

        public void onDestroyActionMode(ActionMode actionMode) {
            getListView().setItemChecked(position, false);
            currentActionMode = null;
        }
    };

}
