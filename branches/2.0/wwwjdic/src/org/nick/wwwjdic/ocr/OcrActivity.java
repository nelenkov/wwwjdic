package org.nick.wwwjdic.ocr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.acra.ErrorReporter;
import org.nick.wwwjdic.Constants;
import org.nick.wwwjdic.DictionaryResultListView;
import org.nick.wwwjdic.ExamplesResultListView;
import org.nick.wwwjdic.KanjiResultListView;
import org.nick.wwwjdic.R;
import org.nick.wwwjdic.SearchCriteria;
import org.nick.wwwjdic.WebServiceBackedActivity;
import org.nick.wwwjdic.Wwwjdic;
import org.nick.wwwjdic.WwwjdicPreferences;
import org.nick.wwwjdic.ocr.crop.CropImage;
import org.nick.wwwjdic.utils.Analytics;
import org.nick.wwwjdic.utils.Dialogs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
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
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class OcrActivity extends WebServiceBackedActivity implements
        SurfaceHolder.Callback, OnClickListener, OnTouchListener,
        OnCheckedChangeListener {

    private static final String TAG = OcrActivity.class.getSimpleName();

    // kind of arbitrary, but OCR seems to work fine with this, and we need to
    // keep picture size small for faster recognition
    private static final int MIN_PIXELS = 320 * 480;
    private static final int MAX_PIXELS = 640 * 480;

    private static final String IMAGE_CAPTURE_URI_KEY = "ocr.imageCaptureUri";

    private static final int CROP_REQUEST_CODE = 1;
    private static final int SELECT_IMAGE_REQUEST_CODE = 2;

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

    private Button pickImageButton;

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

        camera = CameraHolder.getInstance().tryOpen();
        if (camera == null) {
            Dialogs.createFinishActivityAlertDialog(this,
                    R.string.camera_in_use_title,
                    R.string.camera_in_use_message).show();
            return;
        }

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

        pickImageButton = (Button) findViewById(R.id.pick_image);
        pickImageButton.setOnClickListener(this);

        flashToggle = (ToggleButton) findViewById(R.id.auto_flash_toggle);
        flashToggle.setOnCheckedChangeListener(this);

        if (icicle != null) {
            imageCaptureUri = icicle.getParcelable(IMAGE_CAPTURE_URI_KEY);
        }

        surfaceView.requestFocus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeCamera();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(IMAGE_CAPTURE_URI_KEY, imageCaptureUri);
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
            // if camera is closed, ignore
            if (camera == null) {
                return;
            }

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
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            deleteTempFile();

            ErrorReporter.getInstance().handleException(e);
            Toast t = Toast.makeText(OcrActivity.this,
                    R.string.image_capture_failed, Toast.LENGTH_SHORT);
            t.show();
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
                WeOcrClient client = new WeOcrClient(
                        WwwjdicPreferences.getWeocrUrl(OcrActivity.this),
                        WwwjdicPreferences.getWeocrTimeout(OcrActivity.this));
                String ocredText = client.sendLineOcrRequest(bitmap);
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
            Analytics.event("ocrTake", this);
            requestAutoFocus();
        }

        return false;
    }

    private void crop() {
        try {
            if (!tempFileExists()) {
                String path = "";
                if (imageCaptureUri != null) {
                    path = new File(imageCaptureUri.getPath())
                            .getAbsolutePath();
                }
                Log.w(TAG, "temp file does not exist: " + path);
                Toast.makeText(
                        this,
                        getResources().getString(R.string.read_picture_error,
                                path), Toast.LENGTH_SHORT).show();
            }

            callCropper(imageCaptureUri);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            Toast t = Toast.makeText(OcrActivity.this,
                    R.string.cant_start_cropper, Toast.LENGTH_SHORT);
            t.show();
        }
    }

    private void callCropper(Uri imageUri) {
        Intent intent = new Intent(this, CropImage.class);
        Bundle extras = new Bundle();
        // if we want to scale
        // extras.putInt("outputX", 200);
        // extras.putInt("outputY", 200);
        // extras.putBoolean("scale", true);
        intent.setDataAndType(imageUri, "image/jpeg");

        intent.putExtras(extras);
        startActivityForResult(intent, CROP_REQUEST_CODE);
    }

    private void requestAutoFocus() {
        if (autoFocusInProgress) {
            return;
        }

        autoFocusInProgress = true;
        try {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    sendAutoFocusResultMessage(success);
                }
            });
        } catch (RuntimeException e) {
            Log.e(TAG, "auto focusFailed: " + e.getMessage(), e);
            sendAutoFocusResultMessage(false);
        } finally {
            toggleSearchButtons(false);
            ocrredTextView.setText("");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CROP_REQUEST_CODE) {
            deleteTempFile();

            if (resultCode == RESULT_OK) {
                Bitmap cropped = (Bitmap) data.getExtras()
                        .getParcelable("data");
                try {
                    if (WwwjdicPreferences.isDumpCroppedImages(this)) {
                        dumpBitmap(cropped, "cropped-color.jpg");
                    }

                    Bitmap blackAndWhiteBitmap = convertToGrayscale(cropped);

                    if (WwwjdicPreferences.isDumpCroppedImages(this)) {
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
        } else if (requestCode == SELECT_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Uri selectedImageUri = data.getData();
                callCropper(selectedImageUri);
            }
        }
    }

    private void deleteTempFile() {
        if (imageCaptureUri == null) {
            return;
        }

        File f = new File(imageCaptureUri.getPath());
        if (f.exists()) {
            boolean deleted = f.delete();
            Log.d(TAG, "deleted: " + deleted);
        }
    }

    private boolean tempFileExists() {
        if (imageCaptureUri == null) {
            return false;
        }

        File f = new File(imageCaptureUri.getPath());

        return f.exists();
    }

    private Bitmap convertToGrayscale(Bitmap bitmap) {
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        Paint paint = new Paint();
        ColorMatrixColorFilter cmcf = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(cmcf);

        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.RGB_565);

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

            FileOutputStream out = new FileOutputStream(
                    imageFile.getAbsolutePath());
            bitmap.compress(CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, String.format("surface changed: w=%d; h=%d)", w, h));

        if (holder.getSurface() == null) {
            Log.d(TAG, "holder.getSurface() == null");
            return;
        }

        if (holder != surfaceHolder) {
            return;
        }

        // camera is null if in use. in that case we finish(),
        // so just ignore it
        if (camera == null) {
            return;
        }

        // it seems surfaceChanged is sometimes called twice:
        // once with SCREEN_ORIENTATION_PORTRAIT and once with
        // SCREEN_ORIENTATION_LANDSCAPE. At least on a Sapphire with
        // CyanogenMod. Calling setPreviewSize with wrong width and
        // height leads to a FC, so skip it.
        if (w < h) {
            return;
        }

        if (isPreviewRunning) {
            stopPreview();
        }

        try {
            Camera.Parameters p = camera.getParameters();
            if (previewSize != null) {
                Log.d(TAG, String.format("previewSize: w=%d; h=%d",
                        previewSize.width, previewSize.height));
                p.setPreviewSize(previewSize.width, previewSize.height);
            } else {
                int previewWidth = (w >> 3) << 3;
                int previewHeight = (h >> 3) << 3;
                Log.d(TAG, String.format("previewSize: w=%d; h=%d",
                        previewWidth, previewHeight));
                p.setPreviewSize(previewWidth, previewHeight);
            }

            if (pictureSize != null) {
                Log.d(TAG, String.format("pictureSize: w=%d; h=%d",
                        pictureSize.width, pictureSize.height));
                p.setPictureSize(pictureSize.width, pictureSize.height);
            } else {
                int pictureWidth = (w >> 3) << 3;
                int pictureHeight = (h >> 3) << 3;
                Log.d(TAG, String.format("pictureSize: w=%d; h=%d",
                        pictureWidth, pictureHeight));
                p.setPictureSize(pictureWidth, pictureHeight);
            }

            if (supportsFlash) {
                CameraHolder.getInstance().toggleFlash(flashToggle.isChecked(),
                        p);
            }

            camera.setParameters(p);
            camera.setPreviewDisplay(holder);
            startPreview();
        } catch (Exception e) {
            Log.e(TAG, "error initializing camera: " + e.getMessage(), e);
            ErrorReporter.getInstance().handleException(e);
            Dialogs.createErrorDialog(this, R.string.ocr_error).show();
        }
    }

    private void startPreview() {
        if (isPreviewRunning) {
            stopPreview();
        }

        try {
            Log.v(TAG, "startPreview");
            camera.startPreview();
        } catch (Throwable ex) {
            closeCamera();
            throw new RuntimeException("startPreview failed", ex);
        }

        isPreviewRunning = true;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");

        if (camera == null) {
            camera = CameraHolder.getInstance().tryOpen();
            if (camera == null) {
                Dialogs.createFinishActivityAlertDialog(this,
                        R.string.camera_in_use_title,
                        R.string.camera_in_use_message).show();
                return;
            }
        }

        Camera.Parameters params = camera.getParameters();
        List<Size> supportedPreviewSizes = CameraHolder.getInstance()
                .getSupportedPreviewSizes(params);
        if (supportedPreviewSizes != null) {
            Log.d(TAG, "supported preview sizes");
            for (Size s : supportedPreviewSizes) {
                Log.d(TAG, String.format("%dx%d", s.width, s.height));
            }
        }
        List<Size> supportedPictueSizes = CameraHolder.getInstance()
                .getSupportedPictureSizes(params);
        if (supportedPictueSizes != null) {
            Log.d(TAG, "supported picture sizes:");
            for (Size s : supportedPictueSizes) {
                Log.d(TAG, String.format("%dx%d", s.width, s.height));
            }
        }
        supportsFlash = CameraHolder.getInstance().supportsFlash(params);

        try {
            if (supportedPreviewSizes != null
                    && !supportedPreviewSizes.isEmpty()) {
                previewSize = getOptimalPreviewSize(supportedPreviewSizes);
                Log.d(TAG, String.format("preview width: %d; height: %d",
                        previewSize.width, previewSize.height));
                params.setPreviewSize(previewSize.width, previewSize.height);
                camera.setParameters(params);
            }

            if (supportedPictueSizes != null && !supportedPictueSizes.isEmpty()) {
                pictureSize = getOptimalPictureSize(supportedPictueSizes);
                Log.d(TAG, String.format("picture width: %d; height: %d",
                        pictureSize.width, pictureSize.height));
            }

            flashToggle.setEnabled(supportsFlash);
            camera.setPreviewDisplay(holder);
        } catch (Exception e) {
            Log.e(TAG, "error initializing camera: " + e.getMessage(), e);
            ErrorReporter.getInstance().handleException(e);
            Dialogs.createErrorDialog(this, R.string.ocr_error).show();
        }
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
        int targetWidth = windowManager.getDefaultDisplay().getWidth();

        Size result = null;
        double diff = Double.MAX_VALUE;
        for (Size size : sizes) {
            double newDiff = Math.abs(size.width - targetWidth)
                    + Math.abs(size.height - targetHeight);
            if (newDiff == 0) {
                result = size;
                break;
            } else if (newDiff < diff) {
                diff = newDiff;
                result = size;
            }
        }

        return result;
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
        closeCamera();
    }

    private void stopPreview() {
        if (camera != null && isPreviewRunning) {
            camera.stopPreview();
        }
        isPreviewRunning = false;
    }

    private void closeCamera() {
        if (camera != null) {
            CameraHolder.getInstance().release();
            camera = null;
            isPreviewRunning = false;
        }
    }

    @Override
    public void onClick(View v) {
        TextView t = (TextView) findViewById(R.id.ocrredText);
        String key = t.getText().toString();

        boolean isDirectSearch = WwwjdicPreferences.isDirectSearch(this);
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
        case R.id.pick_image:
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            // only in API 11
            intent.putExtra("android.intent.extra.LOCAL_ONLY", true);
            startActivityForResult(intent, SELECT_IMAGE_REQUEST_CODE);
            return;
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
        Analytics.event("ocrTouch", this);
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
        CameraHolder.getInstance().toggleFlash(isChecked, params);
    }

    private void sendAutoFocusResultMessage(boolean success) {
        Message msg = handler.obtainMessage(AUTO_FOCUS, success ? 1 : 0, -1);
        handler.sendMessage(msg);
    }

}
