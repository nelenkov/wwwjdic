package org.nick.wwwjdic;

import org.nick.wwwjdic.model.Radical;
import org.nick.wwwjdic.model.Radicals;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RadicalChart extends FragmentActivity implements
        OnItemClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.radical_chart);

        GridView radicalChartGrid = (GridView) findViewById(R.id.radicalChartGrid);
        radicalChartGrid.setOnItemClickListener(this);

        Radicals radicals = Radicals.getInstance();
        radicalChartGrid.setAdapter(new RadicalAdapter(this, radicals));

        setTitle(R.string.select_radical);
    }

    private static class RadicalAdapter extends BaseAdapter {

        private Context context;
        private Radicals radicals;

        public RadicalAdapter(Context context, Radicals radicals) {
            this.context = context;
            this.radicals = radicals;
        }

        public int getCount() {
            return radicals.getRadicals().size();
        }

        public Object getItem(int position) {
            return radicals.getRadicals().get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            Radical radical = radicals.getRadical(position);

            RadicalView result = null;
            if (convertView == null) {
                result = new RadicalView(context);
            } else {
                result = (RadicalView) convertView;
            }

            result.populate(radical);

            return result;
        }
    }

    static class RadicalView extends LinearLayout {

        private TextView radicalNumberText;
        private TextView numStrokesText;
        private TextView radicalText;

        RadicalView(Context context) {
            super(context);

            LayoutInflater inflater = LayoutInflater.from(context);
            inflater.inflate(R.layout.radicals_item, this);

            radicalNumberText = (TextView) findViewById(R.id.radical_number_text);
            numStrokesText = (TextView) findViewById(R.id.num_strokes_text);
            radicalText = (TextView) findViewById(R.id.radical_text);
        }

        void populate(Radical radical) {
            radicalNumberText.setText(Integer.toString(radical.getNumber()));
            numStrokesText.setText(Integer.toString(radical.getNumStrokes()));
            radicalText.setText(radical.getGlyph());
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        Radicals radicals = Radicals.getInstance();
        Radical radical = radicals.getRadical(position);

        Intent resultIntent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.RADICAL_KEY, radical);
        resultIntent.putExtras(bundle);

        setResult(RESULT_OK, resultIntent);

        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent intent = new Intent(this, Wwwjdic.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
