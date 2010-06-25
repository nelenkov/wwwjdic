package org.nick.wwwjdic;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.nick.wwwjdic.history.HistoryDbHelper;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;

public abstract class ResultListViewBase<T> extends ListActivity implements
        ResultListView<T> {

    private static final String DEFAULT_WWWJDIC_URL = "http://www.csse.monash.edu.au/~jwb/cgi-bin/wwwjdic.cgi";

    private static final String PREF_WWWJDIC_URL_KEY = "pref_wwwjdic_mirror_url";
    private static final String PREF_WWWJDIC_TIMEOUT_KEY = "pref_wwwjdic_timeout";

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
    protected void onDestroy() {
        if (transPending != null) {
            transPending.cancel(true);
        }

        if (db != null) {
            db.close();
        }

        super.onDestroy();
    }

    protected void submitTranslateTask(TranslateTask<T> translateTask) {
        progressDialog = ProgressDialog.show(this, "", getResources().getText(
                R.string.loading).toString(), true);

        ExecutorService executorService = getApp().getExecutorService();
        transPending = executorService.submit(translateTask);
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
    }

    protected String getWwwjdicUrl() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        return preferences.getString(PREF_WWWJDIC_URL_KEY, DEFAULT_WWWJDIC_URL);
    }

    protected int getHttpTimeoutSeconds() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        String timeoutStr = preferences.getString(PREF_WWWJDIC_TIMEOUT_KEY,
                "10");

        return Integer.parseInt(timeoutStr);
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
