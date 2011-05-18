package org.nick.wwwjdic.ocr;

import java.util.ArrayList;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;

public class CameraHolder {

    private static final String TAG = CameraHolder.class.getSimpleName();

    private static CameraHolder instance;

    private Camera camera;

    private CameraHolder() {
    }

    public static CameraHolder getInstance() {
        if (instance == null) {
            instance = new CameraHolder();
        }

        return instance;
    }

    public synchronized Camera open() {
        if (camera == null) {
            camera = Camera.open();
        }

        return camera;
    }

    public synchronized Camera tryOpen() {
        try {
            return open();
        } catch (RuntimeException e) {
            Log.e(TAG, "error opening camera: " + e.getMessage(), e);
            return null;
        }
    }

    public List<Size> getSupportedPictureSizes(Camera.Parameters params) {
        if (camera == null) {
            return null;
        }

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

    private List<Size> parseSizeListStr(String previewSizesStr) {
        List<Size> result = new ArrayList<Size>();
        String[] sizesStr = previewSizesStr.split(",");

        for (String s : sizesStr) {
            s = s.trim();

            int idx = s.indexOf('x');
            if (idx < 0) {
                Log.w(TAG, "Bad preview-size: " + s);
                continue;
            }

            int width;
            int height;
            try {
                width = Integer.parseInt(s.substring(0, idx));
                height = Integer.parseInt(s.substring(idx + 1));
            } catch (NumberFormatException nfe) {
                Log.w(TAG, "Bad preview-size: " + s);
                continue;
            }
            // why is this not static??
            result.add(camera.new Size(width, height));
        }

        return result;
    }

    public List<Size> getSupportedPreviewSizes(Camera.Parameters params) {
        if (camera == null) {
            return null;
        }

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

    public boolean supportsFlash(Camera.Parameters params) {
        return ReflectionUtils.getFlashMode(params) != null;
    }

    public synchronized void toggleFlash(boolean useFlash,
            Camera.Parameters params) {
        if (camera == null) {
            return;
        }

        String flashMode = "off";
        if (useFlash) {
            flashMode = "on";
        } else {
            flashMode = "off";
        }

        ReflectionUtils.setFlashMode(params, flashMode);
        camera.setParameters(params);
    }

    public synchronized void release() {
        if (camera == null) {
            return;
        }

        camera.stopPreview();
        camera.release();
        camera = null;

    }
}
