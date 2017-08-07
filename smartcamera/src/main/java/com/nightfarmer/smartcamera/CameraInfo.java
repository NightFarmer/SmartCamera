package com.nightfarmer.smartcamera;

import java.io.Serializable;

/**
 * Created by zhangfan on 17-8-7.
 */

public class CameraInfo implements Serializable {

    public CameraType type = CameraType.Picture;
    public int pictureQuality = 50;

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
