package org.nick.wwwjdic;

import java.util.List;

public interface ResultList<T> {

    void setResult(final List<T> result);

    void setError(final Exception ex);
}
