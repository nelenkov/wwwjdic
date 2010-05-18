package org.nick.wwwjdic;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;

public abstract class WebServiceBackedActivity extends Activity {

    protected ExecutorService executorService;
    protected Future activeWsRequest;
    protected Handler handler;
    protected ProgressDialog progressDialog;

    public WebServiceBackedActivity() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initThreading();
        activityOnCreate(savedInstanceState);
    }

    protected abstract void activityOnCreate(Bundle savedInstanceState);

    private void initThreading() {
        handler = createHandler();
        executorService = Executors.newSingleThreadExecutor();
    }

    protected abstract Handler createHandler();

    protected void submitWsTask(Runnable task, String message) {
        progressDialog = ProgressDialog.show(this, "", message, true);
        activeWsRequest = executorService.submit(task);
    }

}
