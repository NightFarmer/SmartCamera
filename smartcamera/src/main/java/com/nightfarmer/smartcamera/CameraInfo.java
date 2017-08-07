package com.nightfarmer.smartcamera;

import android.os.Environment;

import java.io.Serializable;

/**
 * Created by zhangfan on 17-8-7.
 */

public class CameraInfo implements Serializable {

    public CameraType type = CameraType.Picture;
    public int pictureQuality = 50;
    public String pictureOutputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
    public String VideoOutputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath();

    public static enum CameraType {
        Picture,
        Video,
        All;

        public boolean couldClick() {
            return this == Picture || this == All;
        }

        public boolean couldLongPress() {
            return this == Video || this == All;
        }
    }
}
