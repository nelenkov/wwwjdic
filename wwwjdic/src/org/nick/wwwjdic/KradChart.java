package org.nick.wwwjdic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;

public class KradChart extends Activity implements OnItemClickListener {

    private static final int NUM_KRAD_RADICALS = 252;

    private String[] radicals;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.krad_chart);

        GridView radicalChartGrid = (GridView) findViewById(R.id.kradChartGrid);
        radicalChartGrid.setOnItemClickListener(this);

        List<String> radicalsList = new ArrayList<String>();

        try {
            for (int i = 1; i <= 14; i++) {
                radicalsList.add(Integer.toString(i));
                String arrayName = "_" + i + "_stroke";
                Field field = R.array.class.getField(arrayName);
                int resourceId = (Integer) field.get(null);
                String[] radicalArr = getResources().getStringArray(resourceId);
                radicalsList.addAll(Arrays.asList(radicalArr));
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        radicalsList.add(Integer.toString(17));
        String[] radicalArr = getResources().getStringArray(R.array._17_stroke);
        radicalsList.addAll(Arrays.asList(radicalArr));

        radicals = radicalsList.toArray(new String[radicalsList.size()]);
        radicalChartGrid.setAdapter(new ArrayAdapter<String>(this,
                R.layout.krad_item, radicals));

        setTitle(R.string.select_radical);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        // Radicals radicals = Radicals.getInstance();
        // Radical radical = radicals.getRadical(position);
        //
        // Intent resultIntent = new Intent();
        // Bundle bundle = new Bundle();
        // bundle.putSerializable(Constants.RADICAL_KEY, radical);
        // resultIntent.putExtras(bundle);
        //
        // setResult(RESULT_OK, resultIntent);
        //
        // finish();
    }
}
