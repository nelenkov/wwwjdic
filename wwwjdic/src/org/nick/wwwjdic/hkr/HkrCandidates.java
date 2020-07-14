package org.nick.wwwjdic.hkr;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.nick.wwwjdic.ActionBarActivity;
import org.nick.wwwjdic.DetailActivity;
import org.nick.wwwjdic.KanjiEntryDetail;
import org.nick.wwwjdic.KanjiEntryDetailFragment;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.model.KanjiEntry;

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

        setContentView(R.layout.hkr_candidates);

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
            KanjiEntryDetailFragment details = (KanjiEntryDetailFragment) getFragmentManager()
                    .findFragmentById(R.id.details);
            if (details == null
                    || !details.getEntry().getKanji().equals(entry.getKanji())) {
                details = KanjiEntryDetailFragment.newInstance(position, entry);

                FragmentTransaction ft = getFragmentManager()
                        .beginTransaction();

                ft.replace(R.id.details, details);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commitAllowingStateLoss();
            }
        } else {
            candidatesFragment.getListView().clearChoices();

            Bundle extras = new Bundle();
            extras.putSerializable(KanjiEntryDetail.EXTRA_KANJI_ENTRY, entry);
            extras.putInt(DetailActivity.EXTRA_DETAILS_PARENT,
                    DetailActivity.Parent.HKR_CANDIDATES.ordinal());

            Intent intent = new Intent(this, KanjiEntryDetail.class);
            intent.putExtras(extras);

            startActivity(intent);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (dualPane) {
            if (candidatesFragment.isEmpty()) {
                View details = findViewById(R.id.details);
                if (details != null) {
                    details.setVisibility(View.GONE);
                }
            } else {
                candidatesFragment.loadCurrentKanji();
            }
        }
    }

}
