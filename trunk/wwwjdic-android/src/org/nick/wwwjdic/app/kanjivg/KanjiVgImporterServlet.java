package org.nick.wwwjdic.app.kanjivg;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class KanjiVgImporterServlet extends HttpServlet {

    private static final long serialVersionUID = -8302426431937372129L;

    private static final int BATCH_SIZE = 20;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        KanjiVgImporter importer = new KanjiVgImporter();
        int processed = importer.processKanjiVg(20);

        PrintWriter out = resp.getWriter();
        out.write("processed " + processed);
        out.flush();
        out.close();
    }

}
