package org.nick.wwwjdic.widgets;

public interface KanjiGenerator {

    void setCurrentKanji(String kanji);

    String selectNextUnicodeCp();
}
