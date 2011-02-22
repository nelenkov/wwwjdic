package org.nick.wwwjdic.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    private FileUtils() {
    }

    public static String readTextFile(InputStream in) throws IOException {
        return readTextFile(in, "ASCII");
    }

    public static String readTextFile(InputStream in, String encoding)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte buff[] = new byte[1024];

        int len = -1;
        while ((len = in.read(buff)) != -1) {
            baos.write(buff, 0, len);
        }

        return baos.toString(encoding);
    }
}
