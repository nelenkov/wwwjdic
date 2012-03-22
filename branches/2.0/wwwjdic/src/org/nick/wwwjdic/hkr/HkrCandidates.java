package org.nick.wwwjdic.hkr;

import org.nick.wwwjdic.ActionBarActivity;
import org.nick.wwwjdic.KanjiEntryDetail;
import org.nick.wwwjdic.KanjiEntryDetailFragment;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.model.KanjiEntry;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.actionbarsherlock.view.Window;

public class HkrCandidates extends ActionBarActivity implements
        HkrCandidatesFragment.HkrCandidateSelectedListener {

    public static final String EXTRA_HKR_CANDIDATES = "org.nick.wwwjdic.hkrCandidates";

    private boolean dualPane;

    private HkrCandidatesFragment candidatesFragment;

    public HkrCandidates() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.hkr_candidates);
        setSupportProgressBarIndeterminateVisibility(false);

        View detailsFrame = findViewById(R.id.details);
        dualPane = detailsFrame != null
                && detailsFrame.getVisibility() == View.VISIBLE;
        candidatesFragment = (HkrCandidatesFragment) getSupportFragmentManager()
                .findFragmentById(R.id.results_list);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!dualPane) {
            candidatesFragment.getListView().clearChoices();
        }
    }

    protected void checkOrClearCurrentItem() {
        if (!dualPane) {
            candidatesFragment.getListView().clearChoices();
        }
    }

    @Override
    public void onHkrCandidateSelected(KanjiEntry entry, int position) {
        showKanjiDetails(entry, position);
    }

    private void showKanjiDetails(KanjiEntry entry, int position) {
        if (dualPane) {
            KanjiEntryDetailFragment details = (KanjiEntryDetailFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.details);
            if (details == null
                    || !details.getEntry().getKanji().equals(entry.getKanji())) {
                details = KanjiEntryDetailFragment.newInstance(position, entry);

                FragmentTransaction ft = getSupportFragmentManager()
                        .beginTransaction();

                ft.replace(R.id.details, details);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commitAllowingStateLoss();
            }
        } else {
            candidatesFragment.getListView().clearChoices();

            Bundle extras = new Bundle();
            extras.putSerializable(KanjiEntryDetail.EXTRA_KANJI_ENTRY, entry);

            Intent intent = new Intent(this, KanjiEntryDetail.class);
            intent.putExtras(extras);

            startActivity(intent);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (dualPane) {
            candidatesFragment.loadCurrentKanji();
        }
    }

}
