package org.nick.wwwjdic;

import org.nick.wwwjdic.history.HistoryDbHelper;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public abstract class DetailActivity extends FragmentActivity {

    public static final String EXTRA_IS_FAVORITE = "org.nick.wwwjdic.IS_FAVORITE";

    protected HistoryDbHelper db;
    protected WwwjdicEntry wwwjdicEntry;
    protected boolean isFavorite;

    protected DetailActivity() {
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        isFavorite = getIntent().getBooleanExtra(EXTRA_IS_FAVORITE, false);
        db = HistoryDbHelper.getInstance(this);
    }

    protected abstract void setHomeActivityExtras(Intent homeActivityIntent);

}
