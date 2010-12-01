package org.nick.wwwjdic.app.kanjivg;

import java.util.ArrayList;
import java.util.List;

import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Order;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.persistence.OneToMany;

import com.google.appengine.api.datastore.Key;

@PersistenceCapable
public class Kanji {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

    @Persistent
    private String midashi;

    @Persistent
    private String unicodeNumber;

    @Persistent
    @Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "number asc"))
    private List<Stroke> strokes = new ArrayList<Stroke>();

    public Kanji(String midashi, String unicodeNumber) {
        this.midashi = midashi;
        this.unicodeNumber = unicodeNumber;
    }

    public Kanji() {
    }

    public Key getKey() {
        return key;
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

    @OneToMany
    public List<Stroke> getStrokes() {
        return strokes;
    }

    public void setStrokes(List<Stroke> strokes) {
        this.strokes = strokes;
    }

}
