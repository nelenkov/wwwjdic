package org.nick.wwwjdic;

import org.nick.wwwjdic.history.HistoryDbHelper;
import org.nick.wwwjdic.model.KanjiEntry;
import org.nick.wwwjdic.model.SearchCriteria;
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
        intent.putExtra(Wwwjdic.EXTRA_CRITERIA, criteria);

        Analytics.event("kanjiSearch", context);

        context.startActivity(intent);
    }

    public static void showStrokeOrder(Context context, KanjiEntry entry) {
        Intent intent = new Intent(context, SodActivity.class);
        intent.putExtra(SodActivity.EXTRA_KANJI_UNICODE_NUMBER,
                entry.getUnicodeNumber());
        intent.putExtra(SodActivity.EXTRA_KANJI_GLYPH, entry.getKanji());

        context.startActivity(intent);
    }

    public static void home(Context context) {
        Intent intent = new Intent(context, Wwwjdic.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

}
