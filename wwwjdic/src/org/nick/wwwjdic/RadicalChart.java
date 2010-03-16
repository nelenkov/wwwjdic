package org.nick.wwwjdic;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

public class RadicalChart extends Activity {

    private final static String[] RADICALS = new String[] { "ìÒ", "ò≥", "êl", "âª",
            "ò¢", "ôX", "ì¸", "î™", "ôc", "ôk", "ôq", "ô{", "ôÅ", "ìÅ", "óÕ", "ôØ", "ô∂",
            "ô∑ ", "è\", "ñm", "ô≈", "ô ", "ô—", "ñî", "çû" };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.radical_chart);

        GridView gridview = (GridView) findViewById(R.id.radicalChartGrid);
        gridview.setAdapter(new RadicalAdapter(this));

    }

    private static class RadicalAdapter extends BaseAdapter {

        private Context context;

        public RadicalAdapter(Context context) {
            this.context = context;
        }

        public int getCount() {
            return RADICALS.length;
        }

        public Object getItem(int position) {
            return RADICALS[position];
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextView text = new TextView(context);
            text.setText(RADICALS[position]);
            text.setTextSize(32f);

            return text;
        }
    }
}
