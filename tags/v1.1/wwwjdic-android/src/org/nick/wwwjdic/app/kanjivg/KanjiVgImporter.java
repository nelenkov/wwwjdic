package org.nick.wwwjdic.app.kanjivg;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

public class KanjiVgImporter {

    Logger logger = Logger.getLogger("KanjiVgImporter");

    public int processKanjiVg(int batchSize) {
        String name = "kanjivg1.xml.gz";
        String name2 = "kanjivg2.xml.gz";
        String name3 = "kanjivg3.xml.gz";

        GzipBlob blob = findBlob(name);
        if (!blob.isDone()) {
            return processKanjiBatch(blob, batchSize);
        }

        blob = findBlob(name2);
        if (!blob.isDone()) {
            return processKanjiBatch(blob, batchSize);
        }

        blob = findBlob(name3);
        if (!blob.isDone()) {
            return processKanjiBatch(blob, batchSize);
        }

        return -1;
    }

    private int processKanjiBatch(GzipBlob blob, int batchSize) {
        Transaction tx = null;
        try {
            GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(
                    blob.getData()));
            KanjivgParser parser = new KanjivgParser(in);

            int numKanjis = 0;
            int startingKanji = 0;
            if (blob.getNumProcessed() != 0) {
                startingKanji = blob.getNumProcessed() + 1;
            }
            parser.fastForward(startingKanji);

            PersistenceManager pm = PMF.get().getPersistenceManager();
            while (parser.hasNext()) {
                if (numKanjis == batchSize) {

                    try {
                        tx = pm.currentTransaction();
                        tx.begin();
                        int totalProcessed = blob.getNumProcessed() + batchSize;
                        blob.setNumProcessed(totalProcessed);
                        pm.makePersistent(blob);
                        tx.commit();

                        return numKanjis;
                    } finally {
                        if (tx != null && tx.isActive()) {
                            tx.rollback();
                        }
                    }
                }

                Kanji kanji = parser.next();
                if (kanji != null) {
                    logger.info("saving: " + kanji.getMidashi());
                    saveKanji(pm, kanji);
                    logger.info("done");
                    numKanjis++;
                }
            }

            try {
                tx = pm.currentTransaction();
                tx.begin();
                int totalProcessed = blob.getNumProcessed() + numKanjis;
                blob.setNumProcessed(totalProcessed);
                pm.makePersistent(blob);
                tx.commit();

                return numKanjis;
            } finally {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private GzipBlob findBlob(String name) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            Query q = pm
                    .newQuery("select from org.nick.wwwjdic.app.kanjivg.GzipBlob "
                            + "where name == nameParam "
                            + "parameters String nameParam ");
            List<GzipBlob> blobs = (List<GzipBlob>) q.execute(name);
            if (blobs.isEmpty()) {
                return null;
            }

            return blobs.get(0);
        } finally {
            pm.close();
        }
    }

    private void saveKanji(PersistenceManager pm, Kanji kanji) {
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
