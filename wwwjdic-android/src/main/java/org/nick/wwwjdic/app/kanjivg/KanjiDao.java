package org.nick.wwwjdic.app.kanjivg;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

@Singleton
public class KanjiDao {

    @Inject
    private Logger log;

    public Kanji findKanji(String unicodeNumber) {
        log.info("findKnajk: " + unicodeNumber);

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

    public void saveKanji(Kanji kanji) {
        Key<Kanji> key = ofy().save().entity(kanji).now();
        for (Stroke s: kanji.getStrokes()) {
            s.setKanji(key);
            ofy().save().entity(s).now();
        }
    }

}
