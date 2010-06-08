package org.nick.wwwjdic.app.kanjivg;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class KanjiStrokesServlet extends HttpServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 1176775953028953526L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String unicodeNumber = req.getPathInfo().replace("/", "");
        String kanji = findKanji(unicodeNumber);

        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/plain");

        PrintWriter out = resp.getWriter();
        out.write(kanji);
        out.flush();
        out.close();
    }

    @SuppressWarnings("unchecked")
    private String findKanji(String unicodeNumber) {

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
            String result = k.getMidashi() + " " + k.getUnicodeNumber() + "\n";

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

}
