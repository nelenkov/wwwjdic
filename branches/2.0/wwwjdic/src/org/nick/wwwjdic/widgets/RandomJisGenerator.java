package org.nick.wwwjdic.widgets;

import java.io.UnsupportedEncodingException;
import java.util.Random;

public class RandomJisGenerator implements KanjiGenerator {

    // Cf. JIS X 0208:
    //
    // * 94x94 grid
    // * kanji: lines 16-83
    // * line 84 -- 6 kanji
    // * raw JIS: the 16-bit code resulting from adding 0x20 to each
    // JIS coordinate and concatenating the two resulting bytes,
    // the vertical coordinate becoming the high byte. (coords are zero based)

    // Cf.
    // ftp://ftp.unicode.org/Public/MAPPINGS/OBSOLETE/EASTASIA/JIS/JIS0208.TXT
    // To change hex to EUC form, add 0x8080
    // To change hex to kuten form, first subtract 0x2020. Then
    // the high and low bytes correspond to the ku and ten of
    // the kuten form. For example, 0x2121 -> 0x0101 -> 0101;
    // 0x7426 -> 0x5406 -> 8406

    private static final int JIS_GRID_SIZE = 94;

    private static final int KANJI_START_LINE = 16;
    private static final int KANJI_END_LINE = 84;
    private static final int NUM_KANJI_LAST_LINE = 6;

    private static final int LEVEL1_START_LINE = 16;
    private static final int LEVEL1_END_LINE = 47;
    private static final int LEVEL1_END_LINE_NUM_KANJI = 51;

    private static final int OFFSET = 0x20;

    private Random random = new Random();

    private boolean limitToLevelOne;
    private String currentKanji;

    public RandomJisGenerator(boolean limitToLevelOne) {
        this.limitToLevelOne = limitToLevelOne;
    }

    public String generateRawJis() {
        return generateRawJis(false);
    }

    public String generateRawJis(boolean limitToLevelOne) {
        int startLine = KANJI_START_LINE;
        int endLine = KANJI_END_LINE;

        if (limitToLevelOne) {
            startLine = LEVEL1_START_LINE;
            endLine = LEVEL1_END_LINE;
        }

        int line = random.nextInt(endLine - startLine + 1) + startLine;

        int column = 0;
        if (limitToLevelOne && line == LEVEL1_END_LINE) {
            column = random.nextInt(LEVEL1_END_LINE_NUM_KANJI) + 1;
        } else if (!limitToLevelOne && line == KANJI_END_LINE) {
            column = random.nextInt(NUM_KANJI_LAST_LINE) + 1;
        } else {
            column = random.nextInt(JIS_GRID_SIZE);
        }

        int x = line + OFFSET;
        int y = column + OFFSET;

        return Integer.toString(x, 16) + Integer.toString(y, 16);
    }

    @Override
    public void setCurrentKanji(String kanji) {
        currentKanji = kanji;
    }

    @Override
    public String selectNextUnicodeCp() {
        return generateAsUnicodeCp(limitToLevelOne);
    }

    public String generateAsUnicodeCp(boolean limitToLevelOne) {
        return rawJisToUnicodeCp(generateRawJis(limitToLevelOne));
    }

    private static String rawJisToUnicodeCp(String rawJis) {
        if (rawJis.length() != 4) {
            throw new IllegalArgumentException("Invalid raw JIS code");
        }
        byte high = Byte.parseByte(rawJis.substring(0, 2), 16);
        byte low = Byte.parseByte(rawJis.substring(2), 16);

        byte[] eucBytes = { (byte) (high + 0x80), (byte) (low + 0x80) };
        try {
            String kanji = new String(eucBytes, "EUC-JP");
            int unicodeCp = (int) kanji.toCharArray()[0];

            return Integer.toString(unicodeCp, 16);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
