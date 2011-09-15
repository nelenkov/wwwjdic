package org.nick.wwwjdic.model;

import java.io.Serializable;

public class Radical implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private int number;
    private String glyph;
    private int numStrokes;

    public Radical(int number, String glyph, int numStrokes) {
        this.number = number;
        this.glyph = glyph;
        this.numStrokes = numStrokes;
    }

    public int getNumber() {
        return number;
    }

    public String getGlyph() {
        return glyph;
    }

    public int getNumStrokes() {
        return numStrokes;
    }

}
