package org.nick.wwwjdic;

import org.nick.wwwjdic.history.HistoryDbHelper;
import org.nick.wwwjdic.sod.SodActivity;
import org.nick.wwwjdic.utils.Analytics;
import org.nick.wwwjdic.utils.StringUtils;

import android.content.Context;
import android.content.Intent;

public class Activities {

    private Activities() {
    }

    public static void lookupKanji(Context context, HistoryDbHelper db,
            String headword) {
        SearchCriteria criteria = SearchCriteria
                .createForKanjiOrReading(headword);

        if (!StringUtils.isEmpty(criteria.getQueryString())) {
            db.addSearchCriteria(criteria);
        }

        Intent intent = new Intent(context, KanjiResultListView.class);
        intent.putExtra(Constants.CRITERIA_KEY, criteria);

        Analytics.event("kanjiSearch", context);

        context.startActivity(intent);
    }

    public static void showStrokeOrder(Context context, KanjiEntry entry) {
        Intent intent = new Intent(context, SodActivity.class);
        intent.putExtra(Constants.KANJI_UNICODE_NUMBER,
                entry.getUnicodeNumber());
        intent.putExtra(Constants.KANJI_GLYPH, entry.getKanji());

        context.startActivity(intent);
    }

}
