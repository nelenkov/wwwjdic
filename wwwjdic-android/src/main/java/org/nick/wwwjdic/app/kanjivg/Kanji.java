package org.nick.wwwjdic.app.kanjivg;

import com.googlecode.objectify.annotation.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Cache
public class Kanji implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Index
    private String midashi;

    @Index
    private String unicodeNumber;

    @Ignore
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
