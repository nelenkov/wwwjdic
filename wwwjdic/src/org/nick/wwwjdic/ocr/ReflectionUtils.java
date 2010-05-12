package org.nick.wwwjdic.ocr;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Size;

public class ReflectionUtils {

    private static Method Parameters_getSupportedPreviewSizes;
    private static Method Parameters_getSupportedPictureSizes;

    private static Method Parameters_getFlashMode;
    private static Method Parameters_setFlashMode;

    static {
        initCompatibility();
    };

    private ReflectionUtils() {
    }

    private static void initCompatibility() {
        try {
            Parameters_getSupportedPreviewSizes = Camera.Parameters.class
                    .getMethod("getSupportedPreviewSizes", new Class[] {});
            Parameters_getSupportedPictureSizes = Camera.Parameters.class
                    .getMethod("getSupportedPictureSizes", new Class[] {});

            Parameters_getFlashMode = Camera.Parameters.class.getMethod(
                    "getFlashMode", new Class[] {});
            Parameters_setFlashMode = Camera.Parameters.class.getMethod(
                    "setFlashMode", String.class);
        } catch (NoSuchMethodException nsme) {
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Size> getSupportedPreviewSizes(Camera.Parameters p) {
        try {
            if (Parameters_getSupportedPreviewSizes != null) {
                return (List<Size>) Parameters_getSupportedPreviewSizes
                        .invoke(p);
            } else {
                return null;
            }
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Size> getSupportedPictureSizes(Camera.Parameters p) {
        try {
            if (Parameters_getSupportedPictureSizes != null) {
                return (List<Size>) Parameters_getSupportedPictureSizes
                        .invoke(p);
            } else {
                return null;
            }
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            return null;
        }
    }

    public static String getFlashMode(Camera.Parameters p) {
        try {
            if (Parameters_getFlashMode != null) {
                return (String) Parameters_getFlashMode.invoke(p);
            } else {
                return null;
            }
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            return null;
        }
    }

    public static void setFlashMode(Camera.Parameters p, String flashMode) {
        try {
            if (Parameters_setFlashMode != null) {
                Parameters_setFlashMode.invoke(p, flashMode);
            }
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
        }
    }
}
