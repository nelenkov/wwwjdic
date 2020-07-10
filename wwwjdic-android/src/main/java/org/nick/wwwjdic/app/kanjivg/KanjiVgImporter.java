package org.nick.wwwjdic.app.kanjivg;

import org.nick.wwwjdic.app.server.CacheController;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class KanjiVgImporter {

    @Inject
    private Logger log;

    @Inject
    private KanjiDao dao;

    public int processKanjiVg(int batchSize, boolean update) {
        List<GzipBlob> blobs = findAllBlobs();
        for (GzipBlob blob : blobs) {
            if (blob != null && !blob.isDone()) {
                return processKanjiBatch(blob, batchSize, update);
            }
        }

        return -1;
    }

    private int processKanjiBatch(GzipBlob blob, int batchSize, boolean update) {
        log.info("processKanjiBatch: " + blob.getName());

        try {
            GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(
                    blob.getData()));
            KanjivgParser parser = new KanjivgParser(in);

            int numKanjis = 0;
            int startingKanji = 0;
            if (blob.getNumProcessed() != 0) {
                startingKanji = blob.getNumProcessed();
            }
            parser.fastForward(startingKanji);

            while (parser.hasNext()) {
                if (numKanjis == batchSize) {
                    int totalProcessed = blob.getNumProcessed() + batchSize;
                     blob.setNumProcessed(totalProcessed);
                     ofy().save().entity(blob).now();

                     return numKanjis;
                }

                Kanji kanji = parser.next();
                if (kanji != null) {
                    if (update) {
                        updateKanji(kanji.getUnicodeNumber(), kanji);
                    } else {
                        log.info("saving: " + kanji.getMidashi());
                        log.info("strokes: " + kanji.getStrokes());
                        dao.saveKanji(kanji);
                        log.info("done");
                    }
                    numKanjis++;
                }
            }

            int totalProcessed = blob.getNumProcessed() + numKanjis;
            blob.setNumProcessed(totalProcessed);
            ofy().save().entity(blob).now();

            return numKanjis;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<GzipBlob> findAllBlobs() {
        log.info("findAllBlobs");

        return ofy().load().type(GzipBlob.class).list();
    }

    private GzipBlob findBlob(String name) {
        log.info("findBlob: " + name);
        GzipBlob blob = ofy().load().type(GzipBlob.class).filter("name", name).first().now();

        log.info("found: " + blob);

        return blob;
    }

     private void updateKanji(String unicodeNumber, Kanji newKanji) {
         Kanji existing = dao.findKanji(unicodeNumber);
         if (existing == null) {
             log.warning(String.format(
                     "Kanji %s not found, adding", unicodeNumber));
             dao.saveKanji(newKanji);
             return;
         }

         List<Stroke> newStrokes = newKanji.getStrokes();
         List<Stroke> existingStrokes = existing.getStrokes();
         for (int i = 0; i < existingStrokes.size(); i++) {
             Stroke s = newStrokes.get(i);
             Stroke old = existingStrokes.get(i);
             log.info("old stroke: " + old);
             log.info("new stroke: " + s);

             old.setPath(s.getPath());
             old.setNumber(s.getNumber());
             ofy().save().entity(old).now();
         }

         CacheController.remove(unicodeNumber);
     }

}
