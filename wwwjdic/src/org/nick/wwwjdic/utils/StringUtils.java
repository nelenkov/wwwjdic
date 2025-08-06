package org.nick.wwwjdic.utils;

import java.util.List;

public class StringUtils {

    private StringUtils() {
    }

    public static String shorten(String string, int maxLength) {
        if (string.length() <= maxLength) {
            return string;
        }

        String result = string.substring(0, maxLength - 3);
        result += "...";

        return result;
    }

    public static String join(String[] fields, String separator, int idx) {
        StringBuilder buff = new StringBuilder();
        for (int i = idx; i < fields.length; i++) {
            buff.append(fields[i]);
            if (i != fields.length - 1) {
                buff.append(separator);
            }
        }

        return buff.toString();
    }

    public static String join(List<String> fields, String separator, int idx) {
        String[] fieldsArr = fields.toArray(new String[0]);

        return join(fieldsArr, separator, idx);
    }

    public static boolean isEmpty(String str) {
        if (str == null) {
            return true;
        }

        return str.isEmpty();
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}
