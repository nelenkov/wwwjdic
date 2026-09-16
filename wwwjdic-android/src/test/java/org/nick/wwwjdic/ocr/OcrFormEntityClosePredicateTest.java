package org.nick.wwwjdic.ocr;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class OcrFormEntityClosePredicateTest extends TestCase {

    private static final String OCR_FORM_ENTITY_PATH =
            "wwwjdic/src/org/nick/wwwjdic/ocr/OcrFormEntity.java";

    public void testConstructorRetainsStreamWithoutClosingIt() throws Exception {
        String source = readSource(findOcrFormEntitySource());

        Matcher allocation = Pattern.compile(
                "ByteArrayOutputStream\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*="
                        + "\\s*new\\s+ByteArrayOutputStream\\s*\\(")
                .matcher(source);
        assertTrue("OcrFormEntity must allocate a ByteArrayOutputStream", allocation.find());

        String streamVariable = allocation.group(1);
        assertTrue("OcrFormEntity must compress the bitmap into the allocated stream",
                Pattern.compile("\\.compress\\s*\\([^;]*\\b"
                        + Pattern.quote(streamVariable) + "\\b[^;]*\\)")
                        .matcher(source).find());
        assertTrue("OcrFormEntity must retain the populated stream in buff",
                Pattern.compile("(?:this\\s*\\.\\s*)?buff\\s*=\\s*"
                        + Pattern.quote(streamVariable) + "\\s*;")
                        .matcher(source).find());
        assertTrue("OcrFormEntity must serialize buff through writeTo(OutputStream)",
                Pattern.compile("(?:this\\s*\\.\\s*)?buff\\s*\\.\\s*writeTo"
                        + "\\s*\\(\\s*os\\s*\\)")
                        .matcher(source).find());
        assertFalse("The locally allocated ByteArrayOutputStream must not be closed",
                Pattern.compile("\\b" + Pattern.quote(streamVariable)
                        + "\\s*\\.\\s*close\\s*\\(\\s*\\)")
                        .matcher(source).find());
    }

    private File findOcrFormEntitySource() throws IOException {
        File directory = new File(System.getProperty("user.dir")).getCanonicalFile();
        while (directory != null) {
            File source = new File(directory, OCR_FORM_ENTITY_PATH);
            if (source.isFile()) {
                return source;
            }
            directory = directory.getParentFile();
        }
        fail("Unable to locate " + OCR_FORM_ENTITY_PATH + " from the test working directory");
        return null;
    }

    private String readSource(File source) throws IOException {
        FileInputStream input = new FileInputStream(source);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), "UTF-8");
        } finally {
            input.close();
            output.close();
        }
    }
}
