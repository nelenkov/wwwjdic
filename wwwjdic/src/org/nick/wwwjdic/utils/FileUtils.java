
package org.nick.wwwjdic.utils;

import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
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

    public static byte[] readFromUri(Context ctx, Uri uri) {
        InputStream in = null;
        try {
            in = ctx.getContentResolver().openInputStream(uri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buff = new byte[1024];
            int count = -1;
            while ((count = in.read(buff)) != -1) {
                baos.write(buff, 0, count);
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (in != null) {
                FileUtils.closeSilently(in);
            }
        }
    }

    public static void closeSilently(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
            }
        }
    }
}
