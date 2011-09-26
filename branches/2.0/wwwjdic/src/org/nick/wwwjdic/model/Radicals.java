package org.nick.wwwjdic.model;

import java.util.ArrayList;
import java.util.List;

public class Radicals {

    private final List<Radical> radicals = new ArrayList<Radical>();
    private static final Radicals instance = new Radicals();

    private Radicals() {
    }

    public static Radicals getInstance() {
        return instance;
    }

    public void addRadicals(int strokeCount, int[] radicalNumbers,
            String[] radicalStrs) {
        if (radicalNumbers.length != radicalStrs.length) {
            throw new IllegalArgumentException("arrays length does not match");
        }

        for (int i = 0; i < radicalNumbers.length; i++) {
            Radical radical = new Radical(radicalNumbers[i], radicalStrs[i],
                    strokeCount);
            radicals.add(radical);
        }
    }

    public boolean isInitialized() {
        return !radicals.isEmpty();
    }

    public List<Radical> getRadicals() {
        return radicals;
    }

    public Radical getRadical(int index) {
        return radicals.get(index);
    }

    public Radical getRadicalByNumber(int radicalNumber) {
        if (radicalNumber > radicals.size() || radicalNumber < 0) {
            return null;
        }

        return radicals.get(radicalNumber - 1);
    }
}
