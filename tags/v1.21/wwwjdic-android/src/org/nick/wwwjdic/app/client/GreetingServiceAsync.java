package org.nick.wwwjdic.app.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>GreetingService</code>.
 */
public interface GreetingServiceAsync {
    void greetServer(String input, AsyncCallback<String> callback)
            throws IllegalArgumentException;

    void findKanji(String unicodeNumber, AsyncCallback<String> callback);

    void downloadKanjiVg(String url, AsyncCallback<Integer> callback);

    void processKanjiVg(AsyncCallback<Integer> callback);
}
