package org.nick.wwwjdic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExampleSearchTaskBackdoor extends
        BackdoorSearchTask<ExampleSentence> {

    private static final Pattern SENTENCE_PATTERN = Pattern
            .compile("^A:\\s(\\S+)\t(.+)#.*$");
    private static final Pattern BREAKDOWN_PATTERN = Pattern
            .compile("^B:\\s.+\\{(\\S+)\\}.*$");
    private static final Pattern WORD_FORM_PATTERN = Pattern
            .compile(
                    "(\\S+?)(?:\\(\\p{InHiragana}+\\))?(?:\\[\\d+\\])?(?:\\{(\\S+)\\})?~?",
                    Pattern.COMMENTS);

    private ExampleSentence lastSentence;

    private boolean randomExamples;

    public ExampleSearchTaskBackdoor(String url, int timeoutSeconds,
            ResultListView<ExampleSentence> resultView,
            SearchCriteria searchCriteria, boolean randomExamples) {
        super(url, timeoutSeconds, resultView, searchCriteria);
        this.randomExamples = randomExamples;
    }

    @Override
    protected ExampleSentence parseEntry(String entryStr) {
        Matcher m = SENTENCE_PATTERN.matcher(entryStr);
        if (m.matches()) {
            String japanese = m.group(1);
            String english = m.group(2);
            ExampleSentence result = new ExampleSentence(japanese, english);
            lastSentence = result;

            return result;
        }

        if (lastSentence == null) {
            return null;
        }

        m = BREAKDOWN_PATTERN.matcher(entryStr);
        if (m.matches()) {
            String[] words = entryStr.substring(3).split(" ");
            String queryForm = query.getQueryString();
            for (String word : words) {
                Matcher formMatcher = WORD_FORM_PATTERN.matcher(word);
                if (formMatcher.matches()) {
                    String basicForm = formMatcher.group(1);
                    String formInSentence = formMatcher.group(2);
                    if (formInSentence != null) {
                        if (queryForm.equals(basicForm)
                                || queryForm.equals(formInSentence)) {
                            lastSentence.addMatch(formInSentence);
                        }
                    } else {
                        if (queryForm.equals(basicForm)) {
                            lastSentence.addMatch(basicForm);
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    protected String generateBackdoorCode(SearchCriteria criteria) {
        return WwwjdicClient.generateExamplesBackdoorCode(criteria,
                randomExamples);
    }
}
