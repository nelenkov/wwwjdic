package org.nick.wwwjdic.app.kanjivg;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
public class GzipBlob {

    private Key key;

    @Id
    private Long id;

    @Index
    private String name;

    private int totalKanjis;

    private int numProcessed;

    private Blob data;

    public GzipBlob() {
    }

    public GzipBlob(String name, byte[] data) {
        this.name = name;
        this.data = new Blob(data);
    }

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public int getNumProcessed() {
        return numProcessed;
    }

    public void setNumProcessed(int numProcessed) {
        this.numProcessed = numProcessed;
    }

    public byte[] getData() {
        return data.getBytes();
    }

    public int getTotalKanjis() {
        return totalKanjis;
    }

    public void setTotalKanjis(int totalKanjis) {
        this.totalKanjis = totalKanjis;
    }

    public boolean isDone() {
        return totalKanjis == numProcessed;
    }

}
