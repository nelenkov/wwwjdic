package org.nick.wwwjdic.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nick.wwwjdic.utils.StringUtils;

public class KanjiEntry extends WwwjdicEntry implements Serializable {

    private static final long serialVersionUID = -2260771889935344623L;

    private static final int KANJI_IDX = 0;
    private static final int JISCODE_IDX = 1;

    private static final List<String> CODES = Arrays.asList(new String[] { "B",
            "C", "F", "G", "J", "H", "N", "V", "D", "P", "S", "U", "I", "Q",
            "M", "E", "K", "L", "O", "W", "Y", "X", "Z" });

    private static final char UNICODE_CODE = 'U';
    private static final char RADICAL_CODE = 'B';
    private static final char CLASSICAL_RADICAL_CODE = 'C';
    private static final char FREQ_CODE = 'F';
    private static final char GRADE_CODE = 'G';
    private static final char STROKE_CODE = 'S';
    private static final char JLTP_LEVEL_CODE = 'J';
    private static final char SKIP_CODE = 'P';
    private static final char KOREAN_READING_CODE = 'W';
    private static final char PINYIN_CODE = 'Y';

    private static final Pattern HIRAGANA_PATTERN = Pattern.compile(
            "\\p{InHiragana}|-", Pattern.COMMENTS);
    private static final Pattern KATAKANA_PATTERN = Pattern.compile(
            "\\p{InKatakana}+", Pattern.COMMENTS);

    private static final String NANORI_TAG = "T1";
    private static final String RADICAL_NAME_TAG = "T2";

    private String kanji;

    private String jisCode;
    private String unicodeNumber;
    private int radicalNumber;
    private int strokeCount;
    private Integer classicalRadicalNumber;
    private Integer frequncyeRank;
    private Integer grade;
    private Integer jlptLevel;
    private String skipCode;

    private String reading;
    private String onyomi;
    private String kunyomi;
    private String nanori;
    private String radicalName;

    private String koreanReading;
    private String pinyin;

    private List<String> meanings = new ArrayList<String>();

    private KanjiEntry(String dictStr) {
        super(dictStr);
    }

    public static KanjiEntry parseKanjidic(String kanjidicStr) {
        KanjiEntry result = new KanjiEntry(kanjidicStr);

        String[] fields = kanjidicStr.split(" ");

        result.kanji = fields[KANJI_IDX];
        result.jisCode = fields[JISCODE_IDX];

        for (int i = JISCODE_IDX + 1; i < fields.length; i++) {
            String field = fields[i].trim();
            if ("".equals(field)) {
                continue;
            }

            char code = field.charAt(0);
            boolean isCode = CODES.contains(Character.toString(code));
            if (isCode) {
                switch (code) {
                case UNICODE_CODE:
                    result.unicodeNumber = parseStrCode(field);
                    break;
                case RADICAL_CODE:
                    result.radicalNumber = parseIntCode(field);
                    break;
                case CLASSICAL_RADICAL_CODE:
                    result.classicalRadicalNumber = parseIntCode(field);
                    break;
                case FREQ_CODE:
                    result.frequncyeRank = parseIntCode(field);
                    break;
                case GRADE_CODE:
                    result.grade = parseIntCode(field);
                    break;
                case STROKE_CODE:
                    // first one is the most common; do not overwrite
                    if (result.strokeCount == 0) {
                        result.strokeCount = parseIntCode(field);
                    }
                    break;
                case JLTP_LEVEL_CODE:
                    result.jlptLevel = parseIntCode(field);
                    break;
                case SKIP_CODE:
                    result.skipCode = parseStrCode(field);
                    break;
                // there can be multiple readings
                case KOREAN_READING_CODE:
                    if (StringUtils.isEmpty(result.koreanReading)) {
                        result.koreanReading = parseStrCode(field);
                    } else {
                        result.koreanReading += " ";
                        result.koreanReading += parseStrCode(field);
                    }
                    break;
                case PINYIN_CODE:
                    if (StringUtils.isEmpty(result.pinyin)) {
                        result.pinyin = parseStrCode(field);
                    } else {
                        result.pinyin += " ";
                        result.pinyin += parseStrCode(field);
                    }
                    break;
                default:
                    // ignore
                }
            } else {
                String readingAndMeanings = StringUtils.join(fields, " ", i);
                int bracketIdx = readingAndMeanings.indexOf('{');
                if (bracketIdx != -1) {
                    String reading = readingAndMeanings
                            .substring(0, bracketIdx).trim();
                    result.reading = reading;

                    result.parseReading();

                    String meaningsStr = readingAndMeanings
                            .substring(bracketIdx);
                    String[] meanings = meaningsStr.split("\\{");
                    for (String meaning : meanings) {
                        if (!"".equals(meaning)) {
                            result.meanings.add(meaning.replace("{", "")
                                    .replace("}", "").trim());
                        }
                    }
                    break;
                } else {
                    // no meaning? take the rest as reading
                    result.reading = readingAndMeanings;
                    result.parseReading();
                    break;
                }
            }
        }

        return result;
    }

    private void parseReading() {
        String[] readingFields = reading.split(" ");
        StringBuffer onyomiBuff = new StringBuffer();
        StringBuffer kunyomiBuff = new StringBuffer();
        StringBuffer nanoriBuff = new StringBuffer();
        StringBuffer radicalNameBuff = new StringBuffer();

        boolean foundNanori = false;
        boolean foundRadicalName = false;
        for (String r : readingFields) {
            Matcher m = KATAKANA_PATTERN.matcher(r);
            if (m.matches()) {
                onyomiBuff.append(r.trim());
                onyomiBuff.append(" ");
            }

            m = HIRAGANA_PATTERN.matcher(Character.toString(r.charAt(0)));
            if (m.matches()) {
                if (foundNanori) {
                    nanoriBuff.append(r.trim());
                    nanoriBuff.append(" ");
                } else if (foundRadicalName) {
                    radicalNameBuff.append(r.trim());
                    radicalNameBuff.append(" ");
                } else {
                    kunyomiBuff.append(r.trim());
                    kunyomiBuff.append(" ");
                }
            }

            if (NANORI_TAG.equals(r)) {
                foundNanori = true;
                foundRadicalName = false;
            }
            if (RADICAL_NAME_TAG.equals(r)) {
                foundNanori = false;
                foundRadicalName = true;
            }
        }

        onyomi = onyomiBuff.toString().trim();
        kunyomi = kunyomiBuff.toString().trim();
        nanori = nanoriBuff.toString().trim();
        radicalName = radicalNameBuff.toString().trim();
        reading = reading.replaceAll(" " + NANORI_TAG, "");
        reading = reading.replaceAll(" " + RADICAL_NAME_TAG, "");

        if (!StringUtils.isEmpty(koreanReading)) {
            koreanReading = koreanReading.trim();
        }
        if (!StringUtils.isEmpty(pinyin)) {
            pinyin = pinyin.trim();
        }
    }

    private static Integer parseIntCode(String field) {
        return Integer.parseInt(field.substring(1));
    }

    private static String parseStrCode(String field) {
        return field.substring(1);
    }

    public String getKanji() {
        return kanji;
    }

    public String getJisCode() {
        return jisCode;
    }

    public String getUnicodeNumber() {
        return unicodeNumber;
    }

    public int getRadicalNumber() {
        return radicalNumber;
    }

    public Integer getClassicalRadicalNumber() {
        return classicalRadicalNumber;
    }

    public Integer getFrequncyeRank() {
        return frequncyeRank;
    }

    public Integer getGrade() {
        return grade;
    }

    public int getStrokeCount() {
        return strokeCount;
    }

    public Integer getJlptLevel() {
        return jlptLevel;
    }

    public String getSkipCode() {
        return skipCode;
    }

    public String getKoreanReading() {
        return koreanReading;
    }

    public String getPinyin() {
        return pinyin;
    }

    public String getReading() {
        return reading;
    }

    public String getOnyomi() {
        return onyomi;
    }

    public String getKunyomi() {
        return kunyomi;
    }

    public String getNanori() {
        return nanori;
    }

    public String getRadicalName() {
        return radicalName;
    }

    public List<String> getMeanings() {
        return Collections.unmodifiableList(meanings);
    }

    public String getMeaningsAsString() {
        return StringUtils.join(getMeanings(), "/", 0);
    }

    @Override
    public String getDetailString() {
        return reading + " " + getMeaningsAsString();
    }

    @Override
    public String getHeadword() {
        return kanji;
    }

    @Override
    public boolean isKanji() {
        return true;
    }

}
