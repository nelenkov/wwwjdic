package org.nick.wwwjdic.history;

import org.nick.wwwjdic.R;
import org.nick.wwwjdic.model.WwwjdicEntry;
import org.nick.wwwjdic.utils.CheckableLinearLayout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class FavoritesItem extends CheckableLinearLayout implements
        OnCheckedChangeListener {

    static interface FavoriteStatusChangedListener {
        void onStatusChanged(boolean isFavorite, WwwjdicEntry entry);
    }

    private TextView isKanjiText;
    private TextView dictHeadingText;
    private TextView entryDetailsText;
    private CheckBox starCb;

    private FavoriteStatusChangedListener favoriteStatusChangedListener;

    private WwwjdicEntry entry;

    public FavoritesItem(Context context) {
        super(context);
    }

    public FavoritesItem(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public FavoritesItem(Context context, AttributeSet attributeSet,
            int defStyle) {
        super(context, attributeSet, defStyle);
    }

    FavoritesItem(Context context,
            FavoriteStatusChangedListener statusChangedListener) {
        super(context);
        this.favoriteStatusChangedListener = statusChangedListener;

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.favorites_item, this);

        isKanjiText = (TextView) findViewById(R.id.is_kanji);
        dictHeadingText = (TextView) findViewById(R.id.dict_heading);
        entryDetailsText = (TextView) findViewById(R.id.entry_details);
        starCb = (CheckBox) findViewById(R.id.star);
        starCb.setOnCheckedChangeListener(this);
    }

    public void populate(WwwjdicEntry entry) {
        this.entry = entry;

        isKanjiText.setText(entry.isKanji() ? R.string.kanji_kan
                : R.string.hiragana_a);
        dictHeadingText.setText(entry.getHeadword());

        String detailStr = entry.getDetailString();
        if (detailStr != null && !"".equals(detailStr)) {
            entryDetailsText.setText(detailStr);
        }

        starCb.setOnCheckedChangeListener(null);
        // starCb.setChecked(criteria.isFavorite());
        starCb.setChecked(true);
        starCb.setOnCheckedChangeListener(this);

        setChecked(false);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        favoriteStatusChangedListener.onStatusChanged(isChecked, entry);
    }

}
