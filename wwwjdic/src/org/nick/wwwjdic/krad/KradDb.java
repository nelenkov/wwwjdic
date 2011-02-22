package org.nick.wwwjdic.krad;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.nick.wwwjdic.utils.FileUtils;

public class KradDb {

    private static final String TAG = KradDb.class.getSimpleName();

    private Map<Character, Set<Character>> radicalToKanjis = new HashMap<Character, Set<Character>>();
    private Map<Character, Set<Character>> kanjiToRadicals = new HashMap<Character, Set<Character>>();

    public boolean isInitialized() {
        return !radicalToKanjis.isEmpty();
    }

    public void readFromStream(InputStream in) {
        if (isInitialized()) {
            return;
        }

        try {
            String kradStr = FileUtils.readTextFile(in, "UTF-8").trim();
            String[] lines = kradStr.split("\n");
            for (String line : lines) {
                String[] fields = line.split(":");
                Character radical = fields[0].trim().charAt(0);
                Set<Character> kanjis = new HashSet<Character>();
                char[] kanjiChars = fields[2].trim().toCharArray();
                for (char c : kanjiChars) {
                    kanjis.add(c);

                    Set<Character> radicals = kanjiToRadicals.get(c);
                    if (radicals == null) {
                        radicals = new HashSet<Character>();
                        kanjiToRadicals.put(c, radicals);
                    }
                    radicals.add(radical);
                }
                radicalToKanjis.put(radical, kanjis);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public Set<Character> getKanjiForRadical(Character radical) {
        return radicalToKanjis.get(radical);
    }

    public Set<Character> getKanjisForRadicals(Set<Character> radicals) {
        Set<Character> result = new HashSet<Character>();
        for (Character radical : radicals) {
            Set<Character> kanjis = getKanjiForRadical(radical);
            if (result.isEmpty()) {
                result.addAll(kanjis);
            } else {
                result.retainAll(kanjis);
            }
        }

        return result;
    }

    public Set<Character> getRadicalsForKanji(Character kanji) {
        return kanjiToRadicals.get(kanji);
    }

    public Set<Character> getRadicalsForKanjis(Set<Character> kanjis) {
        Set<Character> result = new HashSet<Character>();
        for (Character kanji : kanjis) {
            Set<Character> radicals = getRadicalsForKanji(kanji);
            result.addAll(radicals);
        }

        return result;
    }
}
