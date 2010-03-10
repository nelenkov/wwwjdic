package org.nick.wwwjdic;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class DictionaryTranslateTask extends BackdoorTranslateTask {

	public DictionaryTranslateTask(DictionaryResultListView resultListView,
			SearchCriteria criteria) {
		super(resultListView, criteria);
	}

	@Override
	protected List<DictionaryEntry> parseResult(String html) {
		List<DictionaryEntry> result = new ArrayList<DictionaryEntry>();

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
				DictionaryEntry entry = DictionaryEntry.parseEdict(line.trim());
				result.add(entry);
			}
		}

		return result;
	}
}
