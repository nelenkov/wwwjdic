package org.nick.wwwjdic.model;

import java.io.Serializable;
import java.util.List;

public abstract class WwwjdicEntry implements Serializable {

    private static final long serialVersionUID = 8688955395298491589L;

    protected Long id;
    protected String dictString;
    protected String dictionary;

    protected WwwjdicEntry(String dictString) {
        this.dictString = dictString;
    }

    protected WwwjdicEntry(String dictString, String dictionary) {
        this.dictString = dictString;
        this.dictionary = dictionary;
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

    public abstract String getReading();

    public abstract boolean isKanji();

    public abstract String getDetailString();

    public abstract List<String> getMeanings();

    public boolean isSingleKanji() {
        return getHeadword().length() == 1;
    }

    public String getDictionary() {
        return dictionary;
    }

    public void setDictionary(String dictionary) {
        this.dictionary = dictionary;
    }

}
