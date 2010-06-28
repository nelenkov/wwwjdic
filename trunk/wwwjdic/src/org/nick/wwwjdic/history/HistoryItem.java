package org.nick.wwwjdic.history;

import java.util.regex.Pattern;

import org.nick.wwwjdic.R;
import org.nick.wwwjdic.Radical;
import org.nick.wwwjdic.Radicals;
import org.nick.wwwjdic.SearchCriteria;
import org.nick.wwwjdic.StringUtils;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HistoryItem extends LinearLayout {

    private TextView searchTypeText;
    private TextView searchKeyText;
    private TextView criteriaDetailsText;

    private static final Pattern HEX_PATTERN = Pattern
            .compile("[0-9a-fA-F]{4}");
    private static final int JIS_IDX = 5;

    HistoryItem(Context context) {
        super(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.search_history_item, this);

        searchTypeText = (TextView) findViewById(R.id.search_type);
        searchKeyText = (TextView) findViewById(R.id.search_key);
        criteriaDetailsText = (TextView) findViewById(R.id.criteria_details);
    }

    public void populate(SearchCriteria criteria) {
        if (criteria.getType() == SearchCriteria.CRITERIA_TYPE_DICT) {
            searchTypeText.setText(R.string.hiragana_a);
        } else if (criteria.getType() == SearchCriteria.CRITERIA_TYPE_KANJI) {
            searchTypeText.setText(R.string.kanji_kan);
        } else {
            searchTypeText.setText(R.string.kanji_bun);
        }

        String searchKey = criteria.getQueryString();
        if (criteria.isKanjiRadicalLookup()) {
            Radical radical = Radicals.getInstance().getRadicalByNumber(
                    Integer.parseInt(searchKey));
            searchKey = radical.getGlyph() + " (" + radical.getNumber() + ")";
        }
        searchKeyText.setText(searchKey);

        String detailStr = buildDetailString(criteria);
        if (detailStr != null && !"".equals(detailStr)) {
            criteriaDetailsText.setText(detailStr);
        }
    }

    private String buildDetailString(SearchCriteria criteria) {
        StringBuffer buff = new StringBuffer();

        if (criteria.getType() == SearchCriteria.CRITERIA_TYPE_KANJI) {
            String kanjiSearchName = lookupKanjiSearchName(criteria);

            buff.append(kanjiSearchName);
            if (criteria.hasStrokes()) {
                buff.append(" (strokes: ");
                if (criteria.hasMinStrokes()) {
                    buff.append(criteria.getMinStrokeCount());
                }
                buff.append("-");
                if (criteria.hasMaxStrokes()) {
                    buff.append(criteria.getMaxStrokeCount());
                }
                buff.append(")");
            }
        } else {
            if (criteria.getType() == SearchCriteria.CRITERIA_TYPE_DICT) {
                String dictName = lookupDictionaryName(criteria);

                buff.append(dictName);
            }

            String dictOptStr = buildSearchOptionsString(criteria);
            if (!StringUtils.isEmpty(dictOptStr)) {
                buff.append(" ");
                buff.append(dictOptStr);
            }
        }

        String result = buff.toString();
        if (!StringUtils.isEmpty(result)) {
            return result.trim();
        }

        return result;
    }

    private String buildSearchOptionsString(SearchCriteria criteria) {
        StringBuffer buff = new StringBuffer();
        if (criteria.isCommonWordsOnly()) {
            buff.append(" comm.");
        }

        if (criteria.isExactMatch()) {
            buff.append(" ex.");
        }

        if (criteria.isRomanizedJapanese()) {
            buff.append(" rom.");
        }

        if (criteria.getType() == SearchCriteria.CRITERIA_TYPE_EXAMPLES) {
            buff.append(String.format(" max results: %d", criteria
                    .getNumMaxResults()));
        }

        String result = buff.toString().trim();
        if (!StringUtils.isEmpty(result)) {
            result = "(" + result + ")";
        }

        return result;
    }

    private String lookupDictionaryName(SearchCriteria criteria) {
        String dictCode = criteria.getDictionary();
        String dictName = dictCode;

        String[] dictCodes = getResources().getStringArray(
                R.array.dictionary_codes_array);
        String[] dictNames = getResources().getStringArray(
                R.array.dictionaries_array);
        int idx = linearSearch(dictCode, dictCodes);

        if (idx != -1 && idx < dictNames.length - 1) {
            dictName = dictNames[idx];
        }
        return dictName;
    }

    private String lookupKanjiSearchName(SearchCriteria criteria) {
        String kanjiSearchCode = criteria.getKanjiSearchType();
        String kanjiSearchName = kanjiSearchCode;

        String[] searchCodes = getResources().getStringArray(
                R.array.kanji_search_codes_array);
        String[] searchNames = getResources().getStringArray(
                R.array.kanji_search_types_array);
        int idx = linearSearch(kanjiSearchCode, searchCodes);

        if (idx != -1 && idx < searchNames.length) {
            kanjiSearchName = searchNames[idx];
        }

        // ugly, but no other way(?) to differentiate between reading search and
        // JIS code search
        if (isJisSearch(criteria)) {
            kanjiSearchName = searchNames[JIS_IDX];
        }

        return kanjiSearchName;
    }

    private boolean isJisSearch(SearchCriteria criteria) {
        if (!"J".equals(criteria.getKanjiSearchType())) {
            return false;
        }

        return HEX_PATTERN.matcher(criteria.getQueryString()).matches();
    }

    private int linearSearch(String key, String[] arr) {
        for (int i = 0; i < arr.length; i++) {
            String code = arr[i];
            if (code.equals(key)) {
                return i;
            }
        }

        return -1;
    }

}
