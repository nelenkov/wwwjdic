package org.nick.wwwjdic.app.kanjivg;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nick.wwwjdic.app.server.CacheController;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;

@Singleton
public class KanjiStrokesServlet extends HttpServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 1176775953028953526L;

    @Inject
    private Logger log;

    // @Inject
    // @RequestParameters
    // private Map<String, String[]> params;

    @Inject
    private KanjiDao kanjiDao;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String xuserAgent = req.getHeader("X-User-Agent");
            if (xuserAgent != null) {
                log.info("X-User-Agent: " + xuserAgent);
            }

            String xDeviceVersion = req.getHeader("X-Device-Version");
            if (xDeviceVersion != null) {
                log.info("X-Device-Version: " + xDeviceVersion);
            }

            // log.fine("Params: " + params);

            boolean useJson = false;
            String format = req.getParameter("f");
            if (format != null && format.equals("json")) {
                useJson = true;
            }

            String kanjiStr = req.getPathInfo().replace("/", "");
            log.info("kanjiStr: " + kanjiStr);

            String unicodeNumber = null;
            try {
                Integer.parseInt(kanjiStr, 16);
                unicodeNumber = kanjiStr;
            } catch (NumberFormatException e) {
                unicodeNumber = Integer.toHexString(
                        kanjiStr.charAt(0) | 0x10000).substring(1);
            }
            log.info("got request for " + unicodeNumber);

            if (useJson) {
                String kanji = findKanjiJson(unicodeNumber);
                if (kanji == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);

                    return;
                }

                resp.setCharacterEncoding("UTF-8");
                resp.setContentType("application/x-javascript");

                PrintWriter out = resp.getWriter();
                out.write(kanji);
                out.flush();
                out.close();
            } else {
                String kanji = findKanji(unicodeNumber);

                resp.setCharacterEncoding("UTF-8");
                resp.setContentType("text/plain");

                PrintWriter out = resp.getWriter();
                out.write(kanji);
                out.flush();
                out.close();
            }
        } catch (Exception e) {
            error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void error(String message, Throwable t) {
        log.log(Level.SEVERE, message, t);
    }

    private String findKanjiJson(String unicodeNumber) {
        Kanji k = null;

        Kanji cachedKanji = (Kanji) CacheController
                .get("json_" + unicodeNumber);
        if (cachedKanji != null) {
            k = cachedKanji;
            log.info("Got kanji from cache: " + unicodeNumber);
        } else {
            Objectify ofy = ObjectifyService.begin();

            k = ofy.query(Kanji.class).filter("unicodeNumber", unicodeNumber)
                    .get();

            if (k == null) {
                log.info(String.format("KanjiVG data for %s not found",
                        unicodeNumber));
                return null;
            }
            List<Stroke> strokes = ofy.query(Stroke.class)
                    .ancestor(new Key<Kanji>(Kanji.class, k.getId()))
                    .order("number").list();
            k.setStrokes(strokes);

            String key = "json_" + unicodeNumber;
            CacheController.put(key, k);
            log.info("Put kanji in cache: " + key);
        }

        try {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("kanji", k.getMidashi());
            jsonObj.put("unicode", k.getUnicodeNumber());

            JSONArray pathsArr = new JSONArray();
            List<Stroke> strokes = k.getStrokes();
            for (Stroke s : strokes) {
                String path = s.getPath();
                // log.info("path: " + path);
                if (!"".equals(path) && !"null".equals(path) && path != null) {
                    pathsArr.put(s.getPath());
                }
            }
            jsonObj.put("paths", pathsArr);

            return jsonObj.toString();
        } catch (JSONException e) {
            log.severe(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String findKanji(String unicodeNumber) {
        Kanji k = null;

        Kanji cachedKanji = (Kanji) CacheController.get(unicodeNumber);
        if (cachedKanji != null) {
            k = cachedKanji;
            log.info("Got kanji from cache: " + unicodeNumber);
        } else {
            Objectify ofy = ObjectifyService.begin();

            k = ofy.query(Kanji.class).filter("unicodeNumber", unicodeNumber)
                    .get();

            if (k == null) {
                log.info(String.format("KanjiVG data for %s not found",
                        unicodeNumber));
                return null;
            }
            List<Stroke> strokes = ofy.query(Stroke.class)
                    .ancestor(new Key<Kanji>(Kanji.class, k.getId())).list();
            k.setStrokes(strokes);

            CacheController.put(unicodeNumber, k);
            log.info("Put kanji in cache: " + unicodeNumber);
        }
        String result = k.getMidashi() + " " + k.getUnicodeNumber() + "\n";
        log.info("returning " + result);

        List<Stroke> strokes = k.getStrokes();
        for (Stroke s : strokes) {
            String path = s.getPath();
            // log.info("path: " + path);
            if (!"".equals(path) && !"null".equals(path) && path != null) {
                result += s.getPath();
                result += "\n";
            }
        }

        return result;

    }

}
