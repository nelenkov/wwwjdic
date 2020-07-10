package org.nick.wwwjdic.app.kanjivg;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import static com.googlecode.objectify.ObjectifyService.ofy;

@Singleton
public class UpdateStrokesServlet extends HttpServlet {

    private static final long serialVersionUID = -8230188336713626037L;

    @Inject
    private Logger log;

    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try {
            ServletFileUpload upload = new ServletFileUpload();
            res.setContentType("text/plain");

            FileItemIterator iterator = upload.getItemIterator(req);
            while (iterator.hasNext()) {
                FileItemStream item = iterator.next();
                InputStream stream = item.openStream();

                if (item.isFormField()) {
                    log.warning("Got a form field: " + item.getFieldName());
                } else {
                    log.info("Got an uploaded file: " + item.getFieldName()
                            + ", name = " + item.getName());

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buff = new byte[2048];
                    int read = -1;
                    while((read = stream.read(buff, 0, buff.length)) != -1) {
                        baos.write(buff, 0, read);
                    }
                    byte[] data = baos.toByteArray();
                    log.info(String.format("read %d bytes from %s", data.length, item.getName()));
                    if (data.length == 0) {
                        return;
                    }

                    KanjivgParser parser = new KanjivgParser(new GZIPInputStream(new ByteArrayInputStream(data)));
                    int numKanji = parser.countKanji();
                    log.info("numKanji: " + numKanji);

                    GzipBlob blob = new GzipBlob(item.getName(), data);
                    blob.setTotalKanjis(numKanji);

                    ofy().save().entity(blob).now();
                }

                res.sendRedirect("/update-strokes.xhtml");
            }
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }
}
