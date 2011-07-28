package org.nick.wwwjdic;

import java.io.Serializable;

public class ExampleSentence implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1017016303787281524L;

    private String japanese;
    private String english;

    public ExampleSentence(String japanese, String english) {
        this.japanese = japanese;
        this.english = english;
    }

    public String getJapanese() {
        return japanese;
    }

    public String getEnglish() {
        return english;
    }

}
