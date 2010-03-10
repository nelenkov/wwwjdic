package org.nick.wwwjdic;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import android.util.Log;

public abstract class BackdoorTranslateTask extends TranslateTask {

	private static final String BACKDOOR_URL = "http://www.csse.monash.edu.au/~jwb/cgi-bin/wwwjdic.cgi";

	protected static final Pattern PRE_START_PATTERN = Pattern
			.compile("^<pre>.*$");

	protected static final Pattern PRE_END_PATTERN = Pattern
			.compile("^</pre>.*$");

	public BackdoorTranslateTask(ResultListView resultListView,
			SearchCriteria criteria) {
		super(resultListView, criteria);
	}

	@Override
	protected abstract List<?> parseResult(String html);

	@Override
	protected String query(SearchCriteria criteria) {
		try {
			String lookupUrl = String.format("%s?%s%s", BACKDOOR_URL,
					generateBackdoorCode(criteria), URLEncoder.encode(criteria
							.getQueryString(), "UTF-8"));
			HttpGet get = new HttpGet(lookupUrl);

			String responseStr = httpclient.execute(get, responseHandler,
					localContext);

			return responseStr;
		} catch (ClientProtocolException cpe) {
			Log.e("WWWJDIC", "ClientProtocolException", cpe);
			throw new RuntimeException(cpe);
		} catch (IOException e) {
			Log.e("WWWJDIC", "IOException", e);
			throw new RuntimeException(e);
		}
	}

	private String generateBackdoorCode(SearchCriteria criteria) {
		StringBuffer buff = new StringBuffer();
		buff.append(criteria.getDictionary());
		// raw
		buff.append("Z");
		if (criteria.isKanjiLookup()) {
			buff.append("M");
		} else {
			// unicode
			buff.append("U");
		}
		if (criteria.isExactMatch()) {
			buff.append("Q");
		} else {
			if (criteria.isKanjiLookup()) {
				buff.append("J");
			} else {
				// English/Kanji
				buff.append("E");
				// for romanized Japanese
				// buff.append("J");
			}
		}

		return buff.toString();
	}

}
