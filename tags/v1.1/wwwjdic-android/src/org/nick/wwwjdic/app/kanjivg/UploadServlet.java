package org.nick.wwwjdic.app.kanjivg;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;

public class UploadServlet extends HttpServlet {

    private static final long serialVersionUID = 801647943597138887L;

    private BlobstoreService blobstoreService = BlobstoreServiceFactory
            .getBlobstoreService();

    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        Map<String, BlobKey> blobs = blobstoreService.getUploadedBlobs(req);
        BlobKey blobKey = blobs.get("kanjivg");

        String keyStr = blobKey.getKeyString();

        res.sendRedirect("/upload.jsp?blobKey=" + keyStr);
    }

}
