package org.nick.wwwjdic.history.gdocs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.Key;

public class DocumentListFeed extends Feed {
    @Key("entry")
    public List<DocumentListEntry> docs = new ArrayList<DocumentListEntry>();

    public static DocumentListFeed executeGet(HttpTransport transport,
            DocsUrl url) throws IOException {
        return (DocumentListFeed) Feed.executeGet(transport, url,
                DocumentListFeed.class);
    }

}
