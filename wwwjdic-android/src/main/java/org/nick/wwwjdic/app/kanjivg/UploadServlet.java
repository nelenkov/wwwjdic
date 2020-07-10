package org.nick.wwwjdic.app.kanjivg;

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import static com.googlecode.objectify.ObjectifyService.ofy;

@Singleton
public class UploadServlet extends HttpServlet {

    private static final long serialVersionUID = 801647943597138887L;

    private BlobstoreService blobstoreService = BlobstoreServiceFactory
            .getBlobstoreService();

    private Logger log = Logger.getLogger(UploadServlet.class.getSimpleName());

    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        Map<String, List<BlobInfo>> blobInfos = blobstoreService.getBlobInfos(req);
        BlobInfo blobInfo = blobInfos.get("kanjivg").get(0);

        log.info("blobInfo: " + blobInfo);

        Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(req);
        BlobKey blobKey = blobs.get("kanjivg").get(0);

        String keyStr = blobKey.getKeyString();

        byte[] data = blobstoreService.fetchData(blobKey, 0, blobInfo.getSize() - 1);
        KanjivgParser parser = new KanjivgParser(new GZIPInputStream(new ByteArrayInputStream(
                data)));
        int numKanji = parser.countKanji();
        log.info("numKanji: " + numKanji);

        GzipBlob blob = new GzipBlob(blobInfo.getFilename(), data);
        blob.setTotalKanjis(numKanji);

        ofy().save().entity(blob).now();

        res.sendRedirect("/upload.jsp?blobKey=" +
                keyStr + "&size=" + blobInfo.getSize() +
                "&filename=" + blobInfo.getFilename() + "&numKanji=" + numKanji);
    }

}
