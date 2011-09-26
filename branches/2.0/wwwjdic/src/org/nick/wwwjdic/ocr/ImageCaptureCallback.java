package org.nick.wwwjdic.ocr;

import static org.nick.wwwjdic.WwwjdicPreferences.ACRA_DEBUG;

import java.io.OutputStream;

import org.acra.ErrorReporter;

import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ImageCaptureCallback implements PictureCallback {

    private static final String TAG = ImageCaptureCallback.class
            .getSimpleName();

    private OutputStream fileoutputStream;
    private Handler handler;

    public ImageCaptureCallback(OutputStream os, Handler handler) {
        this.fileoutputStream = os;
        this.handler = handler;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        try {
            fileoutputStream.write(data);
            fileoutputStream.flush();
            fileoutputStream.close();

            Message msg = handler.obtainMessage(OcrActivity.PICTURE_TAKEN, -1,
                    -1);
            handler.sendMessage(msg);

        } catch (Exception ex) {
            Log.e(TAG, "onPictureTaken error: " + ex.getMessage(), ex);
            if (ACRA_DEBUG) {
                ErrorReporter.getInstance().handleException(ex);
            }
        }
    }
}
