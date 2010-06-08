package org.nick.wwwjdic.app.kanjivg;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Key;

@PersistenceCapable
public class GzipBlob {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

    @Persistent
    private String name;

    @Persistent
    private int totalKanjis;

    @Persistent
    private int numProcessed;

    @Persistent
    private Blob data;

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
