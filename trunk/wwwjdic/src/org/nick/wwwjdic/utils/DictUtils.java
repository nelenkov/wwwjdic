package org.nick.wwwjdic.utils;

import static org.nick.wwwjdic.Constants.DICT_COMMON_USAGE_MARKER;
import static org.nick.wwwjdic.Constants.DICT_VARIATION_DELIMITER;

import org.nick.wwwjdic.WwwjdicEntry;

public class DictUtils {

    private DictUtils() {
    }

    public static String extractSearchKey(WwwjdicEntry wwwjdicEntry) {
        String searchKey = wwwjdicEntry.getHeadword();
        if (searchKey.indexOf(DICT_VARIATION_DELIMITER) != -1) {
            String[] variations = searchKey.split(DICT_VARIATION_DELIMITER);
            searchKey = variations[0];
            searchKey = searchKey.replace(DICT_COMMON_USAGE_MARKER, "");
        }
        return searchKey;
    }
}
