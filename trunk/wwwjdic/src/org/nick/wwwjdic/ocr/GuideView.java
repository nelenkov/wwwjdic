package org.nick.wwwjdic.ocr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class GuideView extends View {

	private static int GUIDE_HEIGHT = 50;
	private static int FIRST_GUIDE_Y = 50;

	public GuideView(Context context) {
		super(context);
	}

	public GuideView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		final Paint paint = new Paint();
		final Rect rect = new Rect();

		int width = canvas.getWidth();
		int height = canvas.getHeight();

		int usedHeight = height - FIRST_GUIDE_Y;
		int numGuides = usedHeight / GUIDE_HEIGHT;

		int top = 0;
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(Color.LTGRAY);
		for (int i = 0; i < numGuides; i++) {
			if (i == 0) {
				top = FIRST_GUIDE_Y;
			} else {
				top = top + GUIDE_HEIGHT;
			}
			rect.set(0, top, width, top + GUIDE_HEIGHT);
			canvas.drawRect(rect, paint);
		}
	}
}
