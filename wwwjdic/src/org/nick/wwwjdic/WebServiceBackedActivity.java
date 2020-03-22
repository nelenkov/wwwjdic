package org.nick.wwwjdic;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import androidx.appcompat.app.AppCompatActivity;

public abstract class WebServiceBackedActivity extends AppCompatActivity {

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

    protected Future<?> activeWsRequest;
    protected WsResultHandler handler;
    protected ProgressDialog progressDialog;

    public WebServiceBackedActivity() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = createHandler();
        activityOnCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        if (activeWsRequest != null) {
            activeWsRequest.cancel(true);
        }

        activityOnDestroy();

        super.onDestroy();
    }

    protected abstract void activityOnCreate(Bundle savedInstanceState);

    protected void activityOnDestroy() {
    }

    protected abstract WsResultHandler createHandler();

    protected void submitWsTask(Runnable task, String message) {
        showProgressDialog(message);
        ExecutorService executorService = getApp().getExecutorService();
        activeWsRequest = executorService.submit(task);
    }

    public void showProgressDialog(String message) {
        progressDialog = ProgressDialog.show(this, "", message, true);
        progressDialog.setCancelable(true);
    }

    public void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()
                && !isFinishing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    protected WwwjdicApplication getApp() {
        return WwwjdicApplication.getInstance();
    }

}
