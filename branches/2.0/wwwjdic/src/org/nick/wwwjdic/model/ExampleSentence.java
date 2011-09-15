package org.nick.wwwjdic.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ExampleSentence implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1017016303787281524L;

    private String japanese;
    private String english;

    private List<String> matches = new ArrayList<String>();

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

    public List<String> getMatches() {
        return matches;
    }

    public void addMatch(String match) {
        matches.add(match);
    }

}
