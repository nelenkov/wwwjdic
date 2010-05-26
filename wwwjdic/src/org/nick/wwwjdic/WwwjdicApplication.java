package org.nick.wwwjdic;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Application;

public class WwwjdicApplication extends Application {

    private ExecutorService executorService;

    public WwwjdicApplication() {
        executorService = Executors.newSingleThreadExecutor();
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

}
