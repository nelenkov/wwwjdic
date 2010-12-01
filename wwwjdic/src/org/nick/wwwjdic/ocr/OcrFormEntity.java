package org.nick.wwwjdic.ocr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import org.apache.http.entity.AbstractHttpEntity;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;

public class OcrFormEntity extends AbstractHttpEntity {

    private static final String BOUNDARY = "--------------abcdefghijklmnopqrstu";
    private static final String CONTENT_TYPE = "multipart/form-data; boundary="
            + BOUNDARY;

    private static final String HEADER = "--"
            + BOUNDARY
            + "\r\n"
            + "Content-Disposition: form-data; name=\"userfile\"; filename=\"file.jpg\"\r\n"
            + "Content-Type: image/jpeg\r\n"
            + "Content-Transfer-Encoding: binary\r\n" + "\r\n";
    private static final String TRAILER = "\r\n" + "--" + BOUNDARY + "\r\n"
            + "Content-Disposition: form-data; name=\"outputformat\"\r\n"
            + "\r\n" + "txt\r\n" + "--" + BOUNDARY + "\r\n"
            + "Content-Disposition: form-data; name=\"eclass\"\r\n" + "\r\n"
            + "%s\r\n" + "--" + BOUNDARY + "\r\n"
            + "Content-Disposition: form-data; name=\"ntop\"\r\n" + "\r\n"
            + "%d\r\n" + "--" + BOUNDARY + "\r\n"
            + "Content-Disposition: form-data; name=\"outputencoding\"\r\n"
            + "\r\n" + "utf-8\r\n" + "--" + BOUNDARY + "--\r\n";

    private static final int BUFF_SIZE = 2048;
    private static final int IMG_QUALITY = 90;

    private ByteArrayOutputStream buff;
    private String trailer;

    public OcrFormEntity(Bitmap bitmap, int quality, String eclass,
            Integer numberOfTopCandidates) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFF_SIZE);
        bitmap.compress(CompressFormat.JPEG, quality, baos);
        baos.close();

        buff = baos;
        int ntop = 1;
        if (numberOfTopCandidates != null) {
            ntop = numberOfTopCandidates;
        }
        trailer = String.format(Locale.US, TRAILER, eclass, ntop);
        setContentType(CONTENT_TYPE);
        setChunked(false);
    }

    public OcrFormEntity(Bitmap img, String eclass,
            Integer numberOfTopCandidates) throws IOException {
        this(img, IMG_QUALITY, eclass, numberOfTopCandidates);
    }

    public OcrFormEntity(Bitmap img) throws IOException {
        this(img, IMG_QUALITY, "text_line", null);
    }

    @Override
    public InputStream getContent() throws IOException, IllegalStateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getContentLength() {
        return buff.size() + HEADER.length() + trailer.length();
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public boolean isStreaming() {
        return false;
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        if (os == null) {
            throw new IllegalArgumentException("Output stream can't be null.");
        }

        os.write(HEADER.getBytes("ascii"));
        buff.writeTo(os);

        os.write(trailer.getBytes("ascii"));
        os.flush();
    }
}
