package org.nick.wwwjdic.history.gdocs;

import java.util.List;

import com.google.api.client.util.Key;

public class Entry {
    @Key("@gd:etag")
    public String etag;

    @Key("link")
    public List<Link> links;

    @Key
    public String summary;

    @Key
    public String title;

    @Key
    public String updated;

}
