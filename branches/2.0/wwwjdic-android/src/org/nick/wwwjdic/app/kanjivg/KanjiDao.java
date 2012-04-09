package org.nick.wwwjdic.app.kanjivg;

import java.util.List;
import java.util.logging.Logger;

import javax.inject.Singleton;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

@Singleton
public class KanjiDao {

    private static final Logger log = Logger
            .getLogger(KanjiDao.class.getName());

    @SuppressWarnings("unchecked")
    public Kanji findKanji(PersistenceManager pm, String unicodeNumber) {

        Query q = null;

        try {
            Kanji k = null;

            q = pm.newQuery("select from org.nick.wwwjdic.app.kanjivg.Kanji "
                    + "where unicodeNumber == unicodeNumberParam "
                    + "parameters String unicodeNumberParam ");

            List<Kanji> kanjis = (List<Kanji>) q.execute(unicodeNumber);
            if (kanjis.isEmpty()) {
                log.info(String.format("KanjiVG data for %s not found",
                        unicodeNumber));
                return null;
            }

            k = kanjis.get(0);

            String result = k.getMidashi() + " " + k.getUnicodeNumber() + "\n";
            log.info("returning " + result);

            return k;

        } finally {
            if (q != null) {
                q.closeAll();
            }
        }
    }

    public void saveKanji(PersistenceManager pm, Kanji kanji) {
        Transaction tx = null;
        try {
            tx = pm.currentTransaction();
            tx.begin();

            pm.makePersistent(kanji);
            tx.commit();
        } finally {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        }
    }
}
