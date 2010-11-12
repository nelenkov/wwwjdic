package org.nick.wwwjdic.history.gdocs;

import java.io.IOException;
import java.util.List;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.Key;

public class Feed {
    @Key("openSearch:totalResults")
    public int totalResults;

    @Key("link")
    public List<Link> links;

    static Feed executeGet(HttpTransport transport, DocsUrl url,
            Class<? extends Feed> feedClass) throws IOException {
        HttpRequest request = transport.buildGetRequest();
        request.url = url;
        return request.execute().parseAs(feedClass);
    }

}
