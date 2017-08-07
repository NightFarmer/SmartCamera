package com.nightfarmer.smartcamera.sample;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.nightfarmer.smartcamera.CameraInfo;
import com.nightfarmer.smartcamera.SmartCamera;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    TextView result_label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        result_label = (TextView) findViewById(R.id.result_label);
    }

    public void open(View view) {
        CameraInfo cameraInfo = new CameraInfo();
        cameraInfo.type = CameraInfo.CameraType.Video;
        SmartCamera.startCamera(this, 1, cameraInfo);
    }

    private static final String DIR_NAME = "AVRecSample";

    String resultFilePath = "";

    public void show(View view) {
//        final File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), DIR_NAME);
//        File file = new File(dir, "demo" + ".mp4");
        if (TextUtils.isEmpty(resultFilePath)) {
            Toast.makeText(this, "没结果", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//设置intent的Action属性
        intent.setAction(Intent.ACTION_VIEW);
//获取文件file的MIME类型
        File file = new File(resultFilePath);
        String type;
        if (file.getName().endsWith(".mp4")) {
            type = "video/mp4";
        } else {
            type = "image/jpeg";
        }
//设置intent的data和Type属性。
//        Uri uri = Uri.fromFile(file);
//        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

//        ContentValues contentValues = new ContentValues(1);
//        contentValues.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
//        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileProvider", file);
            intent.setDataAndType(uri, type);
        } else {
            intent.setDataAndType(Uri.fromFile(file), type);
        }

        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            resultFilePath = data.getStringExtra("path");
            File file = new File(resultFilePath);
            String info = "文件不存在";
            if (file.exists()) {
                long length = file.length();
                info = resultFilePath + "\n";
                double size = length / (1024.0 * 1024);
                info += size + "\n";
//                resultFilePath = info;
                result_label.setText(info);
            }
        } else {
            resultFilePath = "";
            result_label.setText("");
        }
    }

}
