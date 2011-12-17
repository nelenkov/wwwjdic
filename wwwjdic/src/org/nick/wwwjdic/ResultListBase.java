package org.nick.wwwjdic;

import org.nick.wwwjdic.utils.Analytics;

import android.media.AudioManager;
import android.os.Bundle;

public abstract class ResultListBase extends ActionBarActivity {

    protected ResultListBase() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Analytics.startSession(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Analytics.endSession(this);
    }

}
