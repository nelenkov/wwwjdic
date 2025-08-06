package org.nick.wwwjdic.hkr;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.File;
import org.nick.recognizer.engine.ZinniaRecognizer;
import org.nick.wwwjdic.WwwjdicPreferences;

public class KanjiCharacterRecognizer {

  public static final String TAG = KanjiCharacterRecognizer.class.getSimpleName();
  public static final String HKR_COMBINED_CMP_MODEL_FILENAME = "handwriting-ja-cmb-cmp.model";
  private ZinniaRecognizer recognizer;
  private final Context context;

  private boolean initialized = false;

  KanjiCharacterRecognizer(Context context) {
    this.context = context;
    recognizer = new ZinniaRecognizer();
  }

  public synchronized void initialize(File modelFile) {
    try {
      String modelAbsolutePath = modelFile.getAbsolutePath();
      recognizer.init(modelAbsolutePath);
      initialized = true;
      Log.d(TAG, "using model " + modelAbsolutePath);
    } catch (Exception e) {
      Log.e(TAG, "error initializing zinnia: " + e.getMessage(), e);
      recognizer = null;
      throw new RuntimeException(e);
      //notifyKrError();
    }
  }

  public synchronized boolean  isInitialized() {
    return initialized;
  }

  private void notifyKrError() {
    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    Notification notification = createErrorNotification();
    int notificationId = 1;
    nm.notify(notificationId, notification);
  }

  @SuppressWarnings("deprecation")
  private Notification createErrorNotification() {
    int icon = android.R.drawable.ic_notification_clear_all;
    CharSequence tickerText = context.getResources().getString(org.nick.wwwjdic.R.string.kr_error);
    long when = System.currentTimeMillis();

    Context context = this.context.getApplicationContext();
    Intent notificationIntent = new Intent(context, WwwjdicPreferences.class);
    PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
        notificationIntent, PendingIntent.FLAG_IMMUTABLE);

    Notification.Builder builder = new Notification.Builder(context);
    builder.setAutoCancel(false)
        .setTicker(tickerText)
        .setContentTitle(context.getResources().getString(
            org.nick.wwwjdic.R.string.corrupt_model_notify_title))
        .setContentText(context.getResources().getString(
            org.nick.wwwjdic.R.string.corrupt_model_notify_message))
        .setSmallIcon(icon)
        .setContentIntent(contentIntent)
        .setWhen(when);
    builder.build();

    return builder.getNotification();
  }

  @Override
  protected void finalize() {
    destroy();
  }

  public void destroy() {
    if (recognizer != null) {
      recognizer.destroy();
    }
  }

  //
  public void startRecognition(int width, int height) {
    recognizer.startRecognition(width, height);
  }
  public void addPoint(int strokeNum, int x, int y) {
    recognizer.addPoint(strokeNum, x, y);
  }

  public String[] recognize(int numCandidates) {
    return recognizer.recognize(numCandidates);
  }

}

