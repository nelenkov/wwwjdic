package org.nick.wwwjdic;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DictionaryTranslateTask extends BackdoorTranslateTask {

    public DictionaryTranslateTask(DictionaryResultListView resultListView,
            SearchCriteria criteria) {
        super(resultListView, criteria);
    }

    @Override
    protected List<DictionaryEntry> parseResult(String html) {
        List<DictionaryEntry> result = new ArrayList<DictionaryEntry>();

        Pattern pattern = selectPattern();
        String[] lines = html.split("\n");
        for (String line : lines) {
            Matcher m = pattern.matcher(line);
            if (m.matches()) {
                DictionaryEntry entry = DictionaryEntry.parseEdict(m.group(1)
                        .trim());
                result.add(entry);
            }
        }

        return result;
    }
}
