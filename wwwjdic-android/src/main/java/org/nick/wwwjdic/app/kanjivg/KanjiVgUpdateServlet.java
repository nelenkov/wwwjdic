package org.nick.wwwjdic.app.kanjivg;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Singleton
public class KanjiVgUpdateServlet extends HttpServlet {

    private static final long serialVersionUID = -8302426431937372129L;

    private static final int BATCH_SIZE = 50;

    @Inject
    KanjiVgImporter importer;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int processed = importer.processKanjiVg(BATCH_SIZE, true);

        PrintWriter out = resp.getWriter();
        out.write("processed " + processed);
        out.flush();
        out.close();
    }

}
