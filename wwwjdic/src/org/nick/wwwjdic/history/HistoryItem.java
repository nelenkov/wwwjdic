package org.nick.wwwjdic.history;

import org.nick.wwwjdic.R;
import org.nick.wwwjdic.Radical;
import org.nick.wwwjdic.Radicals;
import org.nick.wwwjdic.SearchCriteria;
import org.nick.wwwjdic.utils.StringUtils;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HistoryItem extends LinearLayout {

    private TextView searchTypeText;
    private TextView searchKeyText;
    private TextView criteriaDetailsText;

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
            String kanjiSearchName = HistoryUtils.lookupKanjiSearchName(
                    criteria.getKanjiSearchType(), criteria.getQueryString(),
                    getContext());

            buff.append(kanjiSearchName);
            if (criteria.hasStrokes()) {
                buff.append(String.format(" (%s: ",
                        getStr(R.string.strokes_short)));
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
                String dictName = HistoryUtils.lookupDictionaryName(criteria,
                        getContext());

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

    private String getStr(int id) {
        return getResources().getString(id);
    }

    private String buildSearchOptionsString(SearchCriteria criteria) {
        StringBuffer buff = new StringBuffer();
        if (criteria.isCommonWordsOnly()) {
            buff.append(String.format(" %s", getStr(R.string.common_short)));
        }

        if (criteria.isExactMatch()) {
            buff.append(String.format(" %s", getStr(R.string.exact_short)));
        }

        if (criteria.isRomanizedJapanese()) {
            buff.append(String.format(" %s", getStr(R.string.romanized_short)));
        }

        if (criteria.getType() == SearchCriteria.CRITERIA_TYPE_EXAMPLES) {
            String message = getStr(R.string.max_results_short);
            buff.append(String.format(" " + message, criteria
                    .getNumMaxResults()));
        }

        String result = buff.toString().trim();
        if (!StringUtils.isEmpty(result)) {
            result = "(" + result + ")";
        }

        return result;
    }

}
