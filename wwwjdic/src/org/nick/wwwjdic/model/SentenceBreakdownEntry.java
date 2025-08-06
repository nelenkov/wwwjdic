package org.nick.wwwjdic.model;

import androidx.annotation.NonNull;
import java.io.Serializable;

public class SentenceBreakdownEntry implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -3859673753381220751L;

    private final String inflectedForm;
    private final String word;
    private final String reading;
    private final String translation;
    private final String explanation;

    public static SentenceBreakdownEntry create(String inflectedForm,
            String word, String reading, String translation) {
        return new SentenceBreakdownEntry(inflectedForm, word, reading,
                translation, null);
    }

    public static SentenceBreakdownEntry createNoReading(String inflectedForm,
            String word, String translation) {
        return new SentenceBreakdownEntry(inflectedForm, word, null,
                translation, null);
    }

    public static SentenceBreakdownEntry createWithExplanation(
            String inflectedForm, String word, String reading,
            String translation, String explanation) {
        return new SentenceBreakdownEntry(inflectedForm, word, reading,
                translation, explanation);
    }

    private SentenceBreakdownEntry(String inflectedForm, String word,
            String reading, String translation, String explanation) {
        this.inflectedForm = inflectedForm;
        this.word = word;
        this.reading = reading;
        this.translation = translation;
        this.explanation = explanation;
    }

    public String getInflectedForm() {
        return inflectedForm;
    }

    public String getWord() {
        return word;
    }

    public String getReading() {
        return reading;
    }

    public String getTranslation() {
        return translation;
    }

    public String getExplanation() {
        return explanation;
    }

    @NonNull
    @Override
    public String toString() {
        return "SentenceBreakdownEntry{" +
                "inflectedForm='" + inflectedForm + '\'' +
                ", word='" + word + '\'' +
                ", reading='" + reading + '\'' +
                ", translation='" + translation + '\'' +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}
