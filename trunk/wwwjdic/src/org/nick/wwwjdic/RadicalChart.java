package org.nick.wwwjdic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
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
		GridView gridview = (GridView) findViewById(R.id.radicalChartGrid);
		gridview.setAdapter(new RadicalAdapter(this, radicals));

		setTitle("Select radical");
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
			LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.FILL_PARENT);
			numberStrokesLayout.setLayoutParams(params);

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
