package org.nick.wwwjdic;

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

}
