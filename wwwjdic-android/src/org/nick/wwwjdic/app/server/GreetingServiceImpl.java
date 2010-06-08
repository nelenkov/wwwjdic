package org.nick.wwwjdic.app.server;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.nick.wwwjdic.app.client.GreetingService;
import org.nick.wwwjdic.app.kanjivg.GzipBlob;
import org.nick.wwwjdic.app.kanjivg.Kanji;
import org.nick.wwwjdic.app.kanjivg.KanjiVgImporter;
import org.nick.wwwjdic.app.kanjivg.PMF;
import org.nick.wwwjdic.app.kanjivg.Stroke;
import org.nick.wwwjdic.app.shared.FieldVerifier;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class GreetingServiceImpl extends RemoteServiceServlet implements
        GreetingService {

    Logger logger = Logger.getLogger("GreetingServiceImpl");

    private KanjiVgImporter importer = new KanjiVgImporter();

    public String greetServer(String input) throws IllegalArgumentException {
        // Verify that the input is valid.
        if (!FieldVerifier.isValidName(input)) {
            // If the input is not valid, throw an IllegalArgumentException back
            // to
            // the client.
            throw new IllegalArgumentException(
                    "Name must be at least 4 characters long");
        }

        // EntityManager em = EMF.get().createEntityManager();
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Kanji kanji = new Kanji();
        kanji.setMidashi("™B");
        kanji.setUnicodeNumber("50b3");

        String[] paths = {
                "M33.01,17.39c0.11,1.16,0.2,2.61-0.1,4.03c-1.93,8.99-9.37,26.36-18.35,38.55",
                "M27.53,42.5c0.59,0.61,0.76,1.97,0.76,3.23c0,13.02-0.29,39.65-0.29,48.4",
                "M43.85,20.27c0.95,0.47,2.69,0.57,3.64,0.47c9.27-0.99,22.02-3.24,33.32-3.87c1.58-0.09,2.53,0.22,3.32,0.46",
                "M43.81,28.83c0.6,0.42,1.37,1.99,1.48,2.55c0.81,4.02,1.85,11.8,3.04,20.23",
                "M46.12,30.36c9.15-0.81,29.21-3.38,35.45-3.5c2.58-0.05,2.79,1.27,2.6,3.47c-0.38,4.33-0.92,11.18-2.46,18.98",
                "M48.78,39.79c6.9-0.21,21.3-3.13,34.29-3.13",
                "M48.38,50.36c8.02-0.84,22.9-2.9,33.34-2.9",
                "M62.56,11.5c0.61,0.6,1.63,1.75,1.63,5.66c0,1.2-0.08,32.63-0.2,40.09",
                "M41.68,61.63c0.61,1.12,2.17,1.71,3.03,1.63c2.84-0.26,30.94-6.63,36.23-7.76",
                "M75.44,52.37c4.18,1.63,10.78,6.71,11.83,9.25",
                "M37.13,72.67c1.28,0.8,3.62,0.96,4.91,0.8C54.02,72,79,69,90.39,67.79c2.13-0.23,3.42,0.38,4.49,0.78",
                "M74.67,61.08c0.06,0.4,0.84,2.06,0.84,4.58c0,16.93-0.17,22.67-0.17,26.78c0,9.99-.05,1.48-6.52,0.21",
                "M50.26,78.87c2.5,1.72,6.46,7.07,7.08,9.75" };
        int strokeNum = 1;
        for (String path : paths) {
            Stroke s = new Stroke(strokeNum, "a", path);
            kanji.getStrokes().add(s);
            strokeNum++;
        }

        Transaction tx = null;
        try {
            tx = pm.currentTransaction();
            tx.begin();

            pm.makePersistent(kanji);
            tx.commit();
            // em.persist(s);
            // kanji.getStrokes().add(key);
            // em.persist(kanji);
        } finally {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }

            // em.close();
            pm.close();
        }

        String serverInfo = getServletContext().getServerInfo();
        String userAgent = getThreadLocalRequest().getHeader("User-Agent");
        return "Hello, " + input + "!<br><br>I am running " + serverInfo
                + ".<br><br>It looks like you are using:<br>" + userAgent;
    }

    @SuppressWarnings("unchecked")
    public String findKanji(String unicodeNumber) {

        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query q = null;

        Transaction tx = null;
        try {
            // q = pm.newQuery(Kanji.class);
            // q.setFilter("unicodeNumber == unicodeNumberParam");
            // q.declareParameters("String unicodeNumberParam");

            q = pm.newQuery("select from org.nick.wwwjdic.app.kanjivg.Kanji "
                    + "where unicodeNumber == unicodeNumberParam "
                    + "parameters String unicodeNumberParam ");

            tx = pm.currentTransaction();
            tx.begin();

            List<Kanji> kanjis = (List<Kanji>) q.execute(unicodeNumber);
            if (kanjis.isEmpty()) {
                return "<empty>";
            }

            Kanji k = kanjis.get(0);
            String result = k.getMidashi() + "|" + k.getUnicodeNumber() + "|";

            List<Stroke> strokes = k.getStrokes();
            for (Stroke s : strokes) {
                result += s.getPath();
                result += "\n";
            }

            return result;

        } finally {
            if (tx != null && tx.isActive()) {
                tx.commit();
            }

            if (q != null) {
                q.closeAll();
            }

            pm.close();
        }
    }

    public int downloadKanjiVg(String url) {
        PersistenceManager pm = null;
        try {
            byte[] kanjivgGzip = downloadBlob(url);
            String[] paths = url.split("/");
            String name = paths[paths.length - 1];
            GzipBlob blob = new GzipBlob(name, kanjivgGzip);

            pm = PMF.get().getPersistenceManager();
            Transaction tx = null;
            try {
                tx = pm.currentTransaction();
                tx.begin();

                pm.makePersistent(blob);
                tx.commit();
            } finally {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
            }

            // GZIPInputStream in = new GZIPInputStream(new
            // ByteArrayInputStream(
            // kanjivgGzip));
            // KanjivgParser parser = new KanjivgParser(in);
            //
            // int numKanjis = 0;
            // pm = PMF.get().getPersistenceManager();
            //
            // while (parser.hasNext()) {
            // Kanji kanji = parser.next();
            // logger.info("saving: " + kanji.getMidashi());
            // saveKanji(pm, kanji);
            // logger.info("done");
            // numKanjis++;
            // }
            //
            // return numKanjis;
            return 1;
        }
        // catch (IOException e) {
        // throw new RuntimeException(e);
        // }
        finally {
            if (pm != null) {
                pm.close();
            }
        }
    }

    private byte[] downloadBlob(String urlStr) {
        BufferedInputStream bis = null;
        try {
            URL url = new URL(urlStr);
            InputStream is = url.openStream();
            bis = new BufferedInputStream(is);

            byte[] buff = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            int read = 0;
            while ((read = bis.read(buff)) != -1) {
                out.write(buff, 0, read);
            }

            return out.toByteArray();

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public int processKanjiVg() {
        return importer.processKanjiVg(50);
    }
}
