package org.nick.wwwjdic;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class KanjiTranslateTask extends BackdoorTranslateTask {

	public KanjiTranslateTask(ResultListView resultListView,
			SearchCriteria criteria) {
		super(resultListView, criteria);
	}

	protected List<KanjiEntry> parseResult(String html) {
		List<KanjiEntry> result = new ArrayList<KanjiEntry>();

		boolean isInPre = false;
		String[] lines = html.split("\n");
		for (String line : lines) {
			Matcher m = PRE_START_PATTERN.matcher(line);
			if (m.matches()) {
				isInPre = true;
			}

			m = PRE_END_PATTERN.matcher(line);
			if (m.matches()) {
				break;
			}

			if (isInPre) {
				KanjiEntry entry = KanjiEntry.parseKanjidic(line.trim());

				result.add(entry);
			}
		}

		return result;
	}
}
