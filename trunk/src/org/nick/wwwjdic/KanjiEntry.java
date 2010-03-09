package org.nick.wwwjdic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KanjiEntry implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

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
	private static final char PINYIN_CODE = 'Y';

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

	private String pinyin;
	private String reading;

	private List<String> meanings = new ArrayList<String>();

	private KanjiEntry() {
	}

	public static KanjiEntry parseKanjidic(String kanjidicStr) {
		KanjiEntry result = new KanjiEntry();

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
				case PINYIN_CODE:
					result.pinyin = parseStrCode(field);
					break;
				default:
					// ignore
				}
			} else {
				String readingAndMeanings = join(fields, i);
				int bracketIdx = readingAndMeanings.indexOf('{');
				if (bracketIdx != -1) {
					String reading = readingAndMeanings
							.substring(0, bracketIdx).trim();
					result.reading = reading;
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
					break;
				}
			}
		}

		return result;
	}

	private static String join(String[] fields, int idx) {
		StringBuffer buff = new StringBuffer();
		for (int i = idx; i < fields.length; i++) {
			buff.append(fields[i]);
			if (i != fields.length - 1) {
				buff.append(" ");
			}
		}

		return buff.toString();
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

	public String getPinyin() {
		return pinyin;
	}

	public String getReading() {
		return reading;
	}

	public List<String> getMeanings() {
		return Collections.unmodifiableList(meanings);
	}

}
