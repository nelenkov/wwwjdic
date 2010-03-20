package org.nick.wwwjdic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class RadicalChart extends Activity implements OnItemClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.radical_chart);

        GridView radicalChartGrid = (GridView) findViewById(R.id.radicalChartGrid);
        radicalChartGrid.setOnItemClickListener(this);

        Radicals radicals = Radicals.getInstance();
        if (!radicals.isInitialized()) {
            radicals.addRadicals(1,
                    getIntArray(R.array.one_stroke_radical_numbers),
                    getStrArray(R.array.one_stroke_radicals));
            radicals.addRadicals(2,
                    getIntArray(R.array.two_stroke_radical_numbers),
                    getStrArray(R.array.two_stroke_radicals));
            radicals.addRadicals(3,
                    getIntArray(R.array.three_stroke_radical_numbers),
                    getStrArray(R.array.three_stroke_radicals));
            radicals.addRadicals(4,
                    getIntArray(R.array.four_stroke_radical_numbers),
                    getStrArray(R.array.four_stroke_radicals));
            radicals.addRadicals(5,
                    getIntArray(R.array.five_stroke_radical_numbers),
                    getStrArray(R.array.five_stroke_radicals));
            radicals.addRadicals(6,
                    getIntArray(R.array.six_stroke_radical_numbers),
                    getStrArray(R.array.six_stroke_radicals));
            radicals.addRadicals(7,
                    getIntArray(R.array.seven_stroke_radical_numbers),
                    getStrArray(R.array.seven_stroke_radicals));
            radicals.addRadicals(8,
                    getIntArray(R.array.eight_stroke_radical_numbers),
                    getStrArray(R.array.eight_stroke_radicals));
            radicals.addRadicals(9,
                    getIntArray(R.array.nine_stroke_radical_numbers),
                    getStrArray(R.array.nine_stroke_radicals));
            radicals.addRadicals(10,
                    getIntArray(R.array.ten_stroke_radical_numbers),
                    getStrArray(R.array.ten_stroke_radicals));
            radicals.addRadicals(11,
                    getIntArray(R.array.eleven_stroke_radical_numbers),
                    getStrArray(R.array.eleven_stroke_radicals));
            radicals.addRadicals(12,
                    getIntArray(R.array.twelve_stroke_radical_numbers),
                    getStrArray(R.array.twelve_stroke_radicals));
            radicals.addRadicals(13,
                    getIntArray(R.array.thirteen_stroke_radical_numbers),
                    getStrArray(R.array.thirteen_stroke_radicals));
            radicals.addRadicals(14,
                    getIntArray(R.array.fourteen_stroke_radical_numbers),
                    getStrArray(R.array.fourteen_stroke_radicals));
            radicals.addRadicals(15,
                    getIntArray(R.array.fivteen_stroke_radical_numbers),
                    getStrArray(R.array.fivteen_stroke_radicals));
            radicals.addRadicals(16,
                    getIntArray(R.array.sixteen_stroke_radical_numbers),
                    getStrArray(R.array.sixteen_stroke_radicals));
            radicals.addRadicals(17,
                    getIntArray(R.array.seventeen_stroke_radical_numbers),
                    getStrArray(R.array.seventeen_stroke_radicals));
        }

        GridView gridview = (GridView) findViewById(R.id.radicalChartGrid);
        gridview.setAdapter(new RadicalAdapter(this, radicals));

        setTitle("Select radical");
    }

    private int[] getIntArray(int id) {
        return getResources().getIntArray(id);
    }

    private String[] getStrArray(int id) {
        return getResources().getStringArray(id);
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

            LinearLayout radicalLayout = new LinearLayout(context);
            radicalLayout.setOrientation(LinearLayout.HORIZONTAL);

            LinearLayout numberStrokesLayout = new LinearLayout(context);
            numberStrokesLayout.setOrientation(LinearLayout.VERTICAL);

            TextView numberText = new TextView(context);
            numberText.setText(Integer.toString(radical.getNumber()));
            numberText.setTextSize(12f);
            numberStrokesLayout.addView(numberText);

            TextView numStrokesText = new TextView(context);
            numStrokesText.setText(Integer.toString(radical.getNumStrokes()));
            numStrokesText.setTextSize(12f);
            numberStrokesLayout.addView(numStrokesText);

            radicalLayout.addView(numberStrokesLayout);

            TextView glyphText = new TextView(context);
            glyphText.setText(radical.getGlyph());
            glyphText.setTextSize(34f);
            glyphText.setTextColor(Color.WHITE);
            radicalLayout.addView(glyphText);

            return radicalLayout;
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
}
