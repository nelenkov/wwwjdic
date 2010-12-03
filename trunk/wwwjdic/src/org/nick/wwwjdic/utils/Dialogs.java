package org.nick.wwwjdic.utils;

import org.nick.wwwjdic.R;
import org.nick.wwwjdic.WwwjdicPreferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;

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

    public static void showTipOnce(Activity activity, String tipKey,
            int messageId) {
        boolean tipShown = WwwjdicPreferences.isTipShown(activity, tipKey);
        if (!tipShown) {
            WwwjdicPreferences.setTipShown(activity, tipKey);

            Dialog dialog = createTipDialog(activity, messageId);
            dialog.show();
        }
    }
}
