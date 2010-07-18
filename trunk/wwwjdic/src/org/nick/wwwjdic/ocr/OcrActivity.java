package org.nick.wwwjdic.ocr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nick.wwwjdic.Analytics;
import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.DictionaryResultListView;
import org.nick.wwwjdic.ExamplesResultListView;
import org.nick.wwwjdic.KanjiResultListView;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.SearchCriteria;
import org.nick.wwwjdic.WebServiceBackedActivity;
import org.nick.wwwjdic.Wwwjdic;
import org.nick.wwwjdic.ocr.crop.CropImage;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.preference.PreferenceManager;
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
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class OcrActivity extends WebServiceBackedActivity implements
        SurfaceHolder.Callback, OnClickListener, OnTouchListener,
        OnCheckedChangeListener {

    private static final String TAG = OcrActivity.class.getSimpleName();

    private static final String WEOCR_DEFAULT_URL = "http://maggie.ocrgrid.org/cgi-bin/weocr/nhocr.cgi";

    // kind of arbitrary, but OCR seems to work fine with this, and we need to
    // keep picture size small for faster recognition
    private static final int MIN_PIXELS = 320 * 480;
    private static final int MAX_PIXELS = 640 * 480;

    private static final String PREF_DUMP_CROPPED_IMAGES_KEY = "pref_ocr_dump_cropped_images";
    private static final String PREF_WEOCR_URL_KEY = "pref_weocr_url";
    private static final String PREF_WEOCR_TIMEOUT_KEY = "pref_weocr_timeout";

    private static final String PREF_DIRECT_SEARCH_KEY = "pref_ocr_direct_search";

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

    private TextView ocrredTextView;
    private Button dictSearchButton;
    private Button kanjidictSearchButton;
    private Button exampleSearchButton;

    private ToggleButton flashToggle;
    private boolean supportsFlash = false;

    @Override
    protected void activityOnCreate(Bundle icicle) {
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
        exampleSearchButton = (Button) findViewById(R.id.send_to_example_search);
        exampleSearchButton.setOnClickListener(this);
        toggleSearchButtons(false);

        flashToggle = (ToggleButton) findViewById(R.id.auto_flash_toggle);
        flashToggle.setOnCheckedChangeListener(this);

        surfaceView.requestFocus();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Analytics.startSession(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Analytics.endSession(this);
    }

    private void toggleSearchButtons(boolean enabled) {
        dictSearchButton.setEnabled(enabled);
        kanjidictSearchButton.setEnabled(enabled);
        exampleSearchButton.setEnabled(enabled);
    }

    void autoFocus() {
        try {
            autoFocusInProgress = false;
            imageCaptureUri = createTempFile();
            if (imageCaptureUri == null) {
                Toast t = Toast.makeText(OcrActivity.this,
                        R.string.sd_file_create_failed, Toast.LENGTH_SHORT);
                t.show();

                return;
            }

            final ImageCaptureCallback captureCb = new ImageCaptureCallback(
                    getContentResolver().openOutputStream(imageCaptureUri),
                    handler);
            camera.takePicture(null, null, captureCb);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    void autoFocusFailed() {
        autoFocusInProgress = false;
        Toast t = Toast.makeText(OcrActivity.this, R.string.af_failed,
                Toast.LENGTH_SHORT);
        t.show();
    }

    void ocrSuccess(String ocrredText) {
        ocrredTextView.setTextSize(30f);
        ocrredTextView.setText(ocrredText);
        toggleSearchButtons(true);
    }

    void ocrFailed() {
        Toast t = Toast.makeText(this, R.string.ocr_failed, Toast.LENGTH_SHORT);
        t.show();
    }

    public static class OcrHandler extends WsResultHandler {

        public OcrHandler(OcrActivity ocrActivity) {
            super(ocrActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            OcrActivity ocrActivity = (OcrActivity) activity;
            if (activity == null) {
                return;
            }

            switch (msg.what) {
            case AUTO_FOCUS:
                if (msg.arg1 == 1) {
                    ocrActivity.autoFocus();
                } else {
                    ocrActivity.autoFocusFailed();
                }
                break;
            case OCRRED_TEXT:
                ocrActivity.dismissProgressDialog();
                int success = msg.arg1;
                if (success == 1) {
                    String ocrredText = (String) msg.obj;
                    ocrActivity.ocrSuccess(ocrredText);
                } else {
                    ocrActivity.ocrFailed();
                }
                break;
            case PICTURE_TAKEN:
                ocrActivity.crop();
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }

    protected WsResultHandler createHandler() {
        return new OcrHandler(this);
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
                WeOcrClient client = new WeOcrClient(getWeocrUrl(),
                        getWeocrTimeout());
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
            Intent intent = new Intent(this, CropImage.class);
            Bundle extras = new Bundle();
            // if we want to scale
            // extras.putInt("outputX", 200);
            // extras.putInt("outputY", 200);
            // extras.putBoolean("scale", true);
            intent.setDataAndType(imageCaptureUri, "image/jpeg");

            intent.putExtras(extras);
            startActivityForResult(intent, Constants.CROP_RETURN_RESULT);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            Toast t = Toast.makeText(OcrActivity.this,
                    R.string.cant_start_cropper, Toast.LENGTH_SHORT);
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
            File f = new File(imageCaptureUri.getPath());
            if (f.exists()) {
                boolean deleted = f.delete();
                Log.d(TAG, "deleted: " + deleted);
            }

            if (resultCode == RESULT_OK) {
                Bitmap cropped = (Bitmap) data.getExtras()
                        .getParcelable("data");
                try {
                    if (isDumpCroppedImages()) {
                        dumpBitmap(cropped, "cropped-color.jpg");
                    }

                    Bitmap blackAndWhiteBitmap = convertToGrayscale(cropped);

                    if (isDumpCroppedImages()) {
                        dumpBitmap(blackAndWhiteBitmap, "cropped.jpg");
                    }

                    Analytics.event("ocr", this);

                    OcrTask task = new OcrTask(blackAndWhiteBitmap, handler);
                    String message = getResources().getString(
                            R.string.doing_ocr);
                    submitWsTask(task, message);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast t = Toast.makeText(this, R.string.cancelled,
                        Toast.LENGTH_SHORT);
                t.show();
            }
        }
    }

    private boolean isDumpCroppedImages() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        return preferences.getBoolean(PREF_DUMP_CROPPED_IMAGES_KEY, false);
    }

    private boolean isDirectSearch() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        return preferences.getBoolean(PREF_DIRECT_SEARCH_KEY, false);
    }

    private int getWeocrTimeout() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        String timeoutStr = preferences.getString(PREF_WEOCR_TIMEOUT_KEY, "10");

        return Integer.parseInt(timeoutStr) * 1000;
    }

    private String getWeocrUrl() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        return preferences.getString(PREF_WEOCR_URL_KEY, WEOCR_DEFAULT_URL);
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
            File sdDir = Environment.getExternalStorageDirectory();
            File wwwjdicDir = new File(sdDir.getAbsolutePath() + "/wwwjdic");
            if (!wwwjdicDir.exists()) {
                wwwjdicDir.mkdir();
            }

            if (!wwwjdicDir.canWrite()) {
                return;
            }

            File imageFile = new File(wwwjdicDir, filename);

            FileOutputStream out = new FileOutputStream(imageFile
                    .getAbsolutePath());
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
                int previewWidth = (w >> 3) << 3;
                int previewHeight = (h >> 3) << 3;
                p.setPreviewSize(previewWidth, previewHeight);
            }

            if (pictureSize != null) {
                p.setPictureSize(pictureSize.width, pictureSize.height);
            } else {
                int pictureWidth = (w >> 3) << 3;
                int pictureHeight = (h >> 3) << 3;
                p.setPictureSize(pictureWidth, pictureHeight);
            }

            if (supportsFlash) {
                toggleFlash(flashToggle.isChecked(), p);
            }

            camera.setParameters(p);
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            isPreviewRunning = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Size> parseSizeListStr(String previewSizesStr) {
        List<Size> result = new ArrayList<Size>();
        String[] sizesStr = previewSizesStr.split(",");

        for (String s : sizesStr) {
            s = s.trim();

            int idx = s.indexOf('x');
            if (idx < 0) {
                Log.w(TAG, "Bad preview-size: " + previewSize);
                continue;
            }

            int width;
            int height;
            try {
                width = Integer.parseInt(s.substring(0, idx));
                height = Integer.parseInt(s.substring(idx + 1));
            } catch (NumberFormatException nfe) {
                Log.w(TAG, "Bad preview-size: " + previewSize);
                continue;
            }
            // why is this not static??
            result.add(camera.new Size(width, height));
        }

        return result;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();

        Camera.Parameters params = camera.getParameters();
        List<Size> supportedPreviewSizes = getSupportedPreviewSizes(params);
        List<Size> supportedPictueSizes = getSupportedPictureSizes(params);
        supportsFlash = ReflectionUtils.getFlashMode(params) != null;

        try {
            if (supportedPreviewSizes != null) {
                previewSize = getOptimalPreviewSize(supportedPreviewSizes);
                Log.d(TAG, String.format("preview width: %d; height: %d",
                        previewSize.width, previewSize.height));
                params.setPreviewSize(previewSize.width, previewSize.height);
                camera.setParameters(params);
            }

            if (supportedPictueSizes != null) {
                pictureSize = getOptimalPictureSize(supportedPictueSizes);
                Log.d(TAG, String.format("picture width: %d; height: %d",
                        pictureSize.width, pictureSize.height));
            }

            flashToggle.setEnabled(supportsFlash);
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Size> getSupportedPictureSizes(Camera.Parameters params) {
        List<Size> supportedPictureSizes = ReflectionUtils
                .getSupportedPictureSizes(params);

        if (supportedPictureSizes == null) {
            String supportedSizesStr = params.get("picture-size-values");
            if (supportedSizesStr == null) {
                supportedSizesStr = params.get("picture-size-value");
            }
            Log.d(TAG, "picture sizes: " + supportedSizesStr);
            if (supportedSizesStr != null) {
                supportedPictureSizes = parseSizeListStr(supportedSizesStr);
            }
        }

        return supportedPictureSizes;
    }

    private List<Size> getSupportedPreviewSizes(Camera.Parameters params) {
        List<Size> supportedPreviewSizes = ReflectionUtils
                .getSupportedPreviewSizes(params);

        if (supportedPreviewSizes == null) {
            String previewSizesStr = params.get("preview-size-values");
            if (previewSizesStr == null) {
                previewSizesStr = params.get("preview-size-value");
            }
            Log.d(TAG, "preview sizes: " + previewSizesStr);
            if (previewSizesStr != null) {
                supportedPreviewSizes = parseSizeListStr(previewSizesStr);
            }
        }

        return supportedPreviewSizes;
    }

    private Size getOptimalPictureSize(List<Size> supportedPictueSizes) {
        Size result = supportedPictueSizes.get(supportedPictueSizes.size() - 1);

        for (Size s : supportedPictueSizes) {
            int pixels = s.width * s.height;
            if (pixels >= MIN_PIXELS && pixels <= MAX_PIXELS) {
                return s;
            }
        }

        return result;
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

        boolean isDirectSearch = isDirectSearch();
        SearchCriteria criteria = null;
        Intent intent = new Intent(this, Wwwjdic.class);
        Bundle extras = new Bundle();

        switch (v.getId()) {
        case R.id.send_to_dict:
            if (isDirectSearch) {
                criteria = SearchCriteria.createForDictionaryDefault(key);
                intent = new Intent(this, DictionaryResultListView.class);
            } else {
                extras.putInt(Constants.SEARCH_TYPE,
                        SearchCriteria.CRITERIA_TYPE_DICT);
            }
            break;
        case R.id.send_to_kanjidict:
            if (isDirectSearch) {
                criteria = SearchCriteria.createForKanjiOrReading(key);
                intent = new Intent(this, KanjiResultListView.class);
            } else {
                extras.putInt(Constants.SEARCH_TYPE,
                        SearchCriteria.CRITERIA_TYPE_KANJI);
            }
            break;
        case R.id.send_to_example_search:
            if (isDirectSearch) {
                criteria = SearchCriteria.createForExampleSearchDefault(key);
                intent = new Intent(this, ExamplesResultListView.class);
            } else {
                extras.putInt(Constants.SEARCH_TYPE,
                        SearchCriteria.CRITERIA_TYPE_EXAMPLES);
            }
            break;
        default:
            // do nothing
        }

        if (isDirectSearch) {
            extras.putSerializable(Constants.CRITERIA_KEY, criteria);
        } else {
            extras.putString(Constants.SEARCH_TEXT_KEY, key);
        }

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
        if (isUseInternalStorage()) {
            sdDir = new File("/emmc");
        }

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

    private boolean isUseInternalStorage() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        return preferences.getBoolean("pref_ocr_use_internal_storage", false);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!supportsFlash) {
            return;
        }

        Camera.Parameters params = camera.getParameters();
        toggleFlash(isChecked, params);
    }

    private void toggleFlash(boolean useFlash, Camera.Parameters params) {
        String flashMode = "off";
        if (useFlash) {
            flashMode = "on";
        } else {
            flashMode = "off";
        }

        ReflectionUtils.setFlashMode(params, flashMode);
        camera.setParameters(params);
    }
}
