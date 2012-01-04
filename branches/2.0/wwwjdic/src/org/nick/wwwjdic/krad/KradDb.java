package org.nick.wwwjdic.krad;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.nick.wwwjdic.utils.FileUtils;

import android.util.Log;

public class KradDb {

    private static final String TAG = KradDb.class.getSimpleName();

    private Map<String, Set<String>> radicalToKanjis = new HashMap<String, Set<String>>();
    private Map<String, Set<String>> kanjiToRadicals = new HashMap<String, Set<String>>();

    private static KradDb instance;

    public static KradDb getInstance() {
        if (instance == null) {
            instance = new KradDb();
        }

        return instance;
    }

    private KradDb() {
    }

    public boolean isInitialized() {
        return !radicalToKanjis.isEmpty();
    }

    public void readFromStream(InputStream in) {
        if (isInitialized()) {
            return;
        }

        try {
            Log.d(TAG, "loading radkfile...");
            long start = System.currentTimeMillis();

            String kradStr = FileUtils.readTextFile(in, "UTF-8").trim();
            String[] lines = kradStr.split("\n");
            for (String line : lines) {
                String[] fields = line.split(":");
                if (fields.length < 3) {
                    continue;
                }
                String radical = fields[0].trim();
                Set<String> kanjis = new HashSet<String>();
                String[] kanjiChars = fields[2].trim().split("");
                for (String c : kanjiChars) {
                    if ("".equals(c)) {
                        continue;
                    }

                    kanjis.add(c);

                    Set<String> radicals = kanjiToRadicals.get(c);
                    if (radicals == null) {
                        radicals = new HashSet<String>();
                        kanjiToRadicals.put(c, radicals);
                    }
                    radicals.add(radical);
                }
                radicalToKanjis.put(radical, kanjis);
            }

            long time = System.currentTimeMillis() - start;
            Log.d(TAG, String.format("loaded %d radicals, %d kanji in %d [ms]",
                    radicalToKanjis.size(), kanjiToRadicals.size(), time));
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

    public Set<String> getKanjiForRadical(String radical) {
        Set<String> result = radicalToKanjis.get(radical);

        return result == null ? new HashSet<String>() : result;
    }

    public Set<String> getKanjisForRadicals(Set<String> radicals) {
        Set<String> result = new HashSet<String>();
        for (String radical : radicals) {
            Set<String> kanjis = getKanjiForRadical(radical);
            if (result.isEmpty()) {
                result.addAll(kanjis);
            } else {
                result.retainAll(kanjis);
            }
        }

        return result;
    }

    public Set<String> getRadicalsForKanji(String kanji) {
        Set<String> result = kanjiToRadicals.get(kanji);

        return result == null ? new HashSet<String>() : result;
    }

    public Set<String> getRadicalsForKanjis(Set<String> kanjis) {
        Set<String> result = new HashSet<String>();
        for (String kanji : kanjis) {
            Set<String> radicals = getRadicalsForKanji(kanji);
            result.addAll(radicals);
        }

        return result;
    }
}
