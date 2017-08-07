package com.nightfarmer.smartcamera;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;

public class PermissionCheckActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.hide();//隐藏掉整个ActionBar，包括下面的Tabs
        }

        // 隐藏状态栏
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //让虚拟键盘一直不显示
//        hideBottomBar();

        setContentView(R.layout.activity_permission_check);

        boolean permission = checkPermission();
        if (!permission) {
            int requestCode = getIntent().getIntExtra("requestCode", 1);
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, requestCode);
        } else {
            startCameraActivity();
        }
    }


    private boolean checkPermission() {
        boolean permission;
        permission = ActivityCompat.checkSelfPermission(this, PERMISSIONS_STORAGE[0]) == PackageManager.PERMISSION_GRANTED;
        permission = permission && (ActivityCompat.checkSelfPermission(this, PERMISSIONS_STORAGE[1]) == PackageManager.PERMISSION_GRANTED);
        permission = permission && (ActivityCompat.checkSelfPermission(this, PERMISSIONS_STORAGE[2]) == PackageManager.PERMISSION_GRANTED);
        permission = permission && (ActivityCompat.checkSelfPermission(this, PERMISSIONS_STORAGE[3]) == PackageManager.PERMISSION_GRANTED);
        return permission;
    }

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    private final String[] perNameList = {
            "文件读取",
            "文件写入",
            "相机",
            "录音"
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if ()
        String perStr = "";
        for (int i = 0; i < permissions.length; i++) {
            Log.i("abc", permissions[i]);
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                perStr += "'" + perNameList[i] + "'" + ",";
            }
        }
        if (perStr.length() > 0) {
            perStr = perStr.substring(0, perStr.length() - 1);
        }
        if (!TextUtils.isEmpty(perStr)) {
            perStr = perStr + "权限获取失败";
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("提示");
            builder.setMessage(perStr);
            builder.setNegativeButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                    overridePendingTransition(R.anim.stay, R.anim.bottom_out);
                }
            });
            builder.setCancelable(false);
            builder.show();
        }

        boolean permission = checkPermission();
        Log.i("abc", "resume " + permission);
        if (permission) {
            startCameraActivity();
        }
    }

    private void startCameraActivity() {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("cameraInfo", getIntent().getSerializableExtra("cameraInfo"));
        startActivityForResult(intent, 1);
        overridePendingTransition(R.anim.bottom_in, R.anim.stay);
    }


    @Override
    protected void onResume() {
//        hideBottomBar();
        super.onResume();
    }

    private void hideBottomBar() {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT < 19) { // lower api
            View v = getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(resultCode, data);
        finish();
        overridePendingTransition(R.anim.stay, R.anim.bottom_out);
    }
}
