package com.nightfarmer.smartcamera;

import android.app.Activity;
import android.content.Intent;

/**
 * Created by zhangfan on 17-8-7.
 */

public class SmartCamera {

    public static void startCamera(Activity activity, int requestCode) {
        startCamera(activity, requestCode, new CameraInfo());
    }

    public static void startCamera(Activity activity, int requestCode, CameraInfo cameraInfo) {
        Intent intent = new Intent(activity, PermissionCheckActivity.class);
        intent.putExtra("requestCode", requestCode);
        intent.putExtra("cameraInfo", cameraInfo);
        activity.startActivityForResult(intent, requestCode);
        activity.overridePendingTransition(R.anim.smart_camera_bottom_in, R.anim.smart_camera_stay);
    }

}
