package org.nick.wwwjdic.app.kanjivg;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.nick.wwwjdic.app.server.CacheController;

public class UpdateStrokesServlet extends HttpServlet {

    /**
     * 
     */
    private static final long serialVersionUID = -8230188336713626037L;

    private static final Logger log = Logger
            .getLogger(UpdateStrokesServlet.class.getName());

    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try {
            ServletFileUpload upload = new ServletFileUpload();
            res.setContentType("text/plain");

            KanjiDao dao = new KanjiDao();

            FileItemIterator iterator = upload.getItemIterator(req);
            while (iterator.hasNext()) {
                FileItemStream item = iterator.next();
                InputStream stream = item.openStream();

                if (item.isFormField()) {
                    log.warning("Got a form field: " + item.getFieldName());
                } else {
                    log.info("Got an uploaded file: " + item.getFieldName()
                            + ", name = " + item.getName());

                    KanjSvgParser parser = new KanjSvgParser(stream);
                    Kanji kanji = parser.parse();
                    if (kanji == null) {
                        log.warning("Could not parse SVG");
                        continue;
                    }

                    PersistenceManager pm = PMF.get().getPersistenceManager();
                    try {
                        Kanji existing = dao.findKanji(pm,
                                kanji.getUnicodeNumber());
                        if (existing == null) {
                            log.warning(String.format(
                                    "Kanji %s not found. Nothing to update",
                                    kanji.getUnicodeNumber()));
                            continue;
                        }

                        List<Stroke> newStrokes = kanji.getStrokes();
                        List<Stroke> existingStrokes = existing.getStrokes();
                        for (int i = 0; i < existingStrokes.size(); i++) {
                            Stroke s = newStrokes.get(i);
                            Stroke old = existingStrokes.get(i);
                            log.info("old stroke: " + old);
                            log.info("new stroke: " + s);

                            old.setPath(s.getPath());
                            old.setNumber(s.getNumber());
                        }

                        log.info(String.format("Updated strokes for %s(%s)",
                                existing.getMidashi(),
                                existing.getUnicodeNumber()));
                        log.info(String.format("Removing %s from cache",
                                existing.getUnicodeNumber()));
                        CacheController.remove(existing.getUnicodeNumber());
                    } finally {
                        pm.close();
                    }
                }

                res.sendRedirect("/update-strokes.xhtml");
            }
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }
}
