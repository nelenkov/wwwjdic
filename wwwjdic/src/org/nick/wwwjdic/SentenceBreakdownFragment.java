package org.nick.wwwjdic;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.nick.wwwjdic.actionprovider.ShareActionProvider;
import org.nick.wwwjdic.model.SearchCriteria;
import org.nick.wwwjdic.model.SentenceBreakdownEntry;
import org.nick.wwwjdic.model.WwwjdicQuery;
import org.nick.wwwjdic.utils.CheckableLinearLayout;
import org.nick.wwwjdic.utils.StringUtils;
import org.nick.wwwjdic.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;

@SuppressWarnings("deprecation")
@SuppressLint("NewApi")
public class SentenceBreakdownFragment extends
        ResultListFragmentBase<SentenceBreakdownEntry> implements
        TtsManager.TtsEnabled {

    private static final String TAG = SentenceBreakdownFragment.class.getSimpleName();

    private Toolbar toolbar;

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

        static class SentenceBreakdownEntryView extends CheckableLinearLayout {

            private TextView explanationText;
            private TextView wordText;
            private TextView readingText;
            private TextView translationText;

            SentenceBreakdownEntryView(Context context) {
                super(context);

                LayoutInflater inflater = LayoutInflater.from(context);
                inflater.inflate(R.layout.breakdown_item, this);

                explanationText = findViewById(R.id.explanationText);
                wordText = findViewById(R.id.wordText);
                UIUtils.setJpTextLocale(wordText);
                readingText = findViewById(R.id.readingText);
                UIUtils.setJpTextLocale(readingText);
                translationText = findViewById(R.id.translationText);
            }

            void populate(SentenceBreakdownEntry entry) {
                if (!StringUtils.isEmpty(entry.getExplanation())) {
                    readingText.setVisibility(VISIBLE);
                    explanationText.setText(entry.getExplanation());
                } else {
                    explanationText.setVisibility(GONE);
                }
                wordText.setText(entry.getWord());
                if (!StringUtils.isEmpty(entry.getReading())) {
                    readingText.setVisibility(VISIBLE);
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

    private static final String ENTRIES_KEY = "org.nick.wwwjdic.SENTENCE_BREAKDOWN_ENTRIES";

    private String sentenceStr;
    private String sentenceTranslation;
    private List<SentenceBreakdownEntry> entries;
    private SpannableString markedSentence;

    private TextView sentenceView;
    private TextView englishSentenceText;

    private static final int HILIGHT_COLOR1 = 0xff427ad7;
    private static final int HILIGHT_COLOR2 = 0xfff97600;

    private ClipboardManager clipboardManager;

    private String jpTtsEnginePackageName;
    private boolean showSpeakMenu = false;
    private TtsManager ttsManager;

    private ShareActionProvider shareActionProvider;

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

    public SentenceBreakdownFragment() {
        setRetainInstance(false);
        setHasOptionsMenu(true);
    }

    public int getShownIndex() {
        return getArguments().getInt("index", 0);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setRetainInstance(false);
        clipboardManager = (ClipboardManager) getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            entries = (List<SentenceBreakdownEntry>) savedInstanceState
                    .getSerializable(ENTRIES_KEY);
        }

        if (getView() == null) {
            return;
        }

        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
            args.putAll(getActivity().getIntent().getExtras());
        }

        sentenceStr = args.getString(EXTRA_SENTENCE);
        sentenceTranslation = args.getString(EXTRA_SENTENCE_TRANSLATION);
        markedSentence = new SpannableString(sentenceStr);

        ttsManager = new TtsManager(getActivity(), this, jpTtsEnginePackageName);

        toolbar = getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.inflateMenu(R.menu.example_breakdown);
            for (int i = 0; i < toolbar.getChildCount(); i++) {
                View child = toolbar.getChildAt(i);
                if (child instanceof ActionMenuView) {
                    child.getLayoutParams().width = ActionMenuView.LayoutParams.MATCH_PARENT;
                }
            }

            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onOptionsItemSelected(item);
                }
            });
            toolbar.setOnCreateContextMenuListener(new Toolbar.OnCreateContextMenuListener() {

                @Override
                public void onCreateContextMenu(ContextMenu contextMenu, View view,
                                                ContextMenu.ContextMenuInfo contextMenuInfo) {

                }
            });
        }

        sentenceView.setText(markedSentence);
        if (!StringUtils.isEmpty(sentenceTranslation)) {
            englishSentenceText.setVisibility(View.VISIBLE);
            englishSentenceText.setText(sentenceTranslation);
        } else {
            englishSentenceText.setVisibility(View.GONE);
        }

        if (entries != null) {
            updateEntries(entries);

            return;
        }

        WwwjdicQuery query = new WwwjdicQuery(sentenceStr);
        SearchTask<SentenceBreakdownEntry> searchTask = new SentenceBreakdownTask(
                getWwwjdicUrl(), getHttpTimeoutSeconds(), this, query);
        submitSearchTask(searchTask);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        jpTtsEnginePackageName = WwwjdicPreferences
                .getJpTtsEnginePackage(activity);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (ttsManager != null) {
            ttsManager.checkTtsAvailability();
        }
    }

    @Override
    public void onDestroy() {
        if (ttsManager != null) {
            ttsManager.shutdown();
        }

        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }

        View v = inflater.inflate(R.layout.sentence_breakdown_fragment,
                container, false);
        progressSpinner = v.findViewById(R.id.progress_spinner);
        emptyText = v.findViewById(android.R.id.empty);

        sentenceView = v.findViewById(R.id.sentence);
        UIUtils.setJpTextLocale(sentenceView);
        englishSentenceText = v.findViewById(R.id.englishSentence);

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (toolbar == null) {
            inflater.inflate(R.menu.example_breakdown, menu);
        }

        MenuItem menuItem = menu.findItem(R.id.menu_example_share);
        shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        if (shareActionProvider != null) {
            shareActionProvider.setShareIntent(createShareIntent());
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem item = menu.findItem(R.id.menu_ex_breakdown_speak);
        if (item != null) {
            item.setEnabled(showSpeakMenu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_example_copy_jp) {
            copyJapanese();
            return true;
        } else if (item.getItemId() == R.id.menu_example_copy_eng) {
            copyEnglish();
            return true;
        } else if (item.getItemId() == R.id.menu_example_lookup_kanji) {
            lookupAllKanji();
            return true;
        } else if (item.getItemId() == R.id.menu_ex_breakdown_speak) {
            if (ttsManager != null) {
                ttsManager.speak(sentenceStr);
            }
            return true;
        } else if (item.getItemId() == R.id.menu_example_share) {
            if (shareActionProvider != null) {
                return false;
            }

            share();

            return true;
        }

        return false;
    }

    private void share() {
        Intent shareIntent = createShareIntent();

        getActivity().startActivity(shareIntent);
    }

    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        String str = sentenceStr;
        if (!TextUtils.isEmpty(sentenceTranslation)) {
            str += "\n" + sentenceTranslation;
        }
        shareIntent.putExtra(Intent.EXTRA_TEXT, str);

        return shareIntent;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(ENTRIES_KEY,
                (ArrayList<SentenceBreakdownEntry>) entries);
    }

    private void copyEnglish() {
        if (sentenceTranslation != null) {
            clipboardManager.setText(sentenceTranslation);
            Toast.makeText(getActivity(), R.string.copied_eng,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void copyJapanese() {
        clipboardManager.setText(sentenceStr);
        Toast.makeText(getActivity(), R.string.copied_jp, Toast.LENGTH_SHORT)
                .show();
    }

    private void lookupAllKanji() {
        SearchCriteria criteria = SearchCriteria
                .createForKanjiOrReading(sentenceStr);
        Intent intent = new Intent(getActivity(), KanjiResultList.class);
        intent.putExtra(Wwwjdic.EXTRA_CRITERIA, criteria);
        startActivity(intent);
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
        if (getView() == null) {
            return;
        }

        guiThread.post(new Runnable() {
            public void run() {
                updateEntries(result);
                dismissProgress();
            }
        });
    }

    private void setTitleAndMarkSentence() {
        getActivity().setTitle(
                sentenceTranslation != null ? R.string.sentence_breakdown
                        : R.string.sentence_translation);
        sentenceView.setText(markedSentence);
    }

    private void markSentence() {
        for (SentenceBreakdownEntry entry : entries) {
            markString(markedSentence, entry.getInflectedForm());
        }
    }

    private void updateEntries(final List<SentenceBreakdownEntry> result) {
        entries = result;
        Log.d(TAG, "entries: " + entries);
        SentenceBreakdownAdapter adapter = new SentenceBreakdownAdapter(
                getActivity(), entries);
        setListAdapter(adapter);
        getListView().setTextFilterEnabled(true);

        markSentence();
        setTitleAndMarkSentence();
    }

    @Override
    public Locale getSpeechLocale() {
        return Locale.JAPAN;
    }

    @Override
    public void showTtsButtons() {
        showSpeakMenu = true;
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void hideTtsButtons() {
        showSpeakMenu = false;
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public boolean wantsTts() {
        // XXX don't show install dialog?
        return false;
    }

    @Override
    public void setWantsTts(boolean wantsTts) {
    }
}
