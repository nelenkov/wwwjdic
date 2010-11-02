package org.nick.wwwjdic;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Dialogs {

    private Dialogs() {
    }

    public static Dialog createTipDialog(Context context, int messageId) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(messageId);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                    }
                });
        builder.setTitle(R.string.tip);
        builder.setIcon(android.R.drawable.ic_dialog_info);

        return builder.create();
    }

    public static void showTipOnce(Activity activity, int dialogId,
            String tipKey) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(activity);
        String key = Constants.PREF_TIP_SHOWN + "_" + tipKey;
        boolean tipShown = prefs.getBoolean(key, false);
        if (!tipShown) {
            prefs.edit().putBoolean(key, true).commit();
            activity.showDialog(dialogId);
        }
    }
}
