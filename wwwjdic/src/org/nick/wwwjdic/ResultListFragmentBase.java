package org.nick.wwwjdic;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.nick.wwwjdic.history.HistoryDbHelper;
import org.nick.wwwjdic.utils.Analytics;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.text.ClipboardManager;
import android.util.Log;
import android.widget.Toast;

public abstract class ResultListFragmentBase<T> extends ListFragment implements
        ResultListView<T> {

    private static final String TAG = ResultListFragmentBase.class.getSimpleName();

    protected SearchCriteria criteria;

    protected Handler guiThread;
    protected Future<?> transPending;

    protected ProgressDialog progressDialog;

    protected HistoryDbHelper db;

    protected ResultListFragmentBase() {
        guiThread = new Handler();
        db = HistoryDbHelper.getInstance(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();

        Analytics.startSession(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();

        Analytics.endSession(getActivity());
    }

    @Override
    public void onDestroy() {
        if (transPending != null) {
            transPending.cancel(true);
        }

        super.onDestroy();
    }

    protected void submitSearchTask(SearchTask<T> searchTask) {
        progressDialog = ProgressDialog.show(getActivity(), "", getResources()
                .getText(R.string.loading).toString(), true);

        ExecutorService executorService = getApp().getExecutorService();
        transPending = executorService.submit(searchTask);
    }

    protected WwwjdicApplication getApp() {
        WwwjdicApplication app = (WwwjdicApplication) getActivity()
                .getApplication();
        return app;
    }

    public void setError(final Exception ex) {
        if (getActivity().isFinishing()) {
            return;
        }

        guiThread.post(new Runnable() {
            public void run() {
                getActivity().setTitle(getResources().getText(R.string.error));
                dismissProgressDialog();

                AlertDialog.Builder alert = new AlertDialog.Builder(
                        getActivity());

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
                                getActivity().finish();
                            }
                        });

                alert.show();
            }
        });
    }

    public abstract void setResult(List<T> result);

    protected void extractSearchCriteria() {
        criteria = (SearchCriteria) getActivity().getIntent()
                .getSerializableExtra(Constants.CRITERIA_KEY);
        if (criteria != null) {
            Log.d(TAG, "query string: " + criteria.getQueryString());
        }
    }

    protected String getWwwjdicUrl() {
        return WwwjdicPreferences.getWwwjdicUrl(getActivity());
    }

    protected int getHttpTimeoutSeconds() {
        return WwwjdicPreferences.getWwwjdicTimeoutSeconds(getActivity());
    }

    protected void dismissProgressDialog() {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        } catch (Throwable t) {
            Log.w(TAG, "error dismissing progress dialog: " + t.getMessage(), t);
        }
    }

    protected void setFavoriteId(Intent intent, WwwjdicEntry entry) {
        Long favoriteId = db.getFavoriteId(entry.getHeadword());
        if (favoriteId != null) {
            intent.putExtra(Constants.IS_FAVORITE, true);
            entry.setId(favoriteId);
        }
    }

    protected void copy(WwwjdicEntry entry) {
        String headword = entry.getHeadword();
        ClipboardManager clipboard = (ClipboardManager) getActivity()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setText(headword);
        String message = getResources().getString(R.string.copied_to_clipboard,
                headword);
        Toast t = Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT);
        t.show();
    }

    protected void addToFavorites(WwwjdicEntry entry) {
        db.addFavorite(entry);
        String message = getResources().getString(R.string.added_to_favorites,
                entry.getHeadword());
        Toast t = Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT);
        t.show();
    }

}
