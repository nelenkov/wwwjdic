package org.nick.wwwjdic.widgets;

import java.util.Random;

public class RandomJisGenerator {

    // Cf. JIS X 0208:
    //
    // * 94x94 grid
    // * kanji: lines 16-83
    // * line 84 -- 6 kanji
    // * raw JIS: the 16-bit code resulting from adding 33 to each
    // JIS coordinate and concatenating the two resulting bytes,
    // the vertical coordinate becoming the high byte. (coords are zero based)

    private static final int JIS_GRID_SIZE = 94;
    // zero based
    private static final int KANJI_START_LINE = 15;
    private static final int KANJI_END_LINE = 83;
    private static final int NUM_KANJI_LAST_LINE = 6;

    private static final int LEVEL1_START_LINE = 16;
    private static final int LEVEL1_END_LINE = 47;

    private static final int OFFSET = 33;

    private Random random = new Random();

    public String generate() {
        return generate(false);
    }

    public String generate(boolean limitToLevelOne) {
        int startLine = KANJI_START_LINE;
        int endLine = KANJI_END_LINE;

        if (limitToLevelOne) {
            startLine = LEVEL1_START_LINE;
            endLine = LEVEL1_END_LINE;
        }
        int line = random.nextInt(endLine - startLine + 1) + startLine;

        int column = 0;
        if (line != KANJI_END_LINE) {
            column = random.nextInt(JIS_GRID_SIZE);
        } else {
            column = random.nextInt(NUM_KANJI_LAST_LINE) + 1;
        }
        int x = line + OFFSET;
        int y = column + OFFSET;

        return Integer.toString(x, 16) + Integer.toString(y, 16);
    }
}
