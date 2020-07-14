package org.nick.wwwjdic;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.util.Log;
import android.widget.Toast;

import org.nick.wwwjdic.history.FavoritesAndHistory;
import org.nick.wwwjdic.history.HistoryDbHelper;
import org.nick.wwwjdic.hkr.HkrCandidates;
import org.nick.wwwjdic.model.WwwjdicEntry;

public abstract class DetailActivity extends ActionBarActivity {

    private static final String TAG = DetailActivity.class.getSimpleName();


    public static final String EXTRA_IS_FAVORITE = "org.nick.wwwjdic.IS_FAVORITE";

    protected HistoryDbHelper db;
    protected WwwjdicEntry wwwjdicEntry;
    protected boolean isFavorite;

    public static final String EXTRA_DETAILS_PARENT = "org.nick.wwwjdic.detailsParent";

    public enum Parent {
        MAIN,
        DICT_CANDIDATES,
        KANJI_CANDIDATES,
        HKR_CANDIDATES,
        EXAMPLE_CANDIDATES,
        HISTORY,
        FAVORITES,
    }

    protected DetailActivity() {
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        isFavorite = getIntent().getBooleanExtra(EXTRA_IS_FAVORITE, false);
        db = HistoryDbHelper.getInstance(this);
    }


    protected void copy() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setText(wwwjdicEntry.getHeadword());
        String messageTemplate = getResources().getString(
                R.string.copied_to_clipboard);
        Toast.makeText(this,
                String.format(messageTemplate, wwwjdicEntry.getHeadword()),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public Intent getParentActivityIntent () {
        Log.d(TAG, "getParentActivityIntent");
        int parentIdx = getIntent().getIntExtra(DetailActivity.EXTRA_DETAILS_PARENT, 0);
        DetailActivity.Parent parent = DetailActivity.Parent.values()[parentIdx];
        Log.d(TAG, "parent " + parent);

        switch (parent) {
            case MAIN:
                return getHomeIntent();
            case DICT_CANDIDATES:
                return new Intent(this, DictionaryResultList.class);
            case KANJI_CANDIDATES:
                return new Intent(this, KanjiResultList.class);
            case EXAMPLE_CANDIDATES:
                return new Intent(this, ExamplesResultList.class);
            case HKR_CANDIDATES:
                return new Intent(this, HkrCandidates.class);
            case FAVORITES: {
                Intent intent = new Intent(this, FavoritesAndHistory.class);
                intent.putExtra(FavoritesAndHistory.EXTRA_SELECTED_TAB_IDX,
                        FavoritesAndHistory.FAVORITES_TAB_IDX);
                return intent;
            }
            case HISTORY: {
                Intent intent = new Intent(this, FavoritesAndHistory.class);
                intent.putExtra(FavoritesAndHistory.EXTRA_SELECTED_TAB_IDX,
                        FavoritesAndHistory.HISTORY_TAB_IDX);
                return intent;
            }
            default:
                return  super.getParentActivityIntent();

        }
    }

}
