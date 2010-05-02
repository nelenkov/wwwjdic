package org.nick.wwwjdic.ocr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.Wwwjdic;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Bitmap.CompressFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class OcrActivity extends Activity implements SurfaceHolder.Callback,
		OnClickListener, OnTouchListener {

	private static final String TAG = OcrActivity.class.getSimpleName();

	private static final String WEOCR_URL = "http://maggie.ocrgrid.org/cgi-bin/weocr/nhocr.cgi";

	private Camera camera;
	private Size previewSize;
	private Size pictureSize;

	private boolean isPreviewRunning = false;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private Uri imageCaptureUri;

	private boolean autoFocusInProgress = false;
	private static final int AUTO_FOCUS = 1;
	protected static final int OCRRED_TEXT = 2;
	public static final int PICTURE_TAKEN = 3;

	protected ExecutorService ocrThread;
	protected Future transPending;
	private Handler handler;

	private ProgressDialog progressDialog;

	private TextView ocrredTextView;
	private Button dictSearchButton;
	private Button kanjidictSearchButton;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Log.e(TAG, "onCreate");

		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		window.setFormat(PixelFormat.TRANSLUCENT);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setContentView(R.layout.ocr);
		surfaceView = (SurfaceView) findViewById(R.id.capture_surface);
		surfaceView.setOnTouchListener(this);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		ocrredTextView = (TextView) findViewById(R.id.ocrredText);
		ocrredTextView.setTextSize(30f);

		dictSearchButton = (Button) findViewById(R.id.send_to_dict);
		dictSearchButton.setOnClickListener(this);
		kanjidictSearchButton = (Button) findViewById(R.id.send_to_kanjidict);
		kanjidictSearchButton.setOnClickListener(this);
		toggleSearchButtons(false);

		initThreading();
	}

	private void toggleSearchButtons(boolean enabled) {
		dictSearchButton.setEnabled(enabled);
		kanjidictSearchButton.setEnabled(enabled);
	}

	private void initThreading() {
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case AUTO_FOCUS:
					if (msg.arg1 == 1) {
						try {
							autoFocusInProgress = false;
							imageCaptureUri = createTempFile();
							if (imageCaptureUri == null) {
								Toast t = Toast
										.makeText(
												OcrActivity.this,
												"Could not create temp file on SD card.",
												Toast.LENGTH_SHORT);
								t.show();

								return;
							}

							final ImageCaptureCallback captureCb = new ImageCaptureCallback(
									getContentResolver().openOutputStream(
											imageCaptureUri), this);
							camera.takePicture(null, null, captureCb);
						} catch (IOException e) {
							Log.e(TAG, e.getMessage(), e);
							throw new RuntimeException(e);
						}
					} else {
						autoFocusInProgress = false;
						Toast t = Toast.makeText(OcrActivity.this,
								"A/F failed", Toast.LENGTH_SHORT);
						t.show();
					}
					break;
				case OCRRED_TEXT:
					progressDialog.dismiss();
					int success = msg.arg1;
					if (success == 1) {
						String ocrredText = (String) msg.obj;
						ocrredTextView.setTextSize(30f);
						ocrredTextView.setText(ocrredText);
						toggleSearchButtons(true);
					} else {
						Toast t = Toast.makeText(OcrActivity.this,
								"OCR failed", Toast.LENGTH_SHORT);
						t.show();
					}
					break;
				case PICTURE_TAKEN:
					crop();
					break;
				default:
					super.handleMessage(msg);
				}
			}
		};
		ocrThread = Executors.newSingleThreadExecutor();
	}

	class OcrTask implements Runnable {

		private Bitmap bitmap;
		private Handler handler;

		public OcrTask(Bitmap b, Handler h) {
			bitmap = b;
			handler = h;
		}

		@Override
		public void run() {
			try {
				WeOcrClient client = new WeOcrClient(WEOCR_URL);
				String ocredText = client.sendOcrRequest(bitmap);
				Log.d(TAG, "OCR result: " + ocredText);

				if (ocredText != null && !"".equals(ocredText)) {
					Message msg = handler.obtainMessage(OCRRED_TEXT, 1, 0);
					msg.obj = ocredText;
					handler.sendMessage(msg);
				} else {
					Log.d("TAG", "OCR failed: empty string returned");
					Message msg = handler.obtainMessage(OCRRED_TEXT, 0, 0);
					handler.sendMessage(msg);
				}
			} catch (Exception e) {
				Log.e("TAG", "OCR failed", e);
				Message msg = handler.obtainMessage(OCRRED_TEXT, 0, 0);
				handler.sendMessage(msg);
			}
		}
	}

	private void submitOcrTask(OcrTask ocrTask) {
		progressDialog = ProgressDialog.show(this, "", "Doing OCR...", true);
		transPending = ocrThread.submit(ocrTask);
	}

	Camera.PictureCallback pictureCallbackRaw = new Camera.PictureCallback() {
		public void onPictureTaken(byte[] data, Camera c) {
			OcrActivity.this.camera.startPreview();
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return super.onKeyDown(keyCode, event);
		}
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			requestAutoFocus();
		}

		return false;
	}

	private void crop() {
		try {
			Intent intent = new Intent("com.android.camera.action.CROP");
			Bundle extras = new Bundle();
			extras.putBoolean("noFaceDetection", false);
			extras.putBoolean("return-data", true);
			extras.putBoolean("scale", true);
			intent.setDataAndType(imageCaptureUri, "image/jpeg");

			intent.putExtras(extras);
			startActivityForResult(intent, Constants.CROP_RETURN_RESULT);

		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			Toast t = Toast.makeText(OcrActivity.this,
					"Could not start crop activity.", Toast.LENGTH_SHORT);
			t.show();
		}
	}

	private void requestAutoFocus() {
		if (autoFocusInProgress) {
			return;
		}

		autoFocusInProgress = true;
		camera.autoFocus(new Camera.AutoFocusCallback() {
			@Override
			public void onAutoFocus(boolean success, Camera camera) {
				Message msg = handler.obtainMessage(AUTO_FOCUS,
						success ? 1 : 0, -1);
				handler.sendMessage(msg);
			}
		});

		toggleSearchButtons(false);
		ocrredTextView.setText("");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Constants.CROP_RETURN_RESULT) {
			if (resultCode == RESULT_OK) {
				Bitmap cropped = (Bitmap) data.getExtras()
						.getParcelable("data");
				try {
					// dumpBitmap(cropped,
					// "/sdcard/dcim/Camera/cropped-color.jpg");

					File f = new File(imageCaptureUri.getPath());
					if (f.exists()) {
						boolean deleted = f.delete();
						Log.d(TAG, "deleted: " + deleted);
					}

					Bitmap blackAndWhiteBitmap = convertToGrayscale(cropped);

					// dumpBitmap(blackAndWhiteBitmap,
					// "/sdcard/dcim/Camera/cropped.jpg");

					OcrTask task = new OcrTask(blackAndWhiteBitmap, handler);
					submitOcrTask(task);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else if (resultCode == RESULT_CANCELED) {
				Toast t = Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT);
				t.show();
			}
		}
	}

	private Bitmap convertToGrayscale(Bitmap bitmap) {
		ColorMatrix colorMatrix = new ColorMatrix();
		colorMatrix.setSaturation(0);
		Paint paint = new Paint();
		ColorMatrixColorFilter cmcf = new ColorMatrixColorFilter(colorMatrix);
		paint.setColorFilter(cmcf);

		Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap
				.getHeight(), Bitmap.Config.RGB_565);

		Canvas drawingCanvas = new Canvas(result);
		Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		Rect dst = new Rect(src);
		drawingCanvas.drawBitmap(bitmap, src, dst, paint);

		return result;
	}

	private void dumpBitmap(Bitmap bitmap, String filename) {
		try {
			FileOutputStream out = new FileOutputStream(filename);
			bitmap.compress(CompressFormat.JPEG, 90, out);
			out.flush();
			out.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		if (holder != surfaceHolder) {
			return;
		}

		if (isPreviewRunning) {
			camera.stopPreview();
		}

		try {
			Camera.Parameters p = camera.getParameters();
			if (previewSize != null) {
				p.setPreviewSize(previewSize.width, previewSize.height);
			} else {
				if (w == 480) {
					p.setPreviewSize(w, h);
				}
			}

			if (w == 480) {
				p.setPictureSize(w, h);
			} else {
				if (pictureSize != null) {
					p.setPictureSize(pictureSize.width, pictureSize.height);
				}
			}

			camera.setParameters(p);
			camera.setPreviewDisplay(holder);
			camera.startPreview();
			isPreviewRunning = true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		camera = Camera.open();

		Camera.Parameters params = camera.getParameters();
		List<Size> supportedPreviewSizes = ReflectionUtils
				.getSupportedPreviewSizes(params);
		List<Size> supportedPictueSizes = ReflectionUtils
				.getSupportedPictureSizes(params);

		try {
			if (supportedPreviewSizes != null) {
				previewSize = getOptimalPreviewSize(supportedPreviewSizes);
				Log.d(TAG, String.format("preview width: %d; height: %d",
						previewSize.width, previewSize.height));
				params.setPreviewSize(previewSize.width, previewSize.height);
				camera.setParameters(params);
			}

			if (supportedPictueSizes != null) {
				pictureSize = supportedPictueSizes.get(supportedPictueSizes
						.size() - 1);
				Log.d(TAG, String.format("picture width: %d; height: %d",
						pictureSize.width, pictureSize.height));
			}
			camera.setPreviewDisplay(holder);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Size getOptimalPreviewSize(List<Size> sizes) {
		WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		int targetHeight = windowManager.getDefaultDisplay().getHeight();

		Size result = null;
		double minDiff = Double.MAX_VALUE;
		for (Size size : sizes) {
			if (Math.abs(size.height - targetHeight) < minDiff) {
				result = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		return result;
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		camera.stopPreview();
		isPreviewRunning = false;
		camera.release();
	}

	@Override
	public void onClick(View v) {
		TextView t = (TextView) findViewById(R.id.ocrredText);
		String key = t.getText().toString();

		Bundle extras = new Bundle();
		extras.putString(Constants.SEARCH_TEXT_KEY, key);

		switch (v.getId()) {
		case R.id.send_to_dict:
			extras.putBoolean(Constants.SEARCH_TEXT_KANJI_KEY, false);
			break;
		case R.id.send_to_kanjidict:
			extras.putBoolean(Constants.SEARCH_TEXT_KANJI_KEY, true);
			break;
		default:
		}

		Intent intent = new Intent(this, Wwwjdic.class);
		intent.putExtras(extras);

		startActivity(intent);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		requestAutoFocus();

		return false;
	}

	private Uri createTempFile() {
		File sdDir = Environment.getExternalStorageDirectory();
		File wwwjdicDir = new File(sdDir.getAbsolutePath() + "/wwwjdic");
		if (!wwwjdicDir.exists()) {
			wwwjdicDir.mkdir();
		}

		if (wwwjdicDir.exists() && wwwjdicDir.canWrite()) {
			return Uri.fromFile(new File(wwwjdicDir, "tmp_ocr_"
					+ String.valueOf(System.currentTimeMillis()) + ".jpg"));
		}

		return null;
	}
}
