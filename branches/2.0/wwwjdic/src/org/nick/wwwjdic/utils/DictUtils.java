package org.nick.wwwjdic.utils;

import org.nick.wwwjdic.R;
import org.nick.wwwjdic.model.WwwjdicEntry;

import android.content.Context;

public class DictUtils {

    private static final String DICT_COMMON_USAGE_MARKER = "(P)";

    private static final String DICT_VARIATION_DELIMITER = ";";

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

    public static String stripWwwjdicTags(Context ctx, String meaning) {
        String result = stripNumberTags(ctx, meaning);
        result = stripCommonWordTag(ctx, result);
        result = stripTags(ctx, R.array.wwwjdic_pos_tags, result);
        result = stripTags(ctx, R.array.wwwjdic_fields_tags, result);
        result = stripTags(ctx, R.array.wwwjdic_misc_tags, result);
        result = stripTags(ctx, R.array.wwwjdic_dialect_tags, result);
        result = stripCrossrefTags(ctx, result);
        result = stripLangTags(ctx, result);

        return result;
    }

    public static String stripCrossrefTags(Context ctx, String meaning) {
        String result = new String(meaning);
        return result.replaceAll("\\(See\\s\\S+\\)", "").trim();
    }

    public static String stripNumberTags(Context ctx, String meaning) {
        String result = new String(meaning);
        return result.replaceAll("\\(\\d+\\)", "").trim();
    }

    public static String stripLangTags(Context ctx, String meaning) {
        String result = new String(meaning);
        return result.replaceAll("\\(\\w{3}: \\S+\\)", "").trim();
    }

    public static String stripCommonWordTag(Context ctx, String meaning) {
        String result = new String(meaning);
        result = result.replaceAll("\\(P\\)", "").trim();
        result = result.replaceAll("\\(p\\)", "").trim();

        return result;
    }

    public static String stripPosTags(Context ctx, String meaning) {
        return stripTags(ctx, R.array.wwwjdic_pos_tags, meaning);
    }

    public static String stripTags(Context ctx, int arrayId, String meaning) {
        String[] tags = ctx.getResources().getStringArray(arrayId);
        String result = new String(meaning);
        for (String tag : tags) {
            result = result.replaceAll("\\(" + tag + "\\)", "").trim();
            result = result.replaceAll("\\(" + tag + "\\S+\\)", "").trim();
            result = result.replaceAll("\\{" + tag + "\\}", "").trim();
            result = result.replaceAll("\\{" + tag + "\\S+\\}", "").trim();
        }

        return result;
    }
}
