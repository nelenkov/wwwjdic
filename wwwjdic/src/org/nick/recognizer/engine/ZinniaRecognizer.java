package org.nick.recognizer.engine;

public class ZinniaRecognizer {

  static {
    System.loadLibrary("recognizer");
  }

  private long recognizerPtr;
  private long characterPtr;

  private String modelPath;

  public void init(String modelPath) {
    this.modelPath = modelPath;
    recognizerPtr = nativeInit(modelPath);
    if (recognizerPtr == 0) {
      throw new RuntimeException(
          "Failed to initialize recognizer. Broken model file?");
    }
  }

  public String getModelPath() {
    return modelPath;
  }

  private native long nativeInit(String modelPath);

  public void destroy() {
    nativeDestroy(recognizerPtr);
    recognizerPtr = 0;
  }

  private native void nativeDestroy(long recognizerPtr);

  public void startRecognition(int width, int height) {
    characterPtr = nativeStartRecognition(width, height);
  }

  private native long nativeStartRecognition(int width, int height);

  public void addPoint(int strokeNum, int x, int y) {
    nativeAddPoint(characterPtr, strokeNum, x, y);
  }

  private native void nativeAddPoint(long characterPtr, int strokeNum, int x,
      int y);

  public String[] recognize(int numCandidates) {
    return nativeRecognize(recognizerPtr, characterPtr, numCandidates);
  }

  private native String[] nativeRecognize(long recognizerPtr,
      long characterPtr, int numCandidates);

}
