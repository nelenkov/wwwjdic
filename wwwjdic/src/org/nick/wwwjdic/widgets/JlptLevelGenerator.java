package org.nick.wwwjdic.widgets;

import java.util.Arrays;
import java.util.Random;

import org.nick.wwwjdic.model.JlptLevels;

public class JlptLevelGenerator implements KanjiGenerator {

    private Random random = new Random();

    private boolean isRandom;
    private int jlptLevel;
    private String currentKanji;

    public JlptLevelGenerator(boolean isRandom, int jlptLevel) {
        this.isRandom = isRandom;
        this.jlptLevel = jlptLevel;
    }

    @Override
    public void setCurrentKanji(String kanji) {
        currentKanji = kanji;
    }

    @Override
    public String selectNextUnicodeCp() {
        String kanji = selectNextKanji();
        int unicodeCp = (int) kanji.toCharArray()[0];

        return Integer.toString(unicodeCp, 16);
    }

    @Override
    public String selectNextKanji() {
        String[] kanjis = JlptLevels.getKanjiForLevel(jlptLevel);

        String kanji = null;
        if (isRandom) {
            kanji = kanjis[random.nextInt(kanjis.length)];
        } else {
            if (currentKanji == null) {
                kanji = kanjis[0];
                currentKanji = kanji;
            } else {
                int idx = Arrays.asList(kanjis).indexOf(currentKanji);
                if (idx == -1) {
                    kanji = kanjis[0];
                } else {
                    if (idx + 1 >= kanjis.length) {
                        idx = 0;
                        kanji = kanjis[idx];
                    } else {
                        kanji = kanjis[++idx];
                    }
                }
                currentKanji = kanji;
            }
        }
        return kanji;
    }

}
