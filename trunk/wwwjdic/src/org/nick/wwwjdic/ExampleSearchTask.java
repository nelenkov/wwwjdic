package org.nick.wwwjdic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

public class ExampleSearchTask extends TranslateTask<ExampleSentence> {

    private static final Pattern UL_PATTERN = Pattern.compile("^.*<ul>.*$");
    private static final Pattern CLOSING_UL_PATTERN = Pattern
            .compile("^.*</ul>.*$");
    private static final Pattern LI_PATTERN = Pattern.compile("^.*<li>.*$");
    private static final Pattern INPUT_PATTERN = Pattern
            .compile("^.*<INPUT.*$");

    private static final int IN_EXAMPLES_BLOCK = 0;
    private static final int EXAMPLE_FOLLOWS = 1;
    private static final int TRANSLATION_FOLLOWS = 2;
    private static final int EXAMPLES_FINISHED = 3;

    private int maxNumExamples;

    public ExampleSearchTask(String url, int timeoutSeconds,
            ResultListView<ExampleSentence> resultView,
            SearchCriteria searchCriteria, int maxNumExamples) {
        super(url, timeoutSeconds, resultView, searchCriteria);
        this.maxNumExamples = maxNumExamples;
    }

    @Override
    protected List<ExampleSentence> parseResult(String html) {
        List<ExampleSentence> result = new ArrayList<ExampleSentence>();

        String[] lines = html.split("\n");

        int state = -1;
        String japaneseSentence = null;
        String englishSentence = null;

        for (String line : lines) {
            if (UL_PATTERN.matcher(line).matches()) {
                state = IN_EXAMPLES_BLOCK;
                continue;
            }
            if (LI_PATTERN.matcher(line).matches()) {
                state = EXAMPLE_FOLLOWS;
                continue;
            }
            if (INPUT_PATTERN.matcher(line).matches()) {
                state = TRANSLATION_FOLLOWS;
                continue;
            }
            if (CLOSING_UL_PATTERN.matcher(line).matches()) {
                state = EXAMPLES_FINISHED;
                break;
            }

            switch (state) {
            case EXAMPLE_FOLLOWS:
                japaneseSentence = line.trim();
                break;
            case TRANSLATION_FOLLOWS:
                if (japaneseSentence != null) {
                    englishSentence = line.trim();
                    result.add(new ExampleSentence(japaneseSentence,
                            englishSentence));
                }
                break;
            default:
                continue;
            }
        }

        return result;
    }

    @Override
    protected String query(WwwjdicQuery query) {
        try {
            SearchCriteria criteria = (SearchCriteria) query;

            HttpPost post = new HttpPost(url);
            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            String searchString = criteria.getQueryString();
            if (criteria.isExactMatch()) {
                searchString = "\\<" + searchString + "\\>";
            }
            NameValuePair pair = new BasicNameValuePair("exsrchstr",
                    searchString);
            pairs.add(pair);
            pair = new BasicNameValuePair("exsrchnum", Integer
                    .toString(maxNumExamples));
            pairs.add(pair);

            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(pairs,
                    "EUC-JP");
            post.setEntity(formEntity);

            GzipStringResponseHandler handler = new GzipStringResponseHandler();
            String responseStr = httpclient.execute(post, handler);

            return responseStr;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
