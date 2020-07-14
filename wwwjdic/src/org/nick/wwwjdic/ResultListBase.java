package org.nick.wwwjdic;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

import org.nick.wwwjdic.history.FavoritesAndHistory;

public abstract class ResultListBase extends ActionBarActivity {

    private static final String TAG = ResultListBase.class.getSimpleName();

    public static final String EXTRA_RESULT_LIST_PARENT = "org.nick.wwwjdic.resultListParent";

    public enum Parent {
        MAIN,
        FAVORITES,
        HISTORY
    }

    protected ResultListBase() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public Intent getParentActivityIntent () {
        Log.d(TAG, "getParentActivityIntent");
        int parentIdx = getIntent().getIntExtra(EXTRA_RESULT_LIST_PARENT, 0);
        ResultListBase.Parent parent = ResultListBase.Parent.values()[parentIdx];
        Log.d(TAG, "parent " + parent);

        switch (parent) {
            case MAIN:
                return getHomeIntent();
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
