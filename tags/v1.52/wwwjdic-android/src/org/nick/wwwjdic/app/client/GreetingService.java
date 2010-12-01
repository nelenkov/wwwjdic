package org.nick.wwwjdic.app.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("greet")
public interface GreetingService extends RemoteService {
    String greetServer(String name) throws IllegalArgumentException;

    String findKanji(String unicodeNumber);

    int downloadKanjiVg(String url);

    int processKanjiVg();
}
