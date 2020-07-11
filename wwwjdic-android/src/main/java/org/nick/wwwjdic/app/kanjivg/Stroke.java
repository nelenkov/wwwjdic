package org.nick.wwwjdic.app.kanjivg;

import java.io.Serializable;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class Stroke implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Index
    private int number;

    private String type;

    private String path;

    @Parent
    private Key<Kanji> kanji;

    public Stroke() {
    }

    public Stroke(int number, String type, String path) {
        this.number = number;
        this.type = type;
        this.path = path;
    }

    public Long getId() {
        return id;
    }

    public Key<Kanji> getKanji() {
        return kanji;
    }

    public void setKanji(Key<Kanji> kanji) {
        this.kanji = kanji;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String toString() {
        return String.format("%d|%s|%s", number, type, path);
    }

}
