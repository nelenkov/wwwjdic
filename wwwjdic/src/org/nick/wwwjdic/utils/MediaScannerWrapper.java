package org.nick.wwwjdic.utils;

import android.content.Context;
import android.media.MediaScannerConnection;

public class MediaScannerWrapper {

    private MediaScannerWrapper() {
    }

    public static void scanFile(Context ctx, String filename) {
        MediaScannerConnection.scanFile(ctx, new String[] { filename }, null,
                null);
    }
}
