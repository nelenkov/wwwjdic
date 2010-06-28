package org.nick.wwwjdic;

public class SentenceBreakdownEntry {

    private String word;
    private String reading;
    private String translation;
    private String explanation;

    public static SentenceBreakdownEntry create(String word, String reading,
            String translation) {
        return new SentenceBreakdownEntry(word, reading, translation, null);
    }

    public static SentenceBreakdownEntry createNoReading(String word,
            String translation) {
        return new SentenceBreakdownEntry(word, null, translation, null);
    }

    public static SentenceBreakdownEntry createWithExplanation(String word,
            String reading, String translation, String explanation) {
        return new SentenceBreakdownEntry(word, reading, translation,
                explanation);
    }

    private SentenceBreakdownEntry(String word, String reading,
            String translation, String explanation) {
        this.word = word;
        this.reading = reading;
        this.translation = translation;
        this.explanation = explanation;
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

}
