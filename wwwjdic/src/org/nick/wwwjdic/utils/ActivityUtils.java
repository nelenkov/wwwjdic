package org.nick.wwwjdic.utils;

import java.io.File;

import org.nick.wwwjdic.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class ActivityUtils {

    private ActivityUtils() {
    }

    public static Intent createShareFileIntent(Context ctx, String filename,
            String mimeType) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        File file = new File(filename);
        intent.putExtra(Intent.EXTRA_STREAM,
                android.net.Uri.parse(file.getAbsolutePath()));

        return Intent.createChooser(intent, (ctx.getString(R.string.share)));
    }

    @SuppressLint("InlinedApi")
    public static Intent createOpenIntent(Context ctx, String filename,
            String mimeType) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.fromFile(new File(filename)), mimeType);

        return Intent.createChooser(intent, ctx.getString(R.string.open));
    }
}
