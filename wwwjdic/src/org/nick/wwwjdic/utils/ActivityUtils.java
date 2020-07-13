package org.nick.wwwjdic.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicApplication;

import java.io.File;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

public class ActivityUtils {

    private static final String NOTIFICATION_CHANNEL_ID = "wwwjdic-01";
    private static final String NOTIFICATION_CHANNEL_NAME = "WWWJDIC";

    private ActivityUtils() {
    }

    public static @NonNull
    Intent createShareFileIntent(@NonNull Context ctx, @NonNull String filename,
                                 @NonNull String mimeType) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, getShareableUriForfile(ctx, filename));

        return Intent.createChooser(intent, (ctx.getString(R.string.share)));
    }

    public static @NonNull
    Intent createOpenIntent(@NonNull Context ctx, @NonNull String filename, @NonNull String mimeType) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(getShareableUriForfile(ctx, filename), mimeType);

        return Intent.createChooser(intent, ctx.getString(R.string.open));
    }

    public static @NonNull
    Uri getShareableUriForfile(@NonNull Context ctx, @NonNull String filename) {
        File file = new File(filename);
        Uri uri;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            uri = Uri.fromFile(file);
        } else {
            String authority = ctx.getApplicationContext().getPackageName() + ".fileprovider";
            uri = FileProvider.getUriForFile(ctx, authority, file);
        }
        return uri;
    }

    public static void showOpenShareNotification(@NonNull Context ctx,
                                                 int notificationId,
                                                 @NonNull Intent intent,
                                                 @NonNull String title,
                                                 @NonNull String message,
                                                 @DrawableRes int smallIconRes, boolean open) {
        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        // clear current notification
        notificationManager.cancel(notificationId);

        Context appCtx = WwwjdicApplication.getInstance();
        PendingIntent pendingIntent = PendingIntent.getActivity(appCtx, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appCtx, NOTIFICATION_CHANNEL_ID);
        builder.setSmallIcon(smallIconRes).setContentTitle(title).setContentText(message);
        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle(builder);
        style.bigText(message).setBigContentTitle(title);
        builder.setStyle(style);
        builder.setContentIntent(pendingIntent);
        builder.setWhen(System.currentTimeMillis());
        builder.setDefaults(Notification.DEFAULT_LIGHTS);
        builder.setAutoCancel(true).setOngoing(false);

        if (open) {
            builder.addAction(android.R.drawable.ic_menu_view,
                    appCtx.getString(R.string.open), pendingIntent);
        } else {
            builder.addAction(android.R.drawable.ic_menu_share,
                    appCtx.getString(R.string.share), pendingIntent);
        }

        notificationManager.notify(notificationId, builder.build());
    }

    public static @NonNull
    NotificationCompat.Builder createNotification(@NonNull Context ctx,
                                                  @NonNull PendingIntent pendingIntent,
                                                  @NonNull String title,
                                                  @NonNull String message,
                                                  @DrawableRes int smallIconRes) {
        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, NOTIFICATION_CHANNEL_ID);
        builder.setSmallIcon(smallIconRes);
        builder.setContentTitle(title).setContentText(message);
        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle(builder);
        style.bigText(message).setBigContentTitle(title);
        builder.setStyle(style);
        builder.setContentIntent(pendingIntent);
        builder.setWhen(System.currentTimeMillis());
        builder.setDefaults(Notification.DEFAULT_LIGHTS);
        builder.setAutoCancel(true).setOngoing(true);

        return builder;
    }

}
