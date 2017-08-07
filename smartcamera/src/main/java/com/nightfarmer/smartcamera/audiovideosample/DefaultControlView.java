package com.nightfarmer.smartcamera.audiovideosample;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nightfarmer.smartcamera.CameraInfo;
import com.nightfarmer.smartcamera.R;

/**
 * Created by zhangfan on 17-7-18.
 */

public class DefaultControlView extends RelativeLayout {

    IOperator operator;
    ImageView btn_switch_camera;
    ImageView btn_back;
    TextView tv_label;
    SnapView btn_snap;
    View btn_cancel;
    View btn_ok;

    boolean takePictureFinish = true;
    boolean canceled = false;
    boolean ok = false;
    String filePath = null;
    private CameraInfo cameraInfo;

    public void attachToSmartCameraView(IOperator operator) {
        this.operator = operator;
    }

    public DefaultControlView(Context context) {
        this(context, null);
    }

    public DefaultControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DefaultControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void setCameraInfo(CameraInfo cameraInfo) {
        this.cameraInfo = cameraInfo;
        btn_snap.cameraInfo = cameraInfo;
        if (cameraInfo.type == CameraInfo.CameraType.All) {
            tv_label.setText("轻触拍照，长摁录像");
        } else if (cameraInfo.type == CameraInfo.CameraType.Picture) {
            tv_label.setText("轻触拍照");
        } else if (cameraInfo.type == CameraInfo.CameraType.Video) {
            tv_label.setText("长摁录像");
        }
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.default_control_view, this, false);
        view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(view);

        btn_snap = (SnapView) view.findViewById(R.id.btn_snap);
        btn_snap.setPressListener(new SnapView.PressListener() {
            @Override
            public void onClick() {
                if (!takePictureFinish) return;
                resetFiled();
                operator.takePicture(new CameraGLView.cameraFinishCallback() {
                    @Override
                    public void onFinish(String path) {
                        takePictureFinish = true;
                        filePath = path;
                        if (canceled) {
                            onCancel();
                            return;
                        }
                        if (ok) {
                            ok();
                        }
                    }
                });
                setState(STATE_RESULT);
                startResultAnim();
            }

            @Override
            public void onLongPressDown() {
                operator.startVideotape();
                setState(STATE_PRESS);
            }

            @Override
            public void onLongPressUp() {
                operator.stopVideotape(new CameraGLView.cameraFinishCallback() {
                    @Override
                    public void onFinish(String path) {
                        takePictureFinish = true;
                        filePath = path;
                        setState(STATE_RESULT);
                        startResultAnim();
                    }
                });
            }
        });

        btn_switch_camera = (ImageView) findViewById(R.id.btn_switch_camera);
        btn_switch_camera.setColorFilter(Color.parseColor("#F2F2F2"));
        btn_switch_camera.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                operator.switchCamera();
            }
        });
        tv_label = (TextView) findViewById(R.id.tv_label);
        tv_label.setTextColor(Color.parseColor("#F2F2F2"));
        btn_back = (ImageView) findViewById(R.id.btn_back);
        btn_back.setColorFilter(Color.parseColor("#F2F2F2"));
        btn_back.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = (Activity) getContext();
                activity.finish();
            }
        });
        btn_cancel = findViewById(R.id.btn_cancel);
        btn_cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                canceled = true;
                ok = false;
                if (takePictureFinish) {
                    onCancel();
                }
            }
        });
        btn_ok = findViewById(R.id.btn_ok);
        btn_ok.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                canceled = false;
                ok = true;
                if (!takePictureFinish) return;
                ok();
            }
        });
        setState(STATE_IDLE);
    }

    private void resetFiled() {
        takePictureFinish = false;
        canceled = false;
        ok = false;
        filePath = null;
    }

    private void ok() {
        Activity activity = (Activity) getContext();
        Intent intent = new Intent();
        intent.putExtra("path", filePath);
        activity.setResult(Activity.RESULT_OK, intent);
        activity.finish();
        activity.overridePendingTransition(R.anim.stay, R.anim.bottom_out);
    }

    private void onCancel() {
        btn_cancel.setClickable(false);
        btn_ok.setClickable(false);
        operator.reset();
        operator.onCancel();
//                operator.setReadyCallback(new SmartCameraView.ReadyCallback() {
//                    @Override
//                    public void onReady() {
        setState(STATE_IDLE);
        btn_cancel.setClickable(true);
        btn_ok.setClickable(true);
//                    }
//                });
    }


    public void startResultAnim() {
        ObjectAnimator animBtnCancel = ObjectAnimator.ofFloat(btn_cancel, "translationX", 0, -(width / 3.5f));
        animBtnCancel.setDuration(200);
        animBtnCancel.setInterpolator(new LinearInterpolator());
        animBtnCancel.start();

        ObjectAnimator animBtnOk = ObjectAnimator.ofFloat(btn_ok, "translationX", 0, width / 3.5f);
        animBtnOk.setDuration(200);
        animBtnOk.setInterpolator(new LinearInterpolator());
        animBtnOk.start();
    }


    public static final int STATE_IDLE = 0;
    public static final int STATE_PRESS = 1;
    public static final int STATE_RESULT = 2;

    void setState(int state) {
        switch (state) {
            case STATE_IDLE:
                btn_switch_camera.setVisibility(View.VISIBLE);
                btn_back.setVisibility(View.VISIBLE);
                btn_cancel.setVisibility(View.GONE);
                btn_ok.setVisibility(View.GONE);
                btn_snap.setVisibility(View.VISIBLE);
                tv_label.setVisibility(View.VISIBLE);
                break;
            case STATE_PRESS:
                btn_switch_camera.setVisibility(View.GONE);
                btn_back.setVisibility(View.GONE);
                btn_cancel.setVisibility(View.GONE);
                btn_ok.setVisibility(View.GONE);
                btn_snap.setVisibility(View.VISIBLE);
                tv_label.setVisibility(View.GONE);
                break;
            case STATE_RESULT:
                btn_switch_camera.setVisibility(View.GONE);
                btn_back.setVisibility(View.GONE);
                btn_cancel.setVisibility(View.VISIBLE);
                btn_ok.setVisibility(View.VISIBLE);
                btn_snap.setVisibility(View.GONE);
                tv_label.setVisibility(View.GONE);
                break;
        }
    }

    int width;
    int height;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = getMeasuredWidth();
        height = getMeasuredHeight();
    }
}
