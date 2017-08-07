package com.nightfarmer.smartcamera.audiovideosample;

/**
 * Created by zhangfan on 17-8-4.
 */

public interface IOperator {
    void takePicture(CameraGLView.cameraFinishCallback callback);

    void startVideotape();

    void stopVideotape(CameraGLView.cameraFinishCallback callback);

    void switchCamera();

    void reset();

    void onCancel();
}
