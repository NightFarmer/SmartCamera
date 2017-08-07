package com.nightfarmer.smartcamera.audiovideosample;

/**
 * Created by zhangfan on 17-8-2.
 */

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import static android.content.ContentValues.TAG;

/**
 * Handler class for asynchronous camera operation
 */
public final class CameraHandler extends Handler {
    boolean DEBUG = true;


    private static final int MSG_PREVIEW_START = 1;
    private static final int MSG_PREVIEW_STOP = 2;
    private static final int MSG_PREVIEW_SWITCH = 3;
    private CameraThread mThread;

    public CameraHandler(final CameraThread thread) {
        mThread = thread;
    }

    public void startPreview(final int width, final int height) {
        sendMessage(obtainMessage(MSG_PREVIEW_START, width, height));
    }

    public void switchCamera(int cameraId) {
        sendMessage(obtainMessage(MSG_PREVIEW_SWITCH, cameraId));
    }

    /**
     * request to stop camera preview
     *
     * @param needWait need to wait for stopping camera preview
     */
    public void stopPreview(final boolean needWait) {
        Log.i("xxx", "stopPreview");
        synchronized (this) {
            sendEmptyMessage(MSG_PREVIEW_STOP);
            if (needWait && mThread.mIsRunning) {
                try {
                    if (DEBUG) Log.d(TAG, "wait for terminating of camera thread");
                    wait();
                } catch (final InterruptedException e) {
                }
            }
        }
    }

    /**
     * message handler for camera thread
     */
    @Override
    public void handleMessage(final Message msg) {
        switch (msg.what) {
            case MSG_PREVIEW_START:
                mThread.startPreview(msg.arg1, msg.arg2);
                break;
            case MSG_PREVIEW_SWITCH:
                mThread.switchCamera((Integer) msg.obj);
                break;
            case MSG_PREVIEW_STOP:
                mThread.stopPreview();
                synchronized (this) {
                    notifyAll();
                }
                Looper.myLooper().quit();
                mThread = null;
                break;
            default:
                throw new RuntimeException("unknown message:what=" + msg.what);
        }
    }
}