package com.chinanetcenter.streampusherdemo.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.chinanetcenter.StreamPusher.sdk.SPConfig;
import com.chinanetcenter.StreamPusher.sdk.SPManager;
import com.chinanetcenter.StreamPusher.sdk.SPManager.AudioSourceMode;
import com.chinanetcenter.StreamPusher.sdk.SPManager.VideoRatio;
import com.chinanetcenter.StreamPusher.sdk.SPManager.VideoResolution;
import com.chinanetcenter.streampusherdemo.R;
import com.chinanetcenter.streampusherdemo.utils.DialogUtils;
import com.chinanetcenter.streampusherdemo.utils.PreferenceUtil;
import com.chinanetcenter.streampusherdemo.video.CustomYuvSource;

@SuppressWarnings("deprecation")
public class ConfigActivity extends BaseActivity implements OnClickListener {
    private static final String TAG = "ConfigActivity";

    private EditText mUrlEt;
    private Button mConfigBtn;
    private EditText mFrameRate;
    private Spinner mResSp;
    private Spinner mBitrateSp;
    private RadioGroup mCameraRg;
    private RadioGroup mAudioRg;
    private RadioGroup mOrientationGroup;
    private RadioGroup mVideoRatioGroup;
    private RadioGroup mPushEncoderRg;
    private RadioGroup mAudioPlayRg;
    private CheckBox mhasVideoCb;
    private CheckBox mhasAudioCb;
    private CheckBox mPlayerTestCb;
    private RadioGroup mCustomVideoSourceRg;
    private RadioGroup mSoftEncodeStragetyRg;
    private Switch mYuvPreHandlerSourceSw;
    private Switch mEchoCancellationSw;

    private String mUrl = null;

    private int mCameraState;
    private int mPushEncoderState;
    private int mPullDecoderState;
    private int mAudioPlayMethod;
    private int mBitrate;
    private int mScreenOrientation = Configuration.ORIENTATION_PORTRAIT;
    private VideoRatio mVideoRatio = VideoRatio.RATIO_16_9;
    private String mAppId = null;
    private String mAuthKey = null;
    private boolean mDemoDebug = false;

    private Integer[] mBitrates = {500 * 1024, 800 * 1024, 1000 * 1024, 1200 * 1024, 1500 * 1024};
    private Integer[] mSamples = {44100, 16000, 8000};

    private VideoResolution mSelectedResolution = VideoResolution.VIDEO_RESOLUTION_360P;
    private AudioSourceMode mAudioSourceMode = AudioSourceMode.AUDIORECORD_MODE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String appId = preferences.getString("appId", "");
        String authKey = preferences.getString("authKey", "");
        if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(authKey)) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        } else {
            preferences.edit().putString("appId", mAppId).putString("authKey", mAuthKey).apply();
            mAppId = appId;
            mAuthKey = authKey;
        }

        setContentView(R.layout.activity_config);

        mDemoDebug = getResources().getBoolean(R.bool.demo_debug);
        mCameraState = CameraInfo.CAMERA_FACING_FRONT;
        mPushEncoderState = SPConfig.ENCODER_MODE_HARD;
        mAudioPlayMethod = SPConfig.AUDIO_PLAY_AUDIOTRACK;
        mBitrate = mBitrates[0];
        if (mDemoDebug) {
            mBitrates = new Integer[]{500 * 1024, 800 * 1024, 1000 * 1024, 1200 * 1024, 1500 * 1024};
        }

        mUrl = getResources().getString(R.string.rtmp_url);
        initLayout();
        checkAndRequestPermission();
    }

    @Override
    protected void onResume() {
        refreshConfigPage();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void refreshConfigPage() {
        SPConfig config = SPManager.getConfig();
        // url
        // if(!TextUtils.isEmpty(config.getRtmpUrl())){
        // mPushUrlEt.setText(config.getRtmpUrl());
        // }
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mUrlEt.setText(preferences.getString("rtmp_url", mUrl));

        // camera
        mCameraState = config.getCameraId();
        if (mCameraState == CameraInfo.CAMERA_FACING_FRONT) {
            mCameraRg.check(mCameraRg.getChildAt(0).getId());
        } else {
            mCameraRg.check(mCameraRg.getChildAt(1).getId());
        }
        // audio
        mAudioSourceMode = config.getAudioSourceMode();
        if (mAudioSourceMode == AudioSourceMode.AUDIORECORD_MODE) {
            mAudioRg.check(mAudioRg.getChildAt(0).getId());
        } else {
            mAudioRg.check(mAudioRg.getChildAt(1).getId());
        }
        // encode mode
        mPushEncoderState = config.getEncoderMode();
        switch (mPushEncoderState) {
            case SPConfig.ENCODER_MODE_HARD:
                mPushEncoderRg.check(mPushEncoderRg.getChildAt(1).getId());
                break;
            case SPConfig.ENCODER_MODE_SOFT:
                mPushEncoderRg.check(mPushEncoderRg.getChildAt(0).getId());
                break;
            default:
                break;
        }

        // resolution
        mSelectedResolution = config.getVideoResolution();
        if (mSelectedResolution == VideoResolution.VIDEO_RESOLUTION_CUSTOM) {
            mSelectedResolution = VideoResolution.VIDEO_RESOLUTION_360P;
        }
        mResSp.setSelection(mSelectedResolution.ordinal());

    }

    private void initLayout() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mUrlEt = (EditText) findViewById(R.id.UrlEt);
        mUrlEt.setText(preferences.getString("rtmp_url", mUrl));
        mUrlEt.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        mConfigBtn = (Button) findViewById(R.id.ConfigBtn);
        mConfigBtn.setOnClickListener(this);

        //
        mCameraRg = (RadioGroup) findViewById(R.id.cameraRg);
        mCameraRg.check(mCameraRg.getChildAt(0).getId());
        mCameraRg.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == group.getChildAt(0).getId() && mCameraState != CameraInfo.CAMERA_FACING_FRONT) {
                    mCameraState = CameraInfo.CAMERA_FACING_FRONT;

                } else if (checkedId == group.getChildAt(1).getId() && mCameraState != CameraInfo.CAMERA_FACING_BACK) {
                    mCameraState = CameraInfo.CAMERA_FACING_BACK;
                }
            }
        });

        mAudioRg = (RadioGroup) findViewById(R.id.audioRg);
        mAudioRg.check(mAudioRg.getChildAt(0).getId());
        mAudioRg.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == group.getChildAt(0).getId() && mAudioSourceMode != AudioSourceMode.AUDIORECORD_MODE) {
                    mAudioSourceMode = AudioSourceMode.AUDIORECORD_MODE;

                } else if (checkedId == group.getChildAt(1).getId() && mAudioSourceMode != AudioSourceMode.OPENSLES_MODE) {
                    mAudioSourceMode = AudioSourceMode.OPENSLES_MODE;
                }
            }
        });

        mOrientationGroup = (RadioGroup) findViewById(R.id.orientation_group);
        mOrientationGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.orientation_portrait_btn:
                        mScreenOrientation = Configuration.ORIENTATION_PORTRAIT;
                        break;
                    case R.id.orientation_landscape_btn:
                        mScreenOrientation = Configuration.ORIENTATION_LANDSCAPE;
                        break;
                    case R.id.orientation_auto_change_btn:
                        mScreenOrientation = Configuration.ORIENTATION_UNDEFINED;

                    default:
                        break;
                }

            }
        });

        mVideoRatioGroup = (RadioGroup) findViewById(R.id.video_ratio_group);
        VideoRatio videoRatio = getScreenRatio();
        if (videoRatio == VideoRatio.RATIO_16_9) {
            mVideoRatioGroup.check(R.id.ratio_16_9);
            mVideoRatio = VideoRatio.RATIO_16_9;
        } else {
            mVideoRatioGroup.check(R.id.ratio_4_3);
            mVideoRatio = VideoRatio.RATIO_4_3;
        }
        mVideoRatioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.ratio_4_3:
                        mVideoRatio = VideoRatio.RATIO_4_3;
                        break;
                    case R.id.ratio_16_9:
                    default:
                        mVideoRatio = VideoRatio.RATIO_16_9;
                        break;
                }

            }
        });
        mPushEncoderRg = (RadioGroup) findViewById(R.id.encodeRg);
        mPushEncoderRg.check(mPushEncoderRg.getChildAt(1).getId());
        mPushEncoderRg.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == group.getChildAt(0).getId()) {
                    mPushEncoderState = SPConfig.ENCODER_MODE_SOFT;
                } else if (checkedId == group.getChildAt(1).getId()) {
                    mPushEncoderState = SPConfig.ENCODER_MODE_HARD;
                }
            }
        });

        mFrameRate = (EditText) findViewById(R.id.frame_rate_et);

        //
        mResSp = (Spinner) findViewById(R.id.resSp);
        mResSp.setAdapter(new ArrayAdapter<VideoResolution>(this, android.R.layout.simple_spinner_dropdown_item, SPManager.VIDEO_RESOLUTION_ALL));

        int pos = SPManager.VIDEO_RESOLUTION_ALL.length / 2;
        mSelectedResolution = SPManager.VIDEO_RESOLUTION_ALL[pos];

        mResSp.setSelection(pos);
        mResSp.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                mSelectedResolution = SPManager.VIDEO_RESOLUTION_ALL[pos];
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        //
        mBitrateSp = (Spinner) findViewById(R.id.bitSp);
        mBitrateSp.setAdapter(new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_dropdown_item, mBitrates));
        mBitrateSp.setSelection(0);
        mBitrateSp.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                mBitrate = mBitrates[pos];
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

        mhasVideoCb = (CheckBox) findViewById(R.id.has_video);
        mhasAudioCb = (CheckBox) findViewById(R.id.has_audio);
        mCustomVideoSourceRg = (RadioGroup) findViewById(R.id.custom_video_source_rg);
        mCustomVideoSourceRg.check(mCustomVideoSourceRg.getChildAt(0).getId());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //SPManager.pushTextureFrame方法只支持EGL14上下文环境，并且4.3及以上版本
            mCustomVideoSourceRg.getChildAt(2).setVisibility(View.GONE);
        }
        mCustomVideoSourceRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == group.getChildAt(1).getId()) {//yuv方式需要使用摄像头采集，需要先初始化摄像头预览参数
                    final Context context = ConfigActivity.this;
                    String widthString = PreferenceUtil.getString(context, PreferenceUtil.KEY_PREVIEW_WIDTH);
                    String heightString = PreferenceUtil.getString(context, PreferenceUtil.KEY_PREVIEW_HEIGHT);
                    int widthInt = widthString == null ? 0 : Integer.parseInt(widthString);
                    int heightInt = heightString == null ? 0 : Integer.parseInt(heightString);
                    if (widthInt > 0 && heightInt > 0) return;
                    //取摄像头支持的预览分辨率
                    AsyncTask<Void, Void, Camera.Size> voidSizeAsyncTask = new AsyncTask<Void, Void, Camera.Size>() {

                        Dialog progressDialog = null;

                        protected void onPreExecute() {
                            progressDialog = DialogUtils.showSimpleProgressDialog(context, "正在初始化。。。", false, null);
                        }

                        ;

                        @Override
                        protected Camera.Size doInBackground(Void... params) {
                            try {
                                Camera camera = Camera.open(CustomYuvSource.getCameraId());
                                Camera.Size size = CustomYuvSource.getPreviewSize(context, camera);
                                PreferenceUtil.persistString(context, PreferenceUtil.KEY_PREVIEW_WIDTH, size.width + "");
                                PreferenceUtil.persistString(context, PreferenceUtil.KEY_PREVIEW_HEIGHT, size.height + "");
                                camera.release();
                                return size;
                            } catch (Exception e) {
                                return null;
                            }
                        }

                        protected void onPostExecute(Camera.Size result) {
                            progressDialog.dismiss();
                            if (result == null) {
                                mCustomVideoSourceRg.check(mCustomVideoSourceRg.getChildAt(0).getId());
                                Toast.makeText(ConfigActivity.this, "自定义视频源参数初始化失败", Toast.LENGTH_SHORT).show();
                            } else {
                                VideoResolution.VIDEO_RESOLUTION_CUSTOM.setWidth(result.width);
                                VideoResolution.VIDEO_RESOLUTION_CUSTOM.setHeight(result.height);
                            }
                        }

                        ;
                    };
                    voidSizeAsyncTask.execute();
                }
            }
        });

        mSoftEncodeStragetyRg = (RadioGroup) findViewById(R.id.soft_encode_stragety_rg);
        mSoftEncodeStragetyRg.check(mSoftEncodeStragetyRg.getChildAt(0).getId());

        mEchoCancellationSw = ((Switch) findViewById(R.id.echo_cancellatione_sw));
        mEchoCancellationSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // open

                } else {
                    // close
                }

            }

        });

        mYuvPreHandlerSourceSw = ((Switch) findViewById(R.id.yuv_pre_handler_sw));
        mYuvPreHandlerSourceSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // open

                } else {
                    // close
                }

            }

        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ConfigBtn:
                // if (!checkAndRequestPermission())
                // return;
                String url = mUrlEt.getText().toString().trim();
                if (!TextUtils.isEmpty(url)) {
                    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                    Editor editor = preferences.edit();
                    editor.putString("rtmp_url", url);
                    editor.commit();
                }

                String frameRateStr = mFrameRate.getText().toString();
                int frameRate = 15;
                if (!TextUtils.isEmpty(frameRateStr)) {
                    frameRate = Integer.parseInt(frameRateStr);
                }

                Intent intent;
                intent = new Intent(this, MainActivity.class);
                intent.putExtra("rtmp", url);
                intent.putExtra("camera", mCameraState);
                intent.putExtra("audio_source_mode", mAudioSourceMode);
                intent.putExtra("screenOrientation", mScreenOrientation);
                intent.putExtra("encoder", mPushEncoderState);
                intent.putExtra("frame_rate", frameRate);
                intent.putExtra("bitrate", mBitrate);
                intent.putExtra("video_resolution", mSelectedResolution);
                intent.putExtra("video_ratio", mVideoRatio);
                intent.putExtra("appId", mAppId);
                intent.putExtra("authKey", mAuthKey);
                intent.putExtra("has_video", mhasVideoCb.isChecked());
                intent.putExtra("has_audio", mhasAudioCb.isChecked());
                intent.putExtra("custom_video_source", mCustomVideoSourceRg.indexOfChild(mCustomVideoSourceRg.findViewById(mCustomVideoSourceRg.getCheckedRadioButtonId())));
                intent.putExtra("echo_cancellatione", mEchoCancellationSw.isChecked());
                intent.putExtra("yuv_pre_handler", mYuvPreHandlerSourceSw.isChecked());
                intent.putExtra("soft_encode_stragety", mSoftEncodeStragetyRg.getCheckedRadioButtonId() == R.id.soft_encode_stragety_quality ? SPConfig.SOFT_ENCODE_STRATEGY_MORE_QUALITY : SPConfig.SOFT_ENCODE_STRATEGY_MORE_BITRATE);
                startActivity(intent);

                // finish();
                break;

            default:
                break;
        }

    }

    private VideoRatio getScreenRatio() {
        VideoRatio screenRatio = VideoRatio.RATIO_16_9;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float ratioFloat = -1;
        if (dm.heightPixels > dm.widthPixels) {
            ratioFloat = ((float) dm.heightPixels) / dm.widthPixels;
        } else {
            ratioFloat = ((float) dm.widthPixels) / dm.heightPixels;
        }
        if (Math.abs(ratioFloat - 1.778) < Math.abs(ratioFloat - 1.333)) {
            screenRatio = VideoRatio.RATIO_16_9;
        } else {
            screenRatio = VideoRatio.RATIO_4_3;
        }
        return screenRatio;
    }

}
