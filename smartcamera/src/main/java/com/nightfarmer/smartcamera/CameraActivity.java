package com.nightfarmer.smartcamera;

import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.nightfarmer.smartcamera.audiovideosample.CameraGLView;
import com.nightfarmer.smartcamera.audiovideosample.DefaultControlView;
import com.nightfarmer.smartcamera.audiovideosample.IOperator;
import com.nightfarmer.smartcamera.encoder.MediaAudioEncoder;
import com.nightfarmer.smartcamera.encoder.MediaEncoder;
import com.nightfarmer.smartcamera.encoder.MediaMuxerWrapper;
import com.nightfarmer.smartcamera.encoder.MediaVideoEncoder;

import java.io.IOException;

public class CameraActivity extends AppCompatActivity {

    private static final boolean DEBUG = false;    // TODO set false on release
    private static final String TAG = "CameraFragment";

    /**
     * for camera preview display
     */
    private CameraGLView mCameraView;

    /**
     * muxer for audio/video recording
     */
    private MediaMuxerWrapper mMuxer;
    //    private TextureView tv_luxiang;
    private MediaPlayer mMediaPlayer;
    private RelativeLayout container_preview;
    private ImageView iv_preview;

    private CameraInfo cameraInfo;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.hide();//隐藏掉整个ActionBar，包括下面的Tabs
        }

        // 隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //让虚拟键盘一直不显示
        hideBottomBar();

        setContentView(R.layout.activity_camera);

        cameraInfo = (CameraInfo) getIntent().getSerializableExtra("cameraInfo");

        mCameraView = (CameraGLView) findViewById(R.id.cameraView);
        mCameraView.cameraInfo = cameraInfo;
//        mCameraView.setVideoSize(1280, 720);

        iv_preview = (ImageView) findViewById(R.id.iv_preview);
        mCameraView.hehe = iv_preview;

        container_preview = (RelativeLayout) findViewById(R.id.container_preview);
        DefaultControlView defaultControlView = new DefaultControlView(this);
        defaultControlView.setCameraInfo(cameraInfo);
        defaultControlView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        RelativeLayout root_view = (RelativeLayout) findViewById(R.id.root_view);
        root_view.addView(defaultControlView);
        defaultControlView.attachToSmartCameraView(new IOperator() {
            @Override
            public void takePicture(CameraGLView.cameraFinishCallback callback) {
                mCameraView.takePicture(callback);
            }

            @Override
            public void startVideotape() {
                startRecording();
            }

            @Override
            public void stopVideotape(final CameraGLView.cameraFinishCallback callback) {
                stopRecording(new MediaMuxerWrapper.StopListener() {

                    @Override
                    public void onStop() {
                        final String outputPath = mMuxer.getOutputPath();
                        mMuxer = null;
                        mCameraView.post(new Runnable() {
                            @Override
                            public void run() {
                                play(outputPath);
                                callback.onFinish(outputPath);
                            }
                        });
                    }
                });
            }

            @Override
            public void switchCamera() {
                mCameraView.switchCamera();
            }

            @Override
            public void reset() {
                mCameraView.reset();
                iv_preview.setVisibility(View.GONE);
                container_preview.removeAllViews();
                if (mMediaPlayer != null) {
                    mMediaPlayer.reset();
                    mMediaPlayer.stop();
                    mMediaPlayer.release();
                    mMediaPlayer = null;
//                    tv_preview
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideBottomBar();
        mCameraView.onResume();
    }

    @Override
    protected void onPause() {
        stopRecording(null);
        mCameraView.onPause();
        super.onPause();
    }


    /**
     * start resorcing
     * This is a sample project and call this on UI thread to avoid being complicated
     * but basically this should be called on private thread because prepareing
     * of encoder is heavy work
     */
    private void startRecording() {
        if (DEBUG) Log.v(TAG, "startRecording:");
        try {
            mMuxer = new MediaMuxerWrapper(".mp4",cameraInfo);    // if you record audio only, ".m4a" is also OK.
            if (true) {
                // for video capturing
                new MediaVideoEncoder(mMuxer, mMediaEncoderListener, mCameraView.getVideoWidth(), mCameraView.getVideoHeight());
            }
            if (true) {
                // for audio capturing
                new MediaAudioEncoder(mMuxer, mMediaEncoderListener);
            }
            mMuxer.prepare();
            mMuxer.startRecording();
        } catch (final IOException e) {
            Log.e(TAG, "startCapture:", e);
        }
    }

    /**
     * request stop recording
     */
    private void stopRecording(MediaMuxerWrapper.StopListener stopListener) {
        if (DEBUG) Log.v(TAG, "stopRecording:mMuxer=" + mMuxer);
        if (mMuxer != null) {
            mMuxer.stopRecording(stopListener);
        }

    }

    private void play(String outputPath) {
        mMediaPlayer = new MediaPlayer();
        try {
//            final File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "AVRecSample");
//            File file = new File(dir, "demo" + ".mp4");
//            mMediaPlayer.setDataSource(file.getPath());
            mMediaPlayer.setDataSource(outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        TextureView tv_preview = new TextureView(container_preview.getContext());
        tv_preview.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        container_preview.addView(tv_preview);
        tv_preview.setVisibility(View.VISIBLE);
        tv_preview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mMediaPlayer.setSurface(new Surface(surface));
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mMediaPlayer.start();
                    }
                });
                mMediaPlayer.setLooping(true);
                mMediaPlayer.prepareAsync();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

    }

    /**
     * callback methods from encoder
     */
    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onPrepared:encoder=" + encoder);
            if (encoder instanceof MediaVideoEncoder)
                mCameraView.setVideoEncoder((MediaVideoEncoder) encoder);
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onStopped:encoder=" + encoder);
            if (encoder instanceof MediaVideoEncoder)
                mCameraView.setVideoEncoder(null);
        }
    };


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

}
