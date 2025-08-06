package org.nick.wwwjdic.utils;


public class LoaderResult<T> {

    private final T data;
    private final Exception error;

    private LoaderResult(T data, Exception error) {
        this.data = data;
        this.error = error;
    }

    public static <T> LoaderResult<T> create(T data) {
        return new LoaderResult<>(data, null);
    }

    public static <T> LoaderResult<T> createFailed(Exception error) {
        return new LoaderResult<>(null, error);
    }

    public T getData() {
        return data;
    }

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
