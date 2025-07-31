package org.nick.wwwjdic.hkr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.http.entity.AbstractHttpEntity;

@SuppressWarnings("deprecation")
public class KrEntity extends AbstractHttpEntity {

    private final byte[] contentBytes;

    public KrEntity(String content) {
      contentBytes = content.getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public InputStream getContent() throws IllegalStateException {
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
