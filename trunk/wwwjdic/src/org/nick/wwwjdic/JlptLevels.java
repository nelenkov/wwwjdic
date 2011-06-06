package org.nick.wwwjdic;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

public class JlptLevels {

    private static JlptLevels instance = new JlptLevels();

    private Map<String, String> kanjiToLevel = new HashMap<String, String>();

    private JlptLevels() {
    }

    public static JlptLevels getInstance() {
        return instance;
    }

    public void initialize(Context context) {
        for (int i = 1; i < 6; i++) {
            String levelStr = "N" + i;
            String[] kanjis = getKanjiForLevel(context, i);
            for (String kanji : kanjis) {
                kanjiToLevel.put(kanji, levelStr);
            }
        }
    }

    public String getLevel(String kanji) {
        if (kanjiToLevel.isEmpty()) {
            throw new IllegalStateException("Not initialized");
        }

        return kanjiToLevel.get(kanji);
    }

    public static String[] getKanjiForLevel(Context context, int level) {
        int arrayId = context.getResources().getIdentifier("jlpt_n" + level,
                "array", context.getPackageName());

        return context.getResources().getStringArray(arrayId);
    }
}
