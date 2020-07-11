package org.nick.wwwjdic.app.kanjivg;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

@Singleton
public class KanjiDao {

    @Inject
    private Logger log;

    public Kanji findKanji(String unicodeNumber) {
        log.info("findKanj: " + unicodeNumber);

        Objectify ofy = ofy();

        Kanji kanji = ofy.load().type(Kanji.class).filter("unicodeNumber == ", unicodeNumber).first().now();

        if (kanji == null) {
            log.info(String.format("KanjiVG data for %s not found",
                    unicodeNumber));
        } else {
            String result = kanji.getMidashi() + " " + kanji.getUnicodeNumber() + "\n";
            log.info("returning " + result);
        }

        return kanji;
    }

    public Kanji loadWithStrokes(String unicodeNumber) {
        Kanji kanji = findKanji(unicodeNumber);
        if (kanji == null) {
            return null;
        }

        List<Stroke> strokes =  ofy().load().type(Stroke.class)
                .ancestor(Key.create(Kanji.class, kanji.getId()))
                .order("number").list();
        kanji.setStrokes(strokes);

        return kanji;
    }

    public void saveKanji(Kanji kanji) {
        log.info("saveKanji: " + kanji);
        Key<Kanji> key = ofy().save().entity(kanji).now();
        for (Stroke s: kanji.getStrokes()) {
            s.setKanji(key);
            ofy().save().entity(s).now();
        }
    }

}
