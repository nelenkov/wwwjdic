package org.nick.wwwjdic;

import java.io.Serializable;

public class WwwjdicQuery implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -5227778690344003552L;

    protected String queryString;

    public WwwjdicQuery(String queryString) {
        this.queryString = queryString;
    }

    public String getQueryString() {
        return queryString;
    }

}
