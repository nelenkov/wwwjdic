package org.nick.wwwjdic.hkr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.http.entity.AbstractHttpEntity;

public class KrEntity extends AbstractHttpEntity {

    private String content;
    private byte[] contentBytes;

    public KrEntity(String content) {
        this.content = content;
        try {
            contentBytes = content.getBytes("ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getContent() throws IOException, IllegalStateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getContentLength() {
        return contentBytes.length;
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public boolean isStreaming() {
        return false;
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        os.write(contentBytes);
    }

}
