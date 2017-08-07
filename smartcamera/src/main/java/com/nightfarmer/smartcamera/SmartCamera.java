package com.nightfarmer.smartcamera;

import android.app.Activity;
import android.content.Intent;

/**
 * Created by zhangfan on 17-8-7.
 */

public class SmartCamera {

    public static void startCameraActivity(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, PermissionCheckActivity.class);
        intent.putExtra("requestCode", requestCode);
        activity.startActivityForResult(intent, requestCode);
        activity.overridePendingTransition(R.anim.bottom_in, R.anim.stay);
    }

}
