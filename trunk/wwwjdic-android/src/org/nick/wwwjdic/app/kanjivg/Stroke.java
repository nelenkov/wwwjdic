package org.nick.wwwjdic.app.kanjivg;

import java.io.Serializable;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

@PersistenceCapable
public class Stroke implements Serializable {


    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

    @Persistent
    private int number;

    @Persistent
    private String type;

    @Persistent
    private String path;

    public Stroke() {
    }

    public Stroke(int number, String type, String path) {
        this.number = number;
        this.type = type;
        this.path = path;
    }

    public Key getKey() {
        return key;
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
