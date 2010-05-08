package org.nick.wwwjdic.ocr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
            + "Content-Disposition: form-data; name=\"outputencoding\"\r\n"
            + "\r\n" + "utf-8\r\n" + "--" + BOUNDARY + "--\r\n"
            + "Content-Disposition: form-data; name=\"eclass\"\r\n" + "\r\n"
            + "text_line\r\n" + "--" + BOUNDARY + "--\r\n";

    private static final int BUFF_SIZE = 2048;
    private static final int IMG_QUALITY = 90;

    private ByteArrayOutputStream buff;

    public OcrFormEntity(Bitmap bitmap, int quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFF_SIZE);
        bitmap.compress(CompressFormat.JPEG, quality, baos);
        baos.close();

        buff = baos;
        setContentType(CONTENT_TYPE);
        setChunked(false);
    }

    public OcrFormEntity(Bitmap img) throws IOException {
        this(img, IMG_QUALITY);
    }

    @Override
    public InputStream getContent() throws IOException, IllegalStateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getContentLength() {
        return buff.size() + HEADER.length() + TRAILER.length();
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
        os.write(TRAILER.getBytes("ascii"));
        os.flush();
    }
}
