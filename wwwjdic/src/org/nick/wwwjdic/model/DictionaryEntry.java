package org.nick.wwwjdic.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nick.wwwjdic.utils.StringUtils;

public class DictionaryEntry extends WwwjdicEntry implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 2057121563183659358L;

    private static final int WORD_IDX = 0;
    private static final int READING_IDX = 1;

    private static final String WORDNET_PREFIX = "JWN";
    private static final String WIP_MARKER_PREFIX = "WI";

    private String word;
    private String reading;
    private List<String> meanings = new ArrayList<String>();

    private String tranlsationString;

    private DictionaryEntry(String edictStr, String dictionary) {
        super(edictStr, dictionary);
    }

    public static DictionaryEntry parseEdict(String edictStr, String dictionary) {
        DictionaryEntry result = new DictionaryEntry(edictStr, dictionary);
        String[] fields = edictStr.split(" ");

        result.word = fields[WORD_IDX];
        int firstSpaceIdx = edictStr.indexOf(" ");
        String translationString = edictStr.substring(firstSpaceIdx + 1);
        result.tranlsationString = translationString.replace("/", " ")
                .replaceAll(WORDNET_PREFIX + "\\S+", "")
                .replaceAll(WIP_MARKER_PREFIX + "\\S+", "").trim();

        String meaningsField = null;
        int openingBracketIdx = translationString.indexOf('[');
        if (openingBracketIdx != -1) {
            result.reading = fields[READING_IDX].replaceAll("\\[", "")
                    .replaceAll("\\]", "");
            int closingBracketIdx = edictStr.indexOf(']');
            meaningsField = edictStr.substring(closingBracketIdx + 2);
        } else {
            int firstSlashIdx = translationString.indexOf('/');
            if (firstSlashIdx != -1) {
                meaningsField = translationString.substring(firstSlashIdx);
            } else {
                meaningsField = translationString.trim();
            }
        }

        String[] meaningsArr = meaningsField.split("/");
        for (int i = 0; i < meaningsArr.length; i++) {
            String meaning = meaningsArr[i];
            if (!"".equals(meaning) && !"(P)".equals(meaning)
                    && !meaning.startsWith(WORDNET_PREFIX)
                    && !meaning.startsWith(WIP_MARKER_PREFIX)) {
                result.meanings.add(meaning);
            }
        }

        return result;
    }

    public String getWord() {
        return word;
    }

    public String getReading() {
        return reading;
    }

    public List<String> getMeanings() {
        return Collections.unmodifiableList(meanings);
    }

    public String getTranslationString() {
        return tranlsationString;
    }

    public String getMeaningsAsString() {
        return StringUtils.join(getMeanings(), "/", 0);
    }

    @Override
    public String getDetailString() {
        return (reading == null ? "" : reading) + " "
                + (tranlsationString == null ? "" : tranlsationString);
    }

    @Override
    public String getHeadword() {
        return word;
    }

    @Override
    public boolean isKanji() {
        return false;
    }

}
