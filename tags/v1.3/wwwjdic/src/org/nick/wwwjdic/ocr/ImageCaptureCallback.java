package org.nick.wwwjdic.ocr;

import java.io.OutputStream;

import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Handler;
import android.os.Message;

public class ImageCaptureCallback implements PictureCallback {

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
            ex.printStackTrace();
        }
    }
}
