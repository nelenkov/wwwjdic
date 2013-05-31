package org.nick.wwwjdic.utils;

import java.io.File;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

@SuppressLint("NewApi")
public class MediaScannerWrapper {

    private MediaScannerWrapper() {
    }

    public static void scanFile(Context ctx, String filename) {
        // MediaScanner.scanFile is leaking connections, so use a broadcast 
        // instead
        Intent mediaScannerIntent = new Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri fileContentUri = Uri.fromFile(new File(filename));
        mediaScannerIntent.setData(fileContentUri);
        ctx.sendBroadcast(mediaScannerIntent);
    }
}
