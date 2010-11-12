package org.nick.wwwjdic.history.gdocs;

import com.google.api.client.googleapis.GoogleUrl;

public class DocsUrl extends GoogleUrl {

    public static final String ROOT_URL = "https://docs.google.com/feeds";

    public DocsUrl(String url) {
        super(url);
        // if (Debug.ENABLED) {
        this.prettyprint = false;
        // }
    }

    private static DocsUrl forRoot() {
        return new DocsUrl(ROOT_URL);
    }

    private static DocsUrl forDefault() {
        DocsUrl result = forRoot();
        result.pathParts.add("default");
        return result;
    }

    public static DocsUrl forDefaultPrivateFull() {
        DocsUrl result = forDefault();
        result.pathParts.add("private");
        result.pathParts.add("full");
        return result;
    }
}
