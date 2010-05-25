package org.nick.wwwjdic;

import android.app.Application;
import android.app.ProgressDialog;

public class WwwjdicApplication extends Application {

    private ProgressDialog progressDialog;
    private String progressDialogMessage;

    public ProgressDialog getProgressDialog() {
        return progressDialog;
    }

    public void setProgressDialog(ProgressDialog progressDialog) {
        this.progressDialog = progressDialog;
    }

    public String getProgressDialogMessage() {
        return progressDialogMessage;
    }

    public void setProgressDialogMessage(String progressDialogMessage) {
        this.progressDialogMessage = progressDialogMessage;
    }

}
