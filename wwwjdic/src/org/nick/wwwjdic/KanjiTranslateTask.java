package org.nick.wwwjdic;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KanjiTranslateTask extends BackdoorTranslateTask {

	public KanjiTranslateTask(ResultListView resultListView, SearchCriteria criteria) {
		super(resultListView, criteria);
	}

	protected List<KanjiEntry> parseResult(String html) {
		List<KanjiEntry> result = new ArrayList<KanjiEntry>();

		Pattern pattern = selectPattern();
		String[] lines = html.split("\n");
		for (String line : lines) {
			Matcher m = pattern.matcher(line);
			if (m.matches()) {
				KanjiEntry entry = KanjiEntry.parseKanjidic(m.group(1).trim());

				result.add(entry);
			}
		}

		return result;
	}
}
