package com.nightfarmer.smartcamera.audiovideosample;

/**
 * Created by zhangfan on 17-8-2.
 */

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Thread for asynchronous operation of camera preview
 */
public class CameraThread extends Thread {
    boolean DEBUG = true;

    int CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_BACK;

    private final Object mReadyFence = new Object();
    private final WeakReference<CameraGLView> mWeakParent;
    private CameraHandler mHandler;
    public volatile boolean mIsRunning = false;
    private Camera mCamera;
    private boolean mIsFrontFace;

    public CameraThread(final CameraGLView parent) {
        super("Camera thread");
        mWeakParent = new WeakReference<CameraGLView>(parent);
    }

    public CameraHandler getHandler() {
        synchronized (mReadyFence) {
            try {
                mReadyFence.wait();
            } catch (final InterruptedException e) {
            }
        }
        return mHandler;
    }

    /**
     * message loop
     * prepare Looper and create Handler for this thread
     */
    @Override
    public void run() {
        if (DEBUG) Log.d(TAG, "Camera thread start");
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new CameraHandler(this);
            mIsRunning = true;
            mReadyFence.notify();
        }
        Looper.loop();
        if (DEBUG) Log.d(TAG, "Camera thread finish");
        synchronized (mReadyFence) {
            mHandler = null;
            mIsRunning = false;
        }
    }

    /**
     * start camera preview
     *
     * @param width
     * @param height
     */
    public final void startPreview(final int width, final int height) {
        if (DEBUG) Log.v(TAG, "startPreview:");
        final CameraGLView parent = mWeakParent.get();
        if ((parent != null) && (mCamera == null)) {
            // This is a sample project so just use 0 as camera ID.
            // it is better to selecting camera is available
            try {
                mCamera = Camera.open(CAMERA_ID);
                final Camera.Parameters params = mCamera.getParameters();
                final List<String> focusModes = params.getSupportedFocusModes();
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                } else {
                    if (DEBUG) Log.i(TAG, "Camera does not support autofocus");
                }
                // let's try fastest frame rate. You will get near 60fps, but your device become hot.
                final List<int[]> supportedFpsRange = params.getSupportedPreviewFpsRange();
//					final int n = supportedFpsRange != null ? supportedFpsRange.size() : 0;
//					int[] range;
//					for (int i = 0; i < n; i++) {
//						range = supportedFpsRange.get(i);
//						Log.i(TAG, String.format("supportedFpsRange(%d)=(%d,%d)", i, range[0], range[1]));
//					}
                final int[] max_fps = supportedFpsRange.get(supportedFpsRange.size() - 1);
                Log.i(TAG, String.format("fps:%d-%d", max_fps[0], max_fps[1]));
                params.setPreviewFpsRange(max_fps[0], max_fps[1]);
//==============V1.4新增调整↓
                List<Integer> frameRates = params.getSupportedPreviewFrameRates();
                if (frameRates != null) {
                    Integer max = Collections.max(frameRates);
                    params.setPreviewFrameRate(max);
                }

//                params.setExposureCompensation(params.getMaxExposureCompensation());
                if(params.isAutoExposureLockSupported()) {
                    params.setAutoExposureLock(false);
                }
//                final int[] previewFpsRange = new int[2];
//                params.getPreviewFpsRange(previewFpsRange);
//                if (previewFpsRange[0] == previewFpsRange[1]) {
//                    final List<int[]> supportedFpsRanges = params.getSupportedPreviewFpsRange();
//                    for (int[] range : supportedFpsRanges) {
//                        if (range[0] != range[1]) {
//                            params.setPreviewFpsRange(range[0], range[1]);
//                            break;
//                        }
//                    }
//                }
//                params.setRecordingHint(true);
//                params.set("scene-mode", "night");
//                hehe(params);
//                setBestExposure(params,false);
//                params.setExposureCompensation();
//==============V1.4新增调整↑
                // request closest supported preview size
//                Camera.Size closestSize = getClosestSupportedSize(
//                        params.getSupportedPreviewSizes(), width, height);
                Point closestSize = getBestCameraResolution(params, true);
                params.setPreviewSize(closestSize.x, closestSize.y);
                // request closest picture size for an aspect ratio issue on Nexus7
//                final Camera.Size pictureSize = getClosestSupportedSize(
//                        params.getSupportedPictureSizes(), width, height);
                Point pictureSize = getBestCameraResolution(params, false);
                params.setPictureSize(pictureSize.x, pictureSize.y);
                // rotate camera preview according to the device orientation
                setRotation(params);
                mCamera.setParameters(params);
                // get the actual preview size
                final Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
                Log.i(TAG, String.format("previewSize(%d, %d)", previewSize.width, previewSize.height));
                // adjust view size with keeping the aspect ration of camera preview.
                // here is not a UI thread and we should request parent view to execute.
                parent.post(new Runnable() {
                    @Override
                    public void run() {
                        parent.setVideoSize(previewSize.width, previewSize.height);
                    }
                });
                final SurfaceTexture st = parent.getSurfaceTexture();
                st.setDefaultBufferSize(previewSize.width, previewSize.height);
                mCamera.setPreviewTexture(st);
            } catch (final IOException e) {
                Log.e(TAG, "startPreview:", e);
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                }
            } catch (final RuntimeException e) {
                Log.e(TAG, "startPreview:", e);
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                }
            }
            if (mCamera != null) {
                // start camera preview display
                mCamera.startPreview();
            }
        }
    }

    void hehe(Camera.Parameters params){
        if (params.getMaxNumMeteringAreas() > 0){ // check that metering areas are supported

            List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();

            Rect areaRect1 = new Rect(-100, -100, 100, 100);    // specify an area in center of image

            meteringAreas.add(new Camera.Area(areaRect1, 600)); // set weight to 60%

            Rect areaRect2 = new Rect(800, -1000, 1000, -800);  // specify an area in upper right of image

            meteringAreas.add(new Camera.Area(areaRect2, 400)); // set weight to 40%

            params.setMeteringAreas(meteringAreas);

        }
    }

    private static final float MAX_EXPOSURE_COMPENSATION = 3.5f;
    private static final float MIN_EXPOSURE_COMPENSATION = 0.0f;
    public static void setBestExposure(Camera.Parameters parameters, boolean lightOn) {
        int minExposure = parameters.getMinExposureCompensation();
        int maxExposure = parameters.getMaxExposureCompensation();
        float step = parameters.getExposureCompensationStep();
        if ((minExposure != 0 || maxExposure != 0) && step > 0.0f) {
            // Set low when light is on
            float targetCompensation = lightOn ? MIN_EXPOSURE_COMPENSATION : MAX_EXPOSURE_COMPENSATION;
            int compensationSteps = Math.round(targetCompensation / step);
            float actualCompensation = step * compensationSteps;
            // Clamp value:
            compensationSteps = Math.max(Math.min(compensationSteps, maxExposure), minExposure);
            if (parameters.getExposureCompensation() == compensationSteps) {
                Log.i(TAG, "Exposure compensation already set to " + compensationSteps + " / " + actualCompensation);
            } else {
                Log.i(TAG, "Setting exposure compensation to " + compensationSteps + " / " + actualCompensation);
                parameters.setExposureCompensation(compensationSteps);
            }
        } else {
            Log.i(TAG, "Camera does not support exposure compensation");
        }
    }

    private static Camera.Size getClosestSupportedSize(List<Camera.Size> supportedSizes, final int requestedWidth, final int requestedHeight) {
        return (Camera.Size) Collections.min(supportedSizes, new Comparator<Camera.Size>() {

            private int diff(final Camera.Size size) {
                return Math.abs(requestedWidth - size.width) + Math.abs(requestedHeight - size.height);
            }

            @Override
            public int compare(final Camera.Size lhs, final Camera.Size rhs) {
                return diff(lhs) - diff(rhs);
            }
        });

    }

    /**
     * stop camera preview
     */
    public void stopPreview() {
        if (DEBUG) Log.v(TAG, "stopPreview:");
        releaseCurrent();
        final CameraGLView parent = mWeakParent.get();
        if (parent == null) return;
        parent.mCameraHandler = null;
    }

    private void releaseCurrent() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * rotate preview screen according to the device orientation
     *
     * @param params
     */
    public final void setRotation(final Camera.Parameters params) {
        if (DEBUG) Log.v(TAG, "setRotation:");
        final CameraGLView parent = mWeakParent.get();
        if (parent == null) return;

        final Display display = ((WindowManager) parent.getContext()
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        final int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        // get whether the camera is front camera or back camera
        final Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(CAMERA_ID, info);
        mIsFrontFace = (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
        if (mIsFrontFace) {    // front camera
            degrees = (info.orientation + degrees) % 360;
            degrees = (360 - degrees) % 360;  // reverse
        } else {  // back camera
            degrees = (info.orientation - degrees + 360) % 360;
        }
        // apply rotation setting
        mCamera.setDisplayOrientation(degrees);
        parent.mRotation = degrees;
        // XXX This method fails to call and camera stops working on some devices.
//			params.setRotation(degrees);
    }


    /**
     * 获取最佳预览大小
     *
     * @param parameters 相机参数
     * @return
     */
    private Point getBestCameraResolution(Camera.Parameters parameters, boolean isPreView) {
        Point screenSize = getScreenSize(mWeakParent.get().getContext());

        float mindiff = 100f;
        float x_d_y = (float) screenSize.x / (float) screenSize.y;
        Point best = new Point();

        List<Camera.Size> supportedPreviewSizes = new ArrayList<>();
        if (isPreView) {
            supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        } else {
            supportedPreviewSizes = parameters.getSupportedPictureSizes();
        }
        for (Camera.Size size : supportedPreviewSizes) {
            float tmp = Math.abs(1f * size.height / size.width - x_d_y);
            Log.i("xxx", "" + size.height + " # " + size.width);
//            Log.i("xxx", "" + screenSize.x + " # " + screenSize.y + " # " + tmp);
            if (tmp < mindiff || tmp == mindiff && size.width > best.x) {
                Log.i("xxx", "替换 " + tmp + " # " + mindiff);
                mindiff = tmp;
                best.x = size.width;
                best.y = size.height;
            }
        }
        Log.i("xxxx", "best---- " + best.x + " # " + best.y + "--------------------------全屏误差：" + mindiff);
        return best;
    }


    //获取屏幕原始尺寸高度，包括虚拟功能键高度
    public static Point getScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Point point = new Point();
        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealMetrics(displayMetrics);
            point.x = displayMetrics.widthPixels;
            point.y = displayMetrics.heightPixels;
        } else {
            display.getSize(point);
        }
        return point;
    }

    public void switchCamera(int cameraId) {
        releaseCurrent();
        CAMERA_ID = cameraId;
        startPreview(1280, 720);
    }
}