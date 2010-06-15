package org.nick.wwwjdic;

import java.io.Serializable;

public abstract class WwwjdicEntry implements Serializable {

    private static final long serialVersionUID = 8688955395298491589L;

    protected Long id;
    protected String dictString;

    protected WwwjdicEntry(String dictString) {
        this.dictString = dictString;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDictString() {
        return dictString;
    }

    public abstract String getHeadword();

    public abstract boolean isKanji();

    public abstract String getDetailString();

}
