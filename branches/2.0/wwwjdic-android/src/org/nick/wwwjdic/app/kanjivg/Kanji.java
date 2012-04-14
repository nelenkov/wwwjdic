package org.nick.wwwjdic.app.kanjivg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Id;

public class Kanji implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    private String midashi;

    private String unicodeNumber;

    private List<Stroke> strokes = new ArrayList<Stroke>();

    public Kanji(String midashi, String unicodeNumber) {
        this.midashi = midashi;
        this.unicodeNumber = unicodeNumber;
    }

    public Kanji() {
    }

    public Long getId() {
        return id;
    }

    public String getMidashi() {
        return midashi;
    }

    public void setMidashi(String midashi) {
        this.midashi = midashi;
    }

    public String getUnicodeNumber() {
        return unicodeNumber;
    }

    public void setUnicodeNumber(String unicodeNumber) {
        this.unicodeNumber = unicodeNumber;
    }

    public List<Stroke> getStrokes() {
        return strokes;
    }

    public void setStrokes(List<Stroke> strokes) {
        this.strokes = strokes;
    }

}
