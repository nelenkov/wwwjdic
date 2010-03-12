package org.nick.wwwjdic;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;

public abstract class ResultListViewBase extends ListActivity implements
        ResultListView {

    protected SearchCriteria criteria;

    protected Handler guiThread;
    protected ExecutorService transThread;
    protected Future transPending;

    protected ProgressDialog progressDialog;

    protected ResultListViewBase() {
        initThreading();
    }

    @Override
    protected void onDestroy() {
        transThread.shutdownNow();
        super.onDestroy();
    }

    private void initThreading() {
        guiThread = new Handler();
        transThread = Executors.newSingleThreadExecutor();
    }

    protected void submitTranslateTask(TranslateTask translateTask) {
        progressDialog = ProgressDialog.show(this, "",
                "Loading. Please wait...", true);
        transPending = transThread.submit(translateTask);
    }

    public void setError(final Exception ex) {
        guiThread.post(new Runnable() {
            public void run() {
                setTitle("Error");
                progressDialog.dismiss();

                AlertDialog.Builder alert = new AlertDialog.Builder(
                        ResultListViewBase.this);

                alert.setTitle("Error");
                alert.setMessage(ex.getMessage());

                alert.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                dialog.dismiss();
                                finish();
                            }
                        });

                alert.show();
            }
        });
    }

    public abstract void setResult(List<?> result);

    protected void extractSearchCriteria() {
        criteria = (SearchCriteria) getIntent().getSerializableExtra(
                "org.nick.hello.searchCriteria");
    }

}
