package com.nightfarmer.smartcamera.audiovideosample;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Created by zhangfan on 17-7-18.
 */

public class SnapView extends View {
    int maxDelay = 10 * 1000;//10秒


    private int measuredWidth;
    private int measuredHeight;
    RectF rectF = new RectF();

    public SnapView(Context context) {
        this(context, null);
    }

    public SnapView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SnapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    Paint paintBack;
    Paint paintCenter;
    Paint paintProgress;

    private void init(Context context) {
        paintBack = new Paint();
        paintBack.setColor(Color.parseColor("#DDDDDD"));
        paintBack.setAntiAlias(true);

        paintCenter = new Paint();
        paintCenter.setColor(Color.WHITE);
        paintCenter.setAntiAlias(true);

        paintProgress = new Paint();
        paintProgress.setColor(Color.parseColor("#68C067"));
        paintProgress.setAntiAlias(true);
        paintProgress.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int centerX = measuredWidth / 2;
        int centerY = measuredHeight / 2;
        int size = Math.min(measuredHeight, measuredWidth) / 2;
        int backSize = (int) (size * (0.65f + 0.35f * circleScale));
        int centerSize = (int) (size * (0.5f - 0.15f * circleScale));
        canvas.drawCircle(centerX, centerY, backSize, paintBack);
        canvas.drawCircle(centerX, centerY, centerSize, paintCenter);

        paintProgress.setStrokeWidth(getLineWidth());
        canvas.drawArc(rectF, -90, 360 * progress, false, paintProgress);

        super.onDraw(canvas);
    }

    float pressContinued = 0;
    ValueAnimator timer = null;

    float circleScale = 0;
    float progress = 0;

    ValueAnimator expandAnim;
    ValueAnimator collapseAnim;

    private void createExpandAnim() {
        if (collapseAnim != null) {
            collapseAnim.cancel();
        }
        expandAnim = ValueAnimator.ofFloat(circleScale, 1);
        expandAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                circleScale = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        expandAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                expandAnim = null;
            }
        });
        expandAnim.setDuration(100);
        expandAnim.setInterpolator(new LinearInterpolator());
        expandAnim.start();
    }

    private void createCollapseAnim() {
        if (circleScale == 0) return;
        if (expandAnim != null) {
            expandAnim.cancel();
        }
        collapseAnim = ValueAnimator.ofFloat(circleScale, 0);
        collapseAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                circleScale = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        collapseAnim.setDuration(100);
        collapseAnim.setInterpolator(new LinearInterpolator());
        collapseAnim.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //超过500毫秒识为录像 500
        //超过200毫秒开始圆形变形动画
        //超过500毫秒开始显示进度条
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                timer = ValueAnimator.ofFloat(0, maxDelay);
                timer.setDuration(maxDelay);
                timer.setInterpolator(new LinearInterpolator());
                timer.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        Float newPressContinued = (float) animation.getAnimatedValue();
                        if (newPressContinued > 300 && null == expandAnim) {
                            createExpandAnim();
                        }
                        if (pressContinued <= 500 && newPressContinued > 500 && pressListener != null) {
                            pressListener.onLongPressDown();
                        }
                        updateProgress(newPressContinued);
                    }
                });
                timer.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        timerEnd();
                    }
                });
                timer.start();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                timer.cancel();
                break;
        }
        return true;
    }

    private void updateProgress(float newPressContinued) {
        pressContinued = newPressContinued;
        progress = Math.max(0, (pressContinued - 500) / (maxDelay - 500));
    }

    private void timerEnd() {
        createCollapseAnim();
        if (pressContinued > 500) {
//            Toast.makeText(getContext(), "长摁结束", Toast.LENGTH_SHORT).show();
            Log.i("pressContinued", "长摁结束" + pressContinued);
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (pressListener != null) {
                        pressListener.onLongPressUp();
                    }
                }
            }, 100);
        } else {
//            Toast.makeText(getContext(), "轻触", Toast.LENGTH_SHORT).show();
            Log.i("pressContinued", "轻触" + pressContinued);
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (pressListener != null) {
                        pressListener.onClick();
                    }
                }
            }, 100);
        }
        updateProgress(0);
    }

    private float getLineWidth() {
        return Math.min(measuredHeight, measuredWidth) / 25;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measuredWidth = getMeasuredWidth();
        measuredHeight = getMeasuredHeight();
        float halfLineWith = getLineWidth() / 2;
        rectF.left = halfLineWith;
        rectF.top = halfLineWith;
        rectF.right = measuredWidth - halfLineWith;
        rectF.bottom = measuredHeight - halfLineWith;
    }

    PressListener pressListener;

    public void setPressListener(PressListener pressListener) {
        this.pressListener = pressListener;
    }

    interface PressListener {
        void onClick();

        void onLongPressDown();

        void onLongPressUp();
    }
}
