package org.nick.wwwjdic.utils;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

public abstract class LoaderBase<T> extends AsyncTaskLoader<T> {

    private static final String TAG = LoaderBase.class.getSimpleName();

    protected T lastResult;
    protected Exception error;

    protected LoaderBase(Context context) {
        super(context);
    }

    @Override
    public void deliverResult(T result) {
        if (isReset()) {
            if (result != null) {
                releaseResult(result);
            }
            return;
        }

        T oldResult = lastResult;
        lastResult = result;

        if (isStarted()) {
            super.deliverResult(result);
        }

        if (oldResult != null && oldResult != result && isActive(result)) {
            releaseResult(oldResult);
        }
    }

    @Override
    protected void onStartLoading() {
        if (lastResult != null) {
            deliverResult(lastResult);
        }

        if (takeContentChanged() || lastResult == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void onCanceled(T result) {
        super.onCanceled(result);

        if (result != null && isActive(result)) {
            releaseResult(result);
        }
    }

    @Override
    protected void onReset() {
        super.onReset();

        onStopLoading();

        if (lastResult != null && isActive(lastResult)) {
            releaseResult(lastResult);
        }
        lastResult = null;
    }

    @Override
    public T loadInBackground() {
        try {
            return load();
        } catch (Exception e) {
            Log.e(TAG, "Error loading data: " + e.getMessage(), e);
            error = e;

            return null;
        }
    }

    protected abstract T load() throws Exception;

    protected abstract void releaseResult(T result);

    protected abstract boolean isActive(T result);

    public Exception getError() {
        return error;
    }

    public boolean isSuccessful() {
        return error == null;
    }

    public boolean isFailed() {
        return !isSuccessful();
    }
}
