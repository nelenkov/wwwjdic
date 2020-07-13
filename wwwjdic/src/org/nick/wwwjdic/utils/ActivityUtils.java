package org.nick.wwwjdic.utils;

import java.io.File;

import org.nick.wwwjdic.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

public class ActivityUtils {

    private ActivityUtils() {
    }

    public static Intent createShareFileIntent(Context ctx, String filename,
            String mimeType) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        File file = new File(filename);

        Uri uri;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            uri = Uri.fromFile(file);
        } else {
            String authority = ctx.getApplicationContext().getPackageName() + ".fileprovider";
            uri = FileProvider.getUriForFile(ctx.getApplicationContext(), authority, file);
        }
        intent.putExtra(Intent.EXTRA_STREAM, uri);

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
