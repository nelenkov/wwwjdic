package org.nick.wwwjdic.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicPreferences;

public class Dialogs {

    private Dialogs() {
    }

    public static Dialog createTipDialog(Context context, int messageId) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(messageId);
        builder.setTitle(R.string.tip);
        builder.setIcon(android.R.drawable.ic_dialog_info);

        return builder.create();
    }

    public static Dialog createErrorDialog(Context context, int messageId) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(messageId).setTitle(R.string.error)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());

        return builder.create();
    }

    public static void showTipOnce(Activity activity, String tipKey,
            int messageId) {
        boolean tipShown = WwwjdicPreferences.isTipShown(activity, tipKey);
        if (!tipShown) {
            WwwjdicPreferences.setTipShown(activity, tipKey);

            Dialog dialog = createTipDialog(activity, messageId);
            dialog.show();
        }
    }

    public static Dialog createFinishActivityAlertDialog(
            final Activity activity, int titleId, int messageId) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(messageId);
        builder.setTitle(titleId);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setPositiveButton(R.string.ok,
            (dialog, which) -> activity.finish());
        builder.setCancelable(false);

        return builder.create();
    }
}
