package org.nick.wwwjdic;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;

public abstract class WebServiceBackedActivity extends Activity {

    public static abstract class WsResultHandler extends Handler {

        protected Activity activity;

        public WsResultHandler(Activity krActivity) {
            this.activity = krActivity;
        }

        public Activity getActivity() {
            return activity;
        }

        public void setActivity(Activity activity) {
            this.activity = activity;
        }

    }

    protected Future activeWsRequest;
    protected WsResultHandler handler;
    protected ProgressDialog progressDialog;

    private String progressDialogMessage;

    public WebServiceBackedActivity() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initThreading();
        activityOnCreate(savedInstanceState);
    }

    @Override
    protected void onPause() {
        WwwjdicApplication app = getApp();
        if (progressDialog != null && progressDialog.isShowing()) {
            app.setProgressDialogMessage(progressDialogMessage);
            progressDialog.dismiss();
            progressDialog = null;
        }

        handler.setActivity(null);
        app.setWsResultHandler(handler);

        super.onPause();
    }

    @Override
    protected void onResume() {
        WwwjdicApplication app = getApp();
        progressDialogMessage = app.getProgressDialogMessage();
        if (progressDialogMessage != null) {
            app.setProgressDialogMessage(null);
            progressDialog = ProgressDialog.show(this, "",
                    progressDialogMessage, true);
        }

        handler = app.getWsResultHandler();
        handler.setActivity(this);

        super.onResume();
    }

    protected abstract void activityOnCreate(Bundle savedInstanceState);

    private void initThreading() {
        handler = createHandler();
    }

    protected abstract WsResultHandler createHandler();

    protected void submitWsTask(Runnable task, String message) {
        progressDialogMessage = message;

        progressDialog = ProgressDialog.show(this, "", message, true);
        ExecutorService executorService = getApp().getExecutorService();
        activeWsRequest = executorService.submit(task);
    }

    public void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
            WwwjdicApplication app = getApp();
            app.setProgressDialogMessage(null);
        }
    }

    private WwwjdicApplication getApp() {
        WwwjdicApplication app = (WwwjdicApplication) getApplication();
        return app;
    }

}
