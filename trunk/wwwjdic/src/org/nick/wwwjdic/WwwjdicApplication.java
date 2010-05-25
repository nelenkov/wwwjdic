package org.nick.wwwjdic;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.nick.wwwjdic.WebServiceBackedActivity.WsResultHandler;

import android.app.Application;

public class WwwjdicApplication extends Application {

    private String progressDialogMessage;

    private WsResultHandler wsResultHandler;

    private ExecutorService executorService;

    private TranslateTask translateTask;

    public WwwjdicApplication() {
        executorService = Executors.newSingleThreadExecutor();
    }

    public synchronized String getProgressDialogMessage() {
        return progressDialogMessage;
    }

    public synchronized void setProgressDialogMessage(
            String progressDialogMessage) {
        this.progressDialogMessage = progressDialogMessage;
    }

    public synchronized WsResultHandler getWsResultHandler() {
        return wsResultHandler;
    }

    public synchronized void setWsResultHandler(WsResultHandler wsResultHandler) {
        this.wsResultHandler = wsResultHandler;
    }

    public synchronized TranslateTask getTranslateTask() {
        return translateTask;
    }

    public synchronized void setTranslateTask(TranslateTask translateTask) {
        this.translateTask = translateTask;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

}
