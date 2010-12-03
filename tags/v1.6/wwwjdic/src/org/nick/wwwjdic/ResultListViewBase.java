package org.nick.wwwjdic;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.nick.wwwjdic.history.HistoryDbHelper;
import org.nick.wwwjdic.utils.Analytics;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

public abstract class ResultListViewBase<T> extends ListActivity implements
        ResultListView<T> {

    private static final String TAG = ResultListViewBase.class.getSimpleName();

    protected SearchCriteria criteria;

    protected Handler guiThread;
    protected Future<?> transPending;

    protected ProgressDialog progressDialog;

    private HistoryDbHelper db;

    protected ResultListViewBase() {
        guiThread = new Handler();
        db = new HistoryDbHelper(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Analytics.startSession(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Analytics.endSession(this);
    }

    @Override
    protected void onDestroy() {
        if (transPending != null) {
            transPending.cancel(true);
        }

        if (db != null) {
            db.close();
        }

        super.onDestroy();
    }

    protected void submitSearchTask(SearchTask<T> searchTask) {
        progressDialog = ProgressDialog.show(this, "", getResources().getText(
                R.string.loading).toString(), true);

        ExecutorService executorService = getApp().getExecutorService();
        transPending = executorService.submit(searchTask);
    }

    private WwwjdicApplication getApp() {
        WwwjdicApplication app = (WwwjdicApplication) getApplication();
        return app;
    }

    public void setError(final Exception ex) {
        guiThread.post(new Runnable() {
            public void run() {
                setTitle(getResources().getText(R.string.error));
                dismissProgressDialog();

                AlertDialog.Builder alert = new AlertDialog.Builder(
                        ResultListViewBase.this);

                alert.setTitle(R.string.error);

                if (ex instanceof SocketTimeoutException
                        || ex.getCause() instanceof SocketTimeoutException) {
                    alert.setMessage(getResources().getString(
                            R.string.timeout_error_message));
                } else if (ex instanceof SocketException
                        || ex.getCause() instanceof SocketException) {
                    alert.setMessage(getResources().getString(
                            R.string.socket_error_message));
                } else {
                    alert.setMessage(getResources().getString(
                            R.string.generic_error_message)
                            + "(" + ex.getMessage() + ")");
                }

                alert.setPositiveButton(getResources().getText(R.string.ok),
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

    public abstract void setResult(List<T> result);

    protected void extractSearchCriteria() {
        criteria = (SearchCriteria) getIntent().getSerializableExtra(
                Constants.CRITERIA_KEY);
        Log.d(TAG, "query string: " + criteria.getQueryString());
    }

    protected String getWwwjdicUrl() {
        return WwwjdicPreferences.getWwwjdicUrl(this);
    }

    protected int getHttpTimeoutSeconds() {
        return WwwjdicPreferences.getWwwjdicTimeoutSeconds(this);
    }

    protected void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    protected void setFavoriteId(Intent intent, WwwjdicEntry entry) {
        Long favoriteId = db.getFavoriteId(entry.getHeadword());
        if (favoriteId != null) {
            intent.putExtra(Constants.IS_FAVORITE, true);
            entry.setId(favoriteId);
        }
    }
}
