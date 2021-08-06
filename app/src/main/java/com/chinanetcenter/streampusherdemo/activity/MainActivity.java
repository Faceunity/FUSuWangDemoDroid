package com.chinanetcenter.streampusherdemo.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chinanetcenter.StreamPusher.sdk.OnErrorListener;
import com.chinanetcenter.StreamPusher.sdk.SPAudioPlayer;
import com.chinanetcenter.StreamPusher.sdk.SPConfig;
import com.chinanetcenter.StreamPusher.sdk.SPManager;
import com.chinanetcenter.StreamPusher.sdk.SPManager.AudioSourceMode;
import com.chinanetcenter.StreamPusher.sdk.SPManager.OnStateListener;
import com.chinanetcenter.StreamPusher.sdk.SPManager.OutputFormat;
import com.chinanetcenter.StreamPusher.sdk.SPManager.PreProcessHandler;
import com.chinanetcenter.StreamPusher.sdk.SPManager.PushState;
import com.chinanetcenter.StreamPusher.sdk.SPManager.VideoRatio;
import com.chinanetcenter.StreamPusher.sdk.SPManager.VideoResolution;
import com.chinanetcenter.StreamPusher.sdk.SPManager.VideoType;
import com.chinanetcenter.StreamPusher.sdk.SPStickerController;
import com.chinanetcenter.StreamPusher.sdk.SPSurfaceView;
import com.chinanetcenter.StreamPusher.utils.ALog;
import com.chinanetcenter.streampusherdemo.MyApp;
import com.chinanetcenter.streampusherdemo.R;
import com.chinanetcenter.streampusherdemo.adapter.SettingsAdapter;
import com.chinanetcenter.streampusherdemo.custom.render.CameraUtils;
import com.chinanetcenter.streampusherdemo.filter.FaceUnityFilter;
import com.chinanetcenter.streampusherdemo.object.Question;
import com.chinanetcenter.streampusherdemo.object.QuestionGroup;
import com.chinanetcenter.streampusherdemo.object.QuestionUtils;
import com.chinanetcenter.streampusherdemo.object.SettingItem;
import com.chinanetcenter.streampusherdemo.service.FloatWindowService;
import com.chinanetcenter.streampusherdemo.sticker.GifStickerController;
import com.chinanetcenter.streampusherdemo.sticker.MoveStickerController;
import com.chinanetcenter.streampusherdemo.sticker.TimeStickerController;
import com.chinanetcenter.streampusherdemo.utils.DialogUtils;
import com.chinanetcenter.streampusherdemo.utils.DialogUtils.InputConfigClickListener;
import com.chinanetcenter.streampusherdemo.utils.PreferenceUtil;
import com.chinanetcenter.streampusherdemo.utils.ScreenContentRecorder;
import com.chinanetcenter.streampusherdemo.utils.SettingsPanelViewUtil;
import com.chinanetcenter.streampusherdemo.video.CustomTextureSource;
import com.chinanetcenter.streampusherdemo.video.CustomYuvSource;
import com.chinanetcenter.streampusherdemo.view.MusicPickDialog;
import com.faceunity.core.enumeration.CameraFacingEnum;
import com.faceunity.core.enumeration.FUAIProcessorEnum;
import com.faceunity.core.enumeration.FUInputTextureEnum;
import com.faceunity.core.enumeration.FUTransformMatrixEnum;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.data.FaceUnityDataFactory;
import com.faceunity.nama.listener.FURendererListener;
import com.faceunity.nama.ui.FaceUnityView;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity implements OnClickListener, OnItemClickListener, SensorEventListener {
    private static final String TAG = "MainActivity";

    private SPSurfaceView mPreviewView;
    private ViewGroup mPreviewViewGroup;
    private ViewGroup.LayoutParams mPreviewLayoutParams;
    private ServiceConnection mFloatWindowServiceConnection;
    private FloatWindowService mFloatWindowService;
    private TextView mRtmpUrlTv;
    private TextView mTvFps;
    private TextView mInfoTv;
    private SPConfig mSPConfig = null;
    private ImageButton btn_record;
    private ImageButton btn_recordToFile;
    private ImageButton mFlashImageBtn = null;
    private ImageButton mMuteImageBtn = null;
    private SettingsPanelViewUtil mSettingPanelUtil;
    private Handler mErrorHandler;
    private Handler mStateHandler;

    private int mVideoHeight = 0;
    private int mVideoWidth = 0;
    private int mPreviewHeight = 0;
    private int mPreviewWidth = 0;
    private int mPushSpeed = 0;
    private int mVideoBitrate = 0;
    private int mFrameRate = 0;
    private int mFrameEncodeRate = 0;
    private String mPushUrl = null;
    private static int mCurActivityHashCode = 0;
    private int mScreenOrientation;

    private boolean mIsUserRecording = false;
    private SPManager.PushStreamType mPushStreamType = SPManager.PushStreamType.TYPE_CAMERA;

    private OutputFormat mOutputFormat = OutputFormat.MUXER_OUTPUT_FLV;
    private VideoType mVideoType = VideoType.TYPE_SHORT_VIDEO;
    public long mMaxRecordFileSize = 5 * 1024 * 1024; // byte
    public long mVideoMaxRecordDuration = 10 * 1000; // ms
    public long mGIFMaxRecordDuration = 1 * 1000; // ms

    private boolean[] mWaterMarkSelected = new boolean[]{false/*timeMark*/, false/*logoMark*/,false/*timeSticker*/,false/*logoSticker*/,false/*moveSticker*/, false/*gifSticker*/};
    private SPStickerController mStickerControllers[] = new SPStickerController[6];

    private MusicPickDialog mBgmPickDialog = null;
    private SPAudioPlayer mBgmPlayer = null;
    private boolean mIsPause = false;
    private List<String> mBgmFiles = new LinkedList<String>();
    private int mCurrentBgmIndex = -1;
    private float mCurrentBgmVolume = 0.3f;
    private float mCurrentMicVolume = 1.0f;

    private VideoRatio mVideoRatio;
    //外部滤镜
    private FURenderer mFURenderer;
    private SensorManager mSensorManager;
    private FaceUnityFilter filter;
    private FaceUnityDataFactory mFaceUnityDataFactory;

    private List<SettingItem> mSettingItems = new ArrayList<SettingItem>() {
        {
            add(new SettingItem(R.id.setting_beauty, "美颜滤镜", 0, true));
            add(new SettingItem(R.id.setting_set_rtmpurl, "更新url", 0, true));
            add(new SettingItem(R.id.setting_set_camera_focus, "聚焦模式", 0, true));
            add(new SettingItem(R.id.setting_set_audio_loop, "耳返", 0, true));
            add(new SettingItem(R.id.setting_set_audio_reverb, "混响", 0, true));
            add(new SettingItem(R.id.setting_set_bitrate, "码率调节", 0, true));
            add(new SettingItem(R.id.setting_set_fps, "帧率调节", 0, true));
            add(new SettingItem(R.id.setting_set_variable_framerate, "可变帧率", 0, true));
            add(new SettingItem(R.id.setting_set_auto_bitrate, "自动码率", 0, true));
            add(new SettingItem(R.id.setting_set_flip, "图像倒转", 0, false));
            add(new SettingItem(R.id.setting_set_record_format, "录制", 0, true));
            add(new SettingItem(R.id.setting_set_watermark, "水印", 0, true));
            add(new SettingItem(R.id.setting_set_bgm, "背景音乐", 0, true));
            add(new SettingItem(R.id.setting_set_volume, "音量调节", 0, true));
            add(new SettingItem(R.id.setting_take_screenshot, "屏幕截图", 0, true));
            add(new SettingItem(R.id.setting_set_socks5_proxy, "代理", 0, true));
            add(new SettingItem(R.id.setting_set_mirror, "镜像", 0, true));
            add(new SettingItem(R.id.setting_add_question, "答题", 0, true));
        }
    };
    private int mCurrentCameraId = CameraInfo.CAMERA_FACING_FRONT;
    private boolean mIsUserPushing = false;
    private boolean mUseYuvPreHandler = false;
    private int mOpenedCameraId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestFullScreen();
        initHandle();
        initLayout();
        // init
        initFromIntent();
        initLayoutState();
        bindFloatWindowService();
        checkAndRequestPermission();

        Log.i(TAG, "onCreate -- ");

        mCurActivityHashCode = this.hashCode();
        SPManager.init(this, mSPConfig);
        initFilter();
        // yuv 预处理
        if (mUseYuvPreHandler) {
            Log.e(TAG,"yuvPreHandleryuvPreHandleryuvPreHandler");
            SPManager.setPreProcessHandler(new PreProcessHandler() {

                @Override
                public void handleYuvData(ByteBuffer y, ByteBuffer u, ByteBuffer v, int yStride, int uStride, int vStride, int width, int height) {
                    //在这里做YUV的预处理操作
                }
            });
        }
        PushState state = SPManager.getPushState();
        mVideoHeight = state.videoHeight;
        mVideoWidth = state.videoWidth;
        //将DialogUtils中记录的标志位置为默认

    }

    private void initHandle() {
        mErrorHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg != null && msg.obj != null) {
                    String content = "(" + msg.what + ") " + msg.obj.toString();
                    Toast.makeText(MainActivity.this, content, Toast.LENGTH_SHORT).show();
                }
                switch (msg.what) {
                    case SPManager.ERROR_PUSH_INIT_FAILED:
                        resetPushState();
                        if (mRtmpUrlTv == null || !mRtmpUrlTv.getText().toString().startsWith("rtmp://")) {
                            DialogUtils.showRtmpUrlInputDialog(MainActivity.this, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == DialogInterface.BUTTON_POSITIVE) {
                                        EditText et = (EditText) ((AlertDialog) dialog).findViewById(android.R.id.edit);
                                        if (et != null) {
                                            if (SPManager.setConfig(SPManager.getConfig().setRtmpUrl(et.getText().toString()))) {
                                                mRtmpUrlTv.setText(et.getText());
                                            }
                                        }
                                    }
                                }
                            });
                        }

                        break;
                    case SPManager.ERROR_CAMERA_SWITCH_PENDING:
                        SPManager.PushState state = SPManager.getPushState();
                        mFlashImageBtn.setSelected(state.isFlashing);
                        setButtonEnabled(mFlashImageBtn, state.isSupportFlash);
                        break;
                    case SPManager.ERROR_AUTH_FAILED:
                    case SPManager.ERROR_AUTHORIZING:
                    case SPManager.ERROR_PUSH_DISCONN:
                        resetPushState();
                        break;
                    case SPManager.ERROR_PARAM:
                        if (msg.obj.toString().equals("encoderMode =0 error mode!")) {
                            showToast("不支持硬编，自动切换到软编！");
                            mSPConfig.setEncoderMode(SPConfig.ENCODER_MODE_SOFT);
                            mSPConfig.setDecoderMode(SPConfig.DECODER_MODE_SOFT);
                            SPManager.init(MainActivity.this, mSPConfig);
                        }
                    default:
                        break;
                }
            }
        };
        mStateHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg != null && msg.obj != null) {
                    switch (msg.what) {
                        case SPManager.STATE_PUSH_SUCCESS:
                            showToast("Pushstream succeed");
                            break;
                        case SPManager.STATE_PUSH_SPEED:
                            mPushSpeed = Integer.parseInt((String) msg.obj);
                            showPushInfo();
                            break;
                        case SPManager.STATE_VIDEO_BITRATE:
                            mVideoBitrate = Integer.parseInt((String) msg.obj);
                            showPushInfo();
                            break;
                        case SPManager.STATE_FRAME_RATE:
                            mFrameRate = Integer.parseInt((String) msg.obj);
                            showPushInfo();
                            break;
                        case SPManager.STATE_ENCODE_FRAME_RATE:
                            mFrameEncodeRate = Integer.parseInt((String) msg.obj);
                            showPushInfo();
                            break;
                        case SPManager.STATE_CAMERA_OPEN_SUCCESS:
                            SPManager.PushState state = SPManager.getPushState();
                            mFlashImageBtn.setSelected(state.isFlashing);
                            setButtonEnabled(mFlashImageBtn, state.isSupportFlash);
                            break;
                        case SPManager.STATE_RECORD_RECORD_SUCCEED:
                        case SPManager.STATE_RECORD_RECORD_FAILED:
                        case SPManager.STATE_RECORD_NO_ENOUGH_SPACE:
                        case SPManager.STATE_RECORD_PERIOD_NOT_ENOUGH:
                            String content = "(" + msg.what + ") " + msg.obj.toString();
                            Log.e(TAG, content);
                            showToast(content);
                            if (mIsUserRecording) {
                                mIsUserRecording = false;
                                btn_recordToFile.setSelected(mIsUserRecording);
                            }
                            break;
                        case SPManager.STATE_RECORD_EARLY_TERMINATION_RISK:
                        case SPManager.STATE_RECORD_PERIOD_EXCEEDS_LIMIT:
                        case SPManager.STATE_RECORD_RECORD_COMPLETE:

                            content = "(" + msg.what + ") " + msg.obj.toString();
                            showToast(content);
                            break;
                        case SPManager.STATE_VIDEO_RESOLUTION_CHANGED:
                            try {
                                ALog.i(TAG, (String)msg.obj);
                                JSONObject jsonObject = new JSONObject((String)msg.obj);
                                mVideoWidth = jsonObject.optInt("w");
                                mVideoHeight = jsonObject.optInt("h");
                                updateWaterMarkState(mVideoHeight > mVideoWidth ? Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        };

        SPManager.setOnErrorListener(new OnErrorListener() {
            @Override
            public void onError(int what, String extra) {
                mErrorHandler.obtainMessage(what, extra).sendToTarget();
            }
        });
        SPManager.setOnStateListener(new OnStateListener() {
            @Override
            public void onState(int what, String extra) {
                switch (what) {
                    case SPManager.STATE_CAMERA_OPEN_SUCCESS:
                        mOpenedCameraId = Integer.parseInt(extra);
                        boolean mirrorParams[] = new boolean[2];
                        getMirrorParam(mirrorParams);
                        //设置预览和推流是否镜像,该消息在摄像头线程回调，为了保证前后置摄像头切换时不会出现mirror切换延时，尽量在该回调中调用setMirror
                        SPManager.setMirror(mirrorParams[0]/*previewMirror*/, mirrorParams[1]/*encodeMirror*/);
                        mStateHandler.obtainMessage(what, extra).sendToTarget();
                        break;
                    default:
                        mStateHandler.obtainMessage(what, extra).sendToTarget();
                        break;
                }
            }
        });
    }

    private void initFromIntent() {
        Bundle bundle = getIntent().getExtras();

        mPushUrl = bundle.getString("rtmp", "");
        mCurrentCameraId = bundle.getInt("camera", -1);
        AudioSourceMode audioSourceMode = (AudioSourceMode) bundle.getSerializable("audio_source_mode");
        int encoderState = bundle.getInt("encoder", -1);
        int decoderState = bundle.getInt("decoder", -1);
        int audioPlay = bundle.getInt("audio_play", -1);
        int frameRate = bundle.getInt("frame_rate");
        int bitrate = bundle.getInt("bitrate");
        int softEncodeStragety = bundle.getInt("soft_encode_stragety", SPConfig.SOFT_ENCODE_STRATEGY_MORE_QUALITY);
        mVideoBitrate = bitrate;
        VideoResolution videoResolution = (VideoResolution) bundle.getSerializable("video_resolution");
        mVideoRatio = (VideoRatio) bundle.getSerializable("video_ratio");
        String appId = bundle.getString("appId");
        String authKey = bundle.getString("authKey");
        boolean hasVideo = bundle.getBoolean("has_video");
        boolean hasAudio = bundle.getBoolean("has_audio");
        int customVideoSource = bundle.getInt("custom_video_source");
        boolean echoCancellatione = bundle.getBoolean("echo_cancellatione");
        mUseYuvPreHandler = bundle.getBoolean("yuv_pre_handler");
        mPushStreamType = (SPManager.PushStreamType) bundle.get("push_stream_type");

        mSPConfig = SPManager.getConfig();
        mSPConfig.setRtmpUrl(mPushUrl);
        mSPConfig.setSurfaceView(mPreviewView);
        mSPConfig.setCameraId(mCurrentCameraId);
        mSPConfig.setAudioSourceMode(audioSourceMode);
        mSPConfig.setEncoderMode(encoderState);
        mSPConfig.setDecoderMode(decoderState);
        mSPConfig.setAudioPlayMode(audioPlay);
        mSPConfig.setFps(frameRate);
        mSPConfig.setVideoBitrate(bitrate);
        mSPConfig.setAppIdAndAuthKey(appId, authKey);
        mSPConfig.setHasVideo(hasVideo);
        mSPConfig.setHasAudio(hasAudio);
        mSPConfig.setEchoCancellation(echoCancellatione);
        mSPConfig.setSoftEncodeStrategy(softEncodeStragety);

        switch (customVideoSource) {
            case 0://使用sdk采集，设置指定分辨率
            default:
                mSPConfig.setVideoResolution(videoResolution, mVideoRatio);
                break;
            case 1://使用自定义视频源yuv接口
            {
                String widthString = PreferenceUtil.getString(MainActivity.this, PreferenceUtil.KEY_PREVIEW_WIDTH);
                String heightString = PreferenceUtil.getString(MainActivity.this, PreferenceUtil.KEY_PREVIEW_HEIGHT);
//                int widthInt = widthString == null ? 0 : Integer.parseInt(widthString);
//                int heightInt = heightString == null ? 0 : Integer.parseInt(heightString);
                int widthInt = 1280;
                int heightInt = 720;
                // 设置摄像头支持的预览宽高
                // 取预览宽高的时候是宽大于高的，所以设置预览宽高时也要宽大于高
                CustomYuvSource customYuvSource = new CustomYuvSource(widthInt, heightInt);
                customYuvSource.setDisplayPreview(mPreviewView);
                if (widthInt > 0 && heightInt > 0) {
                    Log.i(TAG,"custom video width : " + widthInt + ", height : " + heightInt);
                    if (mScreenOrientation == Configuration.ORIENTATION_PORTRAIT) {
                        VideoResolution.VIDEO_RESOLUTION_CUSTOM.setWidth(heightInt);
                        VideoResolution.VIDEO_RESOLUTION_CUSTOM.setHeight(widthInt);
                    } else {
                        // 横屏交换用于编码的宽高，这是因为横屏是我们对yuv做了270旋转
                        VideoResolution.VIDEO_RESOLUTION_CUSTOM.setWidth(widthInt);
                        VideoResolution.VIDEO_RESOLUTION_CUSTOM.setHeight(heightInt);
                    }
                    mSPConfig.setVideoResolution(VideoResolution.VIDEO_RESOLUTION_CUSTOM, mVideoRatio, true);
                } else {
                    showToast("初始化自定义视频源失败");
                }
            }
            break;
            case 2://使用自定义视频源texture接口
            {
                int widthInt = 640;
                int heightInt = 360;
                //竖屏交换宽高
                if (mScreenOrientation == Configuration.ORIENTATION_PORTRAIT) {
                    int temp = widthInt;
                    widthInt = heightInt;
                    heightInt = temp;
                }
                ViewGroup.LayoutParams layoutParams = mPreviewView.getLayoutParams();
                GLSurfaceView glSurfaceView = new GLSurfaceView(this);
                ViewGroup parent = (ViewGroup) mPreviewView.getParent();
                parent.addView(glSurfaceView, layoutParams);
                parent.removeView(mPreviewView);
                CustomTextureSource customTextureSource = new CustomTextureSource(widthInt, heightInt);
                customTextureSource.setDisplayPreview(glSurfaceView);
                VideoResolution.VIDEO_RESOLUTION_CUSTOM.setWidth(widthInt);
                VideoResolution.VIDEO_RESOLUTION_CUSTOM.setHeight(heightInt);
                mSPConfig.setVideoResolution(VideoResolution.VIDEO_RESOLUTION_CUSTOM, mVideoRatio, true);
            }
            break;
        }

        // Reocrd
        mSPConfig.setRecordVideoType(mVideoType);
        mSPConfig.setMaxRecordDuration(mVideoMaxRecordDuration); // ms
        mSPConfig.setGIFMaxRecordDuration(mGIFMaxRecordDuration);
        mSPConfig.setMaxRecordFileSize(mMaxRecordFileSize); // byte
    }

    private void initLayout() {
        //setContentView(R.layout.activity_main);
        Bundle bundle = getIntent().getExtras();
        mScreenOrientation = bundle.getInt("screenOrientation");
        int requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        switch (mScreenOrientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                setContentView(R.layout.activity_main);
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                setContentView(R.layout.activity_main);
                break;
            case Configuration.ORIENTATION_UNDEFINED:
                int customVideoSource = bundle.getInt("custom_video_source");
                if (customVideoSource > 0) {//自定义视频源自动横竖屏需要用户自己实现
                    mScreenOrientation = Configuration.ORIENTATION_PORTRAIT;
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                } else {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;
                }
                setContentView(R.layout.activity_main);
            default:
                setContentView(R.layout.activity_main);
                break;
        }

        setRequestedOrientation(requestedOrientation);

        mPreviewView = (SPSurfaceView) findViewById(R.id.preview);
        mPreviewView.setScalingType(SPSurfaceView.SPScalingType.SCALE_ASPECT_FIT);
        mPreviewViewGroup = (ViewGroup) mPreviewView.getParent();
        mPreviewLayoutParams = mPreviewView.getLayoutParams();
        mInfoTv = (TextView) findViewById(R.id.tv_info);
        mTvFps = (TextView) findViewById(R.id.tv_fps);

        btn_record = (ImageButton) findViewById(R.id.btn_record);
        btn_record.setOnClickListener(this);

        btn_recordToFile = (ImageButton) findViewById(R.id.btn_recordToFile);
        btn_recordToFile.setOnClickListener(this);

        findViewById(R.id.btn_switch).setOnClickListener(this);

        mFlashImageBtn = (ImageButton) findViewById(R.id.btn_flash);
        mFlashImageBtn.setOnClickListener(this);
        mMuteImageBtn = (ImageButton) findViewById(R.id.btn_mute);
        mMuteImageBtn.setOnClickListener(this);

        findViewById(R.id.btn_setting).setOnClickListener(this);
        mSettingPanelUtil = new SettingsPanelViewUtil(this, new SettingsAdapter(this, mSettingItems), this);
    }

    private void initLayoutState() {
        setButtonEnabled(mFlashImageBtn, false);
        showRtmpUrl();
    }

    protected void resetPushState() {
        SPManager.PushState state = SPManager.getPushState();
        SPConfig config = SPManager.getConfig();

        mIsUserPushing = state.isPushing;
        btn_record.setSelected(mIsUserPushing);
        mCurrentCameraId = config.getCameraId();

        setButtonEnabled(mFlashImageBtn, state.isSupportFlash);
        mFlashImageBtn.setSelected(state.isFlashing);
        mMuteImageBtn.setSelected(state.isMute);

        if (!mIsUserPushing) {
            if (mIsUserRecording) {
                mIsUserRecording = !mIsUserRecording;
                SPManager.stopRecord();
                btn_recordToFile.setSelected(mIsUserRecording);
            }
        }

    }

    private FURendererListener mFURendererListener = new FURendererListener() {

        @Override
        public void onPrepare() {
            mFaceUnityDataFactory.bindCurrentRenderer();
        }

        @Override
        public void onTrackStatusChanged(FUAIProcessorEnum type, int status) {
            Log.e(TAG, "onTrackStatusChanged: 人脸数: " + status);
        }

        @Override
        public void onFpsChanged(double fps, double callTime) {
            final String FPS = String.format(Locale.getDefault(), "%.2f", fps);
            Log.e(TAG, "onFpsChanged: FPS " + FPS + " callTime " + String.format(Locale.getDefault(), "%.2f", callTime));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvFps.setText(FPS);
                }
            });
        }

        @Override
        public void onRelease() {
        }
    };

    private void initFilter() {
        String isOn = PreferenceUtil.getString(MyApp.getMyInstance(), PreferenceUtil.KEY_FACEUNITY_ISON);
        FaceUnityView beautyControlView = findViewById(R.id.faceunity_control);
        if ("false".equals(isOn)) {
            beautyControlView.setVisibility(View.GONE);
            return;
        }
        mFURenderer = FURenderer.getInstance();
        mFURenderer.setInputTextureType(FUInputTextureEnum.FU_ADM_FLAG_COMMON_TEXTURE);
        mFURenderer.setCameraFacing(mCurrentCameraId == CameraInfo.CAMERA_FACING_FRONT ? CameraFacingEnum.CAMERA_FRONT : CameraFacingEnum.CAMERA_BACK);
        mFURenderer.setInputOrientation(CameraUtils.getCameraOrientation(mCurrentCameraId));
        mFURenderer.setCreateEGLContext(true);
        mFURenderer.setMarkFPSEnable(true);
        mFURenderer.setInputBufferMatrix(mCurrentCameraId == CameraInfo.CAMERA_FACING_FRONT ? FUTransformMatrixEnum.CCROT0 : FUTransformMatrixEnum.CCROT0_FLIPVERTICAL);
        mFURenderer.setInputTextureMatrix(mCurrentCameraId == CameraInfo.CAMERA_FACING_FRONT ? FUTransformMatrixEnum.CCROT0 : FUTransformMatrixEnum.CCROT0_FLIPVERTICAL);
        mFURenderer.setOutputMatrix(mCurrentCameraId == CameraInfo.CAMERA_FACING_FRONT ? FUTransformMatrixEnum.CCROT0_FLIPVERTICAL : FUTransformMatrixEnum.CCROT0);
        mFURenderer.setWangsuCamera(true);
        mFURenderer.setFURendererListener(mFURendererListener);


        mFaceUnityDataFactory = new FaceUnityDataFactory(0);
        beautyControlView.bindDataFactory(mFaceUnityDataFactory);
        filter = new FaceUnityFilter(this, mFURenderer);
        SPManager.setFilter(filter);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void showRtmpUrl() {
        mRtmpUrlTv = (TextView) findViewById(R.id.tv_rtmpurl);
        mRtmpUrlTv.setText(getIntent().getExtras().getString("rtmp", null));
    }

    @Override
    protected void onResume() {
        Log.e(TAG, "onResume()...");
        super.onResume();
    }

    @Override
    protected void onStart() {
        Log.e(TAG, "onStart()...");
        super.onStart();
        showPushInfo();
        if(mPushStreamType != SPManager.PushStreamType.TYPE_SCREEN) {
            SPManager.onResume();
            //非录屏推流不支持后台推流，退后台需停止推流，回前台重新开始推流
            if(mIsUserPushing) {
                SPManager.startPushStream(SPManager.PushStreamType.TYPE_CAMERA);
            }
        } else {
            if(mFloatWindowService != null) {
                dismissFloatView();
            } else {
                SPManager.onResume();
            }
        }

    }

    @Override
    public void onClick(final View v) {

        switch (v.getId()) {
            case R.id.btn_record:
                mIsUserPushing = !mIsUserPushing;
                boolean mIsSuccess = false;
                if (mIsUserPushing) {
                    if(mPushStreamType == SPManager.PushStreamType.TYPE_SCREEN) {
                        //必须先调用service的startForeground，否则android 10以后将无法拿到录屏权限，导致崩溃
                        mFloatWindowService.showRecordingNotification();
                    }
                    mIsSuccess = SPManager.startPushStream(mPushStreamType);
                    mPreviewHeight = SPManager.getPushState().previewHeight;
                    mPreviewWidth = SPManager.getPushState().previewWidth;

                } else {
                    mIsSuccess = SPManager.stopPushStream();
                    if(mPushStreamType == SPManager.PushStreamType.TYPE_SCREEN) {
                        mFloatWindowService.cancleRecordingNotification();
                    }
                    if (mIsUserRecording && mIsSuccess) {
                        mIsUserRecording = !mIsUserRecording;
                        SPManager.stopRecord();
                        btn_recordToFile.setSelected(mIsUserRecording);
                    }

                }
                if (!mIsSuccess) {
                    mIsUserPushing = !mIsUserPushing;
                }
                if(mScreenOrientation==Configuration.ORIENTATION_UNDEFINED){
                    if (mIsUserPushing) {
                        lockScreenToCurrentOrientation();
                    } else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                    }
                }
                btn_record.setSelected(mIsUserPushing);

                break;
            case R.id.btn_recordToFile:
                mIsSuccess = false;
                if (mIsUserPushing) {
                    mIsUserRecording = !mIsUserRecording;
                    if (mIsUserRecording) {
                        mIsSuccess = SPManager.startRecord(mOutputFormat);
                    } else {
                        mIsSuccess = SPManager.stopRecord();
                    }
                    if (!mIsSuccess) {
                        mIsUserPushing = !mIsUserPushing;
                    }
                    btn_recordToFile.setSelected(mIsUserRecording);
                } else {
                    showToast("只能在推流状态下才可以录制视频！");
                }
                break;
            case R.id.btn_switch:
                if (SPManager.switchCamera()) {
                    mCurrentCameraId = mCurrentCameraId == 0 ? 1 : 0;
                    mFlashImageBtn.setSelected(false);
                    setButtonEnabled(mFlashImageBtn, false);

                    if (mFURenderer != null) {
                        mFURenderer.setCameraFacing(mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT ? CameraFacingEnum.CAMERA_FRONT : CameraFacingEnum.CAMERA_BACK);
                        mFURenderer.setInputOrientation(CameraUtils.getCameraOrientation(mCurrentCameraId));

                        mFURenderer.setInputBufferMatrix(mCurrentCameraId == CameraInfo.CAMERA_FACING_FRONT ? FUTransformMatrixEnum.CCROT0 : FUTransformMatrixEnum.CCROT0_FLIPVERTICAL);
                        mFURenderer.setInputTextureMatrix(mCurrentCameraId == CameraInfo.CAMERA_FACING_FRONT ? FUTransformMatrixEnum.CCROT0 : FUTransformMatrixEnum.CCROT0_FLIPVERTICAL);
                        mFURenderer.setOutputMatrix(mCurrentCameraId == CameraInfo.CAMERA_FACING_FRONT ? FUTransformMatrixEnum.CCROT0_FLIPVERTICAL : FUTransformMatrixEnum.CCROT0);
                    }
                }
                break;
            case R.id.btn_flash:
                if (SPManager.flashCamera(v.isSelected() ? SPManager.SWITCH_OFF : SPManager.SWITCH_ON)) {
                    v.setSelected(!v.isSelected());
                }
                break;
            case R.id.btn_mute:
                if (SPManager.muteMic(v.isSelected() ? SPManager.SWITCH_OFF : SPManager.SWITCH_ON)) {
                    v.setSelected(!v.isSelected());
                }
                break;
            case R.id.btn_setting:
                if (mSettingPanelUtil.isShowing()) {
                    mSettingPanelUtil.dismiss();
                } else {
                    mSettingPanelUtil.show();
                }
            default:
                break;
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause()...");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.e(TAG, "onStop()... isFinishing ： " + isFinishing());
        // 为保证时序，只有当前Activity才调用SPManager接口
        if (mCurActivityHashCode == this.hashCode()) {
            if(isFinishing() || mPushStreamType != SPManager.PushStreamType.TYPE_SCREEN) {
                if(mIsUserPushing) {
                    SPManager.stopPushStream();
                }
                if (mIsUserRecording) {
                    SPManager.stopRecord();
                }
                SPManager.onPause();
            } else {
                showFloatView();
            }
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        ALog.i(TAG, "onDestroy -- ");
        // 为保证时序，只有当前Activity才调用SPManager接口
        if (mCurActivityHashCode == this.hashCode()) {
            SPManager.stopPushStream();
            SPManager.onPause();
            SPManager.release();
            //内部是静态引用，要主动置null，否则可能造成内存泄漏
            SPManager.setOnErrorListener(null);
            SPManager.setOnStateListener(null);
        }
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        dismissFloatView();
        unbindFloatWindowService();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    protected void setButtonEnabled(ImageButton button, boolean enabled) {
        if (button == null)
            return;
        if (enabled) {
            button.setEnabled(true);
            button.clearColorFilter();
        } else {
            button.setEnabled(false);
            button.setColorFilter(0xAA000000);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SettingItem item = mSettingItems.get(position);
        if (item == null)
            return;
        final SPConfig config = SPManager.getConfig();
        PushState state = SPManager.getPushState();
        switch (item.settingId) {
            case R.id.setting_set_rtmpurl:
                if (state.isPushing) {
                    Toast.makeText(MainActivity.this, " 该参数只能在非推流状态下设置", Toast.LENGTH_SHORT).show();
                    return;
                }
                AlertDialog dialog = DialogUtils.showRtmpUrlInputDialog(MainActivity.this, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            EditText et = (EditText) ((AlertDialog) dialog).findViewById(android.R.id.edit);
                            if (et != null) {
                                String pushUrl = et.getText().toString();
                                if (SPManager.setConfig(SPManager.getConfig().setRtmpUrl(pushUrl))) {
                                    mPushUrl = et.getText().toString();
                                    mRtmpUrlTv.setText(et.getText());
                                    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                                    Editor editor = preferences.edit();
                                    editor.putString("rtmp_url", mPushUrl);
                                    editor.commit();
                                } else {
                                    Toast.makeText(MainActivity.this, "正在推流，无法修改参数", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                });
                EditText et = (EditText) dialog.findViewById(android.R.id.edit);
                et.setText(mPushUrl);
                break;
            case R.id.setting_set_camera_focus:
                DialogUtils.showSingleChoiceDialog(this, "请选择聚焦模式", new String[] { "自动聚焦", "手动聚焦" }, SPManager.getConfig().isCameraManualFocusMode() ? 1 : 0, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SPManager.cameraManualFocusMode(which == 1 ? SPManager.SWITCH_ON : SPManager.SWITCH_OFF);

                    }
                });
                break;
            case R.id.setting_set_audio_loop:
                DialogUtils.showSingleChoiceDialog(this, "耳返", new String[] { "关闭", "打开" }, SPManager.getPushState().audioLoopActive ? 1 : 0, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SPManager.switchAudioLoop(which == 1 ? SPManager.SWITCH_ON : SPManager.SWITCH_OFF);

                    }
                });
                break;
            case R.id.setting_set_audio_reverb:
                DialogUtils.showSingleChoiceDialog(this, "混响", new String[] { "关闭", "level-1", "level-2", "level-3", "level-4", "level-5" }, SPManager.getPushState().audioReverbLevel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SPManager.setAudioReverbLevel(which);

                    }
                });
                break;
            case R.id.setting_set_bgm:
                if (mBgmPickDialog == null) {
                    mBgmPickDialog = new MusicPickDialog();
                }
                if (mBgmPickDialog.isAdded())
                    return;
                mBgmPickDialog.setOnclickListener(new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            if (mBgmPlayer != null) {//正在播放，停止播放
                                SPManager.releaseBgmPlayer(mBgmPlayer);
                                mBgmPlayer = null;
                                if(mBgmPickDialog != null) {
                                    mBgmPickDialog.setActivePlayer(null);
                                }
                            } else {//当前没有歌曲播放，则开始播放
                                mBgmFiles = ((MusicPickDialog) dialog).getCheckedMusicList();
                                if (mBgmFiles.size() == 0) {
                                    mCurrentBgmIndex = -1;
                                } else {
                                    mCurrentBgmIndex = 0;
                                    try {
                                        mBgmPlayer = SPManager.createBgmPlayer(mBgmFiles.get(mCurrentBgmIndex));
                                        mBgmPlayer.setPlayerListener(mBgmListener);
                                        if(mBgmFiles.size() == 1) {//单首歌曲设置循环播放
                                            mBgmPlayer.setLooping(true);
                                        }
                                        if(mBgmPickDialog != null) {
                                            mBgmPickDialog.setActivePlayer(mBgmPlayer);
                                        }
                                    } catch (Exception e) {
                                        showToast("背景音乐加载失败");
                                        mBgmPlayer = null;
                                    }
                                }
                            }
                            mIsPause = false;
                        } else if(which == DialogInterface.BUTTON_NEGATIVE) {
                            if (mBgmPlayer == null) {
                                showToast("当前没有背景音在播放");
                                return;
                            }
                            if (mIsPause) {
                                try {
                                    //必须等待onPrepared回调后才可以播放
                                    mBgmPlayer.start();
                                    mIsPause = false;
                                } catch (IllegalStateException e) {
                                    e.printStackTrace();
                                    showToast("歌曲还未准备好，请稍后");
                                }
                            } else {
                                mBgmPlayer.pause();
                                mIsPause = true;
                            }
                        }

                    }
                });
                mBgmPickDialog.show(getFragmentManager(), mBgmPickDialog.getClass().getSimpleName());
                break;
            case R.id.setting_set_volume:
                DialogUtils.showVolumeAdjustDialog(MainActivity.this, (int) (mCurrentMicVolume * 20), (int) (mCurrentBgmVolume * 20), new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (!fromUser)
                            return;
                        float seekbarProgess = (float) progress / seekBar.getMax();
                        Log.i(TAG, "seekbarProgess == " + seekbarProgess);
                        switch (seekBar.getId()) {
                            case R.id.sb_mic_volume:
                                if (mCurrentMicVolume != seekbarProgess && SPManager.setMicVolume(seekbarProgess)) {
                                    mCurrentMicVolume = seekbarProgess;
                                    Log.i(TAG, "setMicVolume" + seekbarProgess);
                                }
                                break;
                            case R.id.sb_bgm_volume:
                                if (mCurrentBgmVolume != seekbarProgess && SPManager.setBgmVolume(seekbarProgess)) {
                                    mCurrentBgmVolume = seekbarProgess;
                                    Log.i(TAG, "setBgmVolume" + seekbarProgess);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                });

                break;
            case R.id.setting_set_bitrate:
                if (state.isPushing) {
                    Toast.makeText(MainActivity.this, " 该参数只能在非推流状态下设置", Toast.LENGTH_SHORT).show();
                    return;
                }
                StringBuilder promptSb = new StringBuilder();
                promptSb.append("码率范围:\n360P ");
                promptSb.append(VideoResolution.VIDEO_RESOLUTION_360P.getMinBitrate() / 1024);
                promptSb.append("k -- ");
                promptSb.append(VideoResolution.VIDEO_RESOLUTION_360P.getMaxBitrate() / 1024);
                promptSb.append("k\n480P ");
                promptSb.append(VideoResolution.VIDEO_RESOLUTION_480P.getMinBitrate() / 1024);
                promptSb.append("k -- ");
                promptSb.append(VideoResolution.VIDEO_RESOLUTION_480P.getMaxBitrate() / 1024);
                promptSb.append("k\n540P ");
                promptSb.append(VideoResolution.VIDEO_RESOLUTION_540P.getMinBitrate() / 1024);
                promptSb.append("k -- ");
                promptSb.append(VideoResolution.VIDEO_RESOLUTION_540P.getMaxBitrate() / 1024);
                promptSb.append("k\n720P ");
                promptSb.append(VideoResolution.VIDEO_RESOLUTION_720P.getMinBitrate() / 1024);
                promptSb.append("k -- ");
                promptSb.append(VideoResolution.VIDEO_RESOLUTION_720P.getMaxBitrate() / 1024);
                promptSb.append("k");
                showBitrateInputDialog(promptSb.toString());
                break;
            case R.id.setting_set_fps:
                if (state.isPushing) {
                    Toast.makeText(MainActivity.this, " 该参数只能在非推流状态下设置", Toast.LENGTH_SHORT).show();
                    return;
                }
                showFPSInputDialog();
                break;
            case R.id.setting_set_variable_framerate:
                if (state.isPushing) {
                    Toast.makeText(MainActivity.this, " 该参数只能在非推流状态下设置", Toast.LENGTH_SHORT).show();
                    return;
                }
                DialogUtils.showSingleChoiceDialog(this, "请选择可变帧率状态", new String[] { "关闭", "打开" }, config.isVarFramerate() ? 1 : 0, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SPManager.setConfig(config.setVarFramerate(which == 1));
                    }
                });
                break;
            case R.id.setting_set_flip:
                SPManager.flipCamera();
                break;
            case R.id.setting_set_auto_bitrate:
                if (state.isPushing) {
                    Toast.makeText(MainActivity.this, " 该参数只能在非推流状态下设置", Toast.LENGTH_SHORT).show();
                    return;
                }
                final SPConfig spConfig = SPManager.getConfig();
                DialogUtils.showSingleChoiceDialog(this, "请选择聚码率自适应状态", new String[] { "关闭", "打开" }, config.isAutoBitrate() ? 1 : 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SPManager.setConfig(config.setAutoBitrate(which == 1));
                        mSPConfig.getVideoResolution().resetCustomBitrate();
                        if (which == 1) {
                            showAutoBitrateRangeInputDialog();
                        }
                    }
                });
                break;
            case R.id.setting_set_record_format:
                if (state.isPushing) {
                    Toast.makeText(MainActivity.this, " 该参数只能在非推流状态下设置", Toast.LENGTH_SHORT).show();
                    return;
                }
                showRecordFormatDialog();
                break;
            case R.id.setting_set_watermark:
                final CharSequence[] items = {"时间水印", "logo水印", "时间贴图", "logo贴图", "移动贴图", "gif贴图"};
                DialogUtils.showMultiChoiceDialog(this, "请选择水印类型", items, mWaterMarkSelected, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        mWaterMarkSelected[which] = isChecked;
                    }
                }, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            updateWaterMarkState(getResources().getConfiguration().orientation);
                        }
                    }
                });
                break;
            case R.id.setting_take_screenshot:
                new ScreenContentRecorder(MainActivity.this, mFloatWindowService).takeShotOptional(mPreviewView, findViewById(android.R.id.content));
                break;
            case R.id.setting_beauty:
                DialogUtils.showBeautyPickDialog(MainActivity.this);
                break;
            case R.id.setting_set_socks5_proxy:
                if (state.isPushing) {
                    Toast.makeText(MainActivity.this, " 该参数只能在非推流状态下设置", Toast.LENGTH_SHORT).show();
                    return;
                }
                final SPConfig.Socks5Proxy proxyConfig = SPManager.getConfig().getSocks5Proxy();
                DialogUtils.showSingleChoiceDialog(this, "请设置代理服务器", new String[] { "关闭", "打开" }, proxyConfig.enabled ? 1 : 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        proxyConfig.enabled = (which == 1);
                        if (which == 1) {
                            showSocks5ProxyInputDialog();
                        } else {
                            SPManager.setConfig(SPManager.getConfig().setSocks5Proxy(proxyConfig));
                        }
                    }
                });
                break;
            case R.id.setting_set_mirror:
                boolean mirrorParams[] = new boolean[2];
                getMirrorParam(mirrorParams);
                DialogUtils.showMultiChoiceDialog(this, "镜像调节", new String[]{ "预览镜像", "编码镜像" }, mirrorParams, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if(which == 0) {//设置预览镜像
                            PreferenceUtil.persistString(MainActivity.this, PreferenceUtil.KEY_PREVIEW_MIRROR + mOpenedCameraId, "" + isChecked);
                        }else if(which == 1) {//设置编码镜像
                            PreferenceUtil.persistString(MainActivity.this, PreferenceUtil.KEY_ENCODE_MIRROR + mOpenedCameraId, "" + isChecked);
                        }
                    }
                }, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            boolean mirrorParams[] = new boolean[2];
                            getMirrorParam(mirrorParams);
                            //预览过程中由用户手工设置预览和推流是否镜像
                            SPManager.setMirror(mirrorParams[0]/*previewMirror*/, mirrorParams[1]/*encodeMirror*/);
                        }
                    }
                });
                break;
            case R.id.setting_add_question:
                if (!state.isPushing) {
                    Toast.makeText(MainActivity.this, " 该参数只能在推流状态下设置", Toast.LENGTH_SHORT).show();
                    return;
                }
                final QuestionGroup questions = QuestionUtils.loadTestQuestions(this);
                if(questions == null || questions.questionList == null || questions.questionList.isEmpty()) {
                    Toast.makeText(this, "未找到可用题目",Toast.LENGTH_LONG).show();
                    break;
                }
                final String questionStrings[] = new String[questions.questionList.size()];
                for (int i = 0; i < questionStrings.length; i++) {
                    Question question = questions.questionList.get(i);
                    if(question.type == Question.TYPE_SINGLE_CHOICE) {
                        questionStrings[i] = "(单选) " + question.question;
                    }else  if(question.type == Question.TYPE_MULTI_CHOICE){
                        questionStrings[i] = "(多选) " + question.question;
                    } else {
                        questionStrings[i] = question.question;
                    }
                }
                DialogUtils.showSingleChoiceDialog(this, "请选择测试题目",questionStrings, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        QuestionGroup questionGroup = new QuestionGroup();
                        questionGroup.userType = questions.userType;
                        questionGroup.questionList = new ArrayList<>(1);
                        questionGroup.questionList.add(questions.questionList.get(which));
                        SPManager.addSeiContent(questionGroup.toString());
                    }
                });
                break;
            default:
                break;
        }

    }

    private void updateWaterMarkState(int orientation) {
        SPManager.hideWaterMarkTime();
        SPManager.hideWaterMarkLogo();
        if (mWaterMarkSelected[0]) {
            if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                SPManager.showWaterMarkTime(0.02f, 0.02f, 0.4f, Color.WHITE, 1.0f);
            } else {
                SPManager.showWaterMarkTime(0.02f, 0.01f, 0.2f, Color.WHITE, 1.0f);
            }
        }

        if (mWaterMarkSelected[1]) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                if (mVideoRatio == VideoRatio.RATIO_4_3) {
                    SPManager.showWaterMarkLogo("assets://logo.png", 1.0f, 0.00f, 0.100f, 0.075f, 1.0f);
                } else {
                    SPManager.showWaterMarkLogo("assets://logo.png", 1.0f, 0.00f, 0.133f, 0.075f, 1.0f);
                }
            } else {
                if (mVideoRatio == VideoRatio.RATIO_4_3) {
                    SPManager.showWaterMarkLogo("assets://logo.png", 1.0f, 0.00f, 0.075f, 0.100f, 1.0f);
                } else {
                    SPManager.showWaterMarkLogo("assets://logo.png", 1.0f, 0.00f, 0.075f, 0.133f, 1.0f);
                }
            }
        }

        //时间贴图
        if(mWaterMarkSelected[2]) {
            if(mStickerControllers[2] == null) {
                TimeStickerController stickerObject = new TimeStickerController(this);
                stickerObject.zOrder = 2;
                mStickerControllers[2] = stickerObject;
                SPManager.addSticker(stickerObject);
            }
        } else {
            if(mStickerControllers[2] != null) {
                SPManager.removeSticker(mStickerControllers[2]);
                mStickerControllers[2] = null;
            }
        }

        //logo贴图
        if(mWaterMarkSelected[3]) {
            if(mStickerControllers[3] == null) {
                MoveStickerController stickerObject = new MoveStickerController(this);
                stickerObject.x = 0.85f;
                stickerObject.y = 0.0f;
                stickerObject.width = stickerObject.height = 0.15f;
                stickerObject.setDataSource(R.drawable.ic_launcher);
                stickerObject.zOrder = 3;
                mStickerControllers[3] = stickerObject;
                SPManager.addSticker(stickerObject);
            }
        } else {
            if(mStickerControllers[3] != null) {
                SPManager.removeSticker(mStickerControllers[3]);
                mStickerControllers[3] = null;
            }
        }

        //移动贴图
        if(mWaterMarkSelected[4]) {
            if(mStickerControllers[4] == null) {
                MoveStickerController stickerObject = new MoveStickerController(this);
                stickerObject.x = stickerObject.y = 0.0f;
                if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    stickerObject.width = 0.4f;
                    stickerObject.height = 0.1f;
                } else {
                    stickerObject.width = 0.2f;
                    stickerObject.height = 0.2f;
                }

                stickerObject.setDataSource(R.raw.ws_logo);
                stickerObject.setMovable(true);
                stickerObject.zOrder = 4;
                mStickerControllers[4] = stickerObject;
                SPManager.addSticker(stickerObject);
            }
        } else {
            if(mStickerControllers[4] != null) {
                SPManager.removeSticker(mStickerControllers[4]);
                mStickerControllers[4] = null;
            }
        }

        //gif贴图
        if(mWaterMarkSelected[5]) {
            if(mStickerControllers[5] == null) {
                GifStickerController gifStickerObject = new GifStickerController(this);
                gifStickerObject.x = 0.1f;
                gifStickerObject.y = 0.4f;
                gifStickerObject.width = 0.8f;
                gifStickerObject.height = 0.6f;
                gifStickerObject.setDataSource(R.raw.fireworks);
                gifStickerObject.setLoopCount(GifStickerController.LOOP_FOREVER);
                gifStickerObject.zOrder = 5;
                mStickerControllers[5] = gifStickerObject;
                SPManager.addSticker(gifStickerObject);
            }
        } else {
            if(mStickerControllers[5] != null) {
                SPManager.removeSticker(mStickerControllers[5]);
                mStickerControllers[5] = null;
            }
        }
    }


    private void showPushInfo() {
        PushState state = SPManager.getPushState();
        mVideoHeight = state.videoHeight;
        mVideoWidth = state.videoWidth;
        String content = "height :" + mVideoHeight + "\nwidth: " + mVideoWidth + "\npreview height:" + mPreviewHeight + "\npreview widht:" + mPreviewWidth + "\nspeed: " + mPushSpeed + "KB/S" + "\nbitrate :" + mVideoBitrate / 1024 + "k" + "\nencode fps:" + mFrameEncodeRate + "\npush fps:" + mFrameRate;
        mInfoTv.setText(content);
    }

    private void showConnectErrorDialog(String info) {
        DialogUtils.showAlertDialog(this, info);
    }

    private void showBitrateInputDialog(final String promptSb) {
        DialogUtils.showSingleInputNumberDialog(this, "请输入码率", promptSb, "", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (which == DialogInterface.BUTTON_POSITIVE) {
                    EditText et = (EditText) ((AlertDialog) dialog).findViewById(R.id.edit_text);
                    String numString = et.getText().toString();
                    int bitrate = 0;
                    try {
                        int originalData = TextUtils.isEmpty(numString) ? 0 : Integer.parseInt(numString);
                        if (0 < originalData && originalData < Integer.MAX_VALUE / 1024) {
                            bitrate = originalData * 1024;
                            SPManager.setConfig(SPManager.getConfig().setVideoBitrate(bitrate));
                        } else {
                            showToast("输入参数不合法，请重新输入");
                            showBitrateInputDialog(promptSb);
                        }
                    } catch (Exception e) {
                        showToast("输入参数不合法，请重新输入");
                        showBitrateInputDialog(promptSb);
                    }
                }
            }
        });
    }

    private void showFPSInputDialog() {
        DialogUtils.showSingleInputNumberDialog(this, "请输入帧率", "帧率(15~30)", "", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (which == DialogInterface.BUTTON_POSITIVE) {
                    EditText et = (EditText) ((AlertDialog) dialog).findViewById(R.id.edit_text);
                    String numString = et.getText().toString();
                    int frameRate = mFrameRate;
                    try {
                        frameRate = TextUtils.isEmpty(numString) ? 0 : Integer.parseInt(numString);
                    } catch (Exception e) {
                        showToast("输入参数不合法，请重新输入");
                        showFPSInputDialog();
                        return;
                    }
                    SPManager.setConfig(SPManager.getConfig().setFps(TextUtils.isEmpty(numString) ? 0 : Integer.parseInt(numString)));
                }
            }
        });
    }

    private void showRecordFormatDialog() {
        int whichFormat = 0;
        switch (mOutputFormat) {
            case MUXER_OUTPUT_FLV:
                whichFormat = 0;
                break;
            case MUXER_OUTPUT_MPEG_4:
                whichFormat = 1;
                break;
            case MUXER_OUTPUT_GIF:
                whichFormat = 2;
                break;
            default:
                break;
        }
        DialogUtils.showSingleChoiceDialog(this, "请选择录制小视频的格式", new String[] { "flv", "mp4", "gif" }, whichFormat, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                int whichType = 0;

                switch (which) {
                    case 0:
                        mOutputFormat = OutputFormat.MUXER_OUTPUT_FLV;
                        mVideoType = VideoType.TYPE_SHORT_VIDEO;
                        DialogUtils.showSingleChoiceDialog(MainActivity.this, "请选择录制小视频的类型", new String[] { "短视频", "长视频" }, whichType, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        mVideoType = VideoType.TYPE_SHORT_VIDEO;
                                        showRecordShortVideoParameterInputDialog();
                                        break;
                                    case 1:
                                        mVideoType = VideoType.TYPE_LONG_VIDEO;
                                        showRecordLongVideoParameterInputDialog();
                                        break;
                                    default:
                                        break;
                                }
                                SPConfig config = SPManager.getConfig();
                                config.setMaxRecordDuration(mVideoMaxRecordDuration);
                                config.setRecordVideoType(mVideoType);
                                SPManager.setConfig(config);
                            }
                        });
                        break;
                    case 1:
                        mOutputFormat = OutputFormat.MUXER_OUTPUT_MPEG_4;
                        mVideoType = VideoType.TYPE_SHORT_VIDEO;
                        DialogUtils.showSingleChoiceDialog(MainActivity.this, "请选择录制小视频的类型", new String[] { "短视频", "长视频" }, whichType, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        mVideoType = VideoType.TYPE_SHORT_VIDEO;
                                        showRecordShortVideoParameterInputDialog();
                                        break;
                                    case 1:
                                        mVideoType = VideoType.TYPE_LONG_VIDEO;
                                        showRecordLongVideoParameterInputDialog();
                                        break;
                                    default:
                                        break;
                                }
                                SPConfig config = SPManager.getConfig();
                                config.setMaxRecordDuration(mVideoMaxRecordDuration);
                                config.setRecordVideoType(mVideoType);
                                SPManager.setConfig(config);
                            }
                        });
                        break;
                    case 2:
                        mOutputFormat = OutputFormat.MUXER_OUTPUT_GIF;
                        mVideoType = VideoType.TYPE_GIF;
                        SPConfig config = SPManager.getConfig();
                        config.setRecordVideoType(mVideoType);
                        config.setGIFMaxRecordDuration(mGIFMaxRecordDuration);
                        SPManager.setConfig(config);
                        showRecordGIFVideoParameterInputDialog();
                        break;
                    default:
                        break;
                }
                SPConfig config = SPManager.getConfig();
                config.setRecordVideoType(mVideoType);
                SPManager.setConfig(config);
            }
        });
    }

    private void showRecordShortVideoParameterInputDialog() {
        DialogUtils.showConfigInputDialog(this, EditorInfo.TYPE_NUMBER_FLAG_DECIMAL, new String[] { "mVideoMaxRecordDuration" }, new String[] { String.valueOf(mVideoMaxRecordDuration) }, new String[] { "时长[3000~60000],单位ms" }, new InputConfigClickListener() {

            @Override
            public void onResult(DialogInterface dialog, HashMap<String, String> result) {
                String maxRecordDuration = result.get("mVideoMaxRecordDuration");
                try {
                    mVideoMaxRecordDuration = TextUtils.isEmpty(maxRecordDuration) ? mVideoMaxRecordDuration : Integer.parseInt(maxRecordDuration);
                } catch (Exception e) {
                    showRecordShortVideoParameterInputDialog();
                    showToast("输入参数不合法，请重新输入");
                    return;
                }

                if (mVideoMaxRecordDuration < 3 * 1000 || mVideoMaxRecordDuration > 60 * 1000) {
                    showRecordShortVideoParameterInputDialog();
                    showToast("输入参数不合法，请重新输入");
                    return;
                }
                SPConfig config = SPManager.getConfig();
                config.setMaxRecordDuration(mVideoMaxRecordDuration);
                SPManager.setConfig(config);
            }
        });
    }

    private void showRecordGIFVideoParameterInputDialog() {
        DialogUtils.showConfigInputDialog(this, EditorInfo.TYPE_NUMBER_FLAG_DECIMAL, new String[] { "mGIFMaxRecordDuration" }, new String[] { String.valueOf(mGIFMaxRecordDuration) }, new String[] { "时长[1000~5000],单位ms" }, new InputConfigClickListener() {

            @Override
            public void onResult(DialogInterface dialog, HashMap<String, String> result) {
                String maxRecordDuration = result.get("mGIFMaxRecordDuration");
                try {
                    mGIFMaxRecordDuration = TextUtils.isEmpty(maxRecordDuration) ? mGIFMaxRecordDuration : Integer.parseInt(maxRecordDuration);
                } catch (Exception e) {
                    showRecordGIFVideoParameterInputDialog();
                    showToast("输入参数不合法，请重新输入");
                    return;
                }
                if (mGIFMaxRecordDuration < 1000 || mGIFMaxRecordDuration > 5 * 1000) {
                    showRecordGIFVideoParameterInputDialog();
                    showToast("输入参数不合法，请重新输入");
                    return;
                }
                SPConfig config = SPManager.getConfig();
                config.setGIFMaxRecordDuration(mGIFMaxRecordDuration);
                SPManager.setConfig(config);
            }
        });
    }

    private void showRecordLongVideoParameterInputDialog() {
        DialogUtils.showConfigInputDialog(this, EditorInfo.TYPE_NUMBER_FLAG_DECIMAL, new String[] { "mMaxRecordFileSize" }, new String[] { String.valueOf(mMaxRecordFileSize) }, new String[] { "大小[ >=102400],单位byte" }, new InputConfigClickListener() {

            @Override
            public void onResult(DialogInterface dialog, HashMap<String, String> result) {
                String maxRecordFileSize = result.get("mMaxRecordFileSize");
                try {
                    mMaxRecordFileSize = TextUtils.isEmpty(maxRecordFileSize) ? mMaxRecordFileSize : Long.parseLong(maxRecordFileSize);
                } catch (Exception e) {
                    showRecordLongVideoParameterInputDialog();
                    showToast("输入参数不合法，请重新输入");
                    return;
                }

                if (mMaxRecordFileSize < 100 * 1024) {
                    showRecordLongVideoParameterInputDialog();
                    showToast("输入参数不合法，请重新输入");
                    return;
                }
                SPConfig config = SPManager.getConfig();
                config.setMaxRecordFileSize(mMaxRecordFileSize);
                SPManager.setConfig(config);
            }
        });
    }

    private void showAutoBitrateRangeInputDialog() {
        DialogUtils.showConfigInputDialog(MainActivity.this, EditorInfo.TYPE_CLASS_NUMBER, new String[] { "min_bitrate", "max_bitrate" }, new String[] { String.valueOf(mSPConfig.getVideoResolution().getMinBitrate() / 1024), String.valueOf(mSPConfig.getVideoResolution().getMaxBitrate() / 1024) }, new String[] { "上限", "下限" }, new InputConfigClickListener() {

            @Override
            public void onResult(DialogInterface dialog, HashMap<String, String> result) {
                try {
                    int customMinBitrate = Integer.parseInt(result.get("min_bitrate"));
                    int customMaxBitrate = Integer.parseInt(result.get("max_bitrate"));
                    if (customMinBitrate > Integer.MAX_VALUE / 1024 || customMaxBitrate > Integer.MAX_VALUE / 1024) {
                        throw new IllegalArgumentException();
                    }
                    mSPConfig.getVideoResolution().setCustomBitrate(customMinBitrate * 1024, customMaxBitrate * 1024);
                } catch (Exception e) {
                    showToast("输入参数不合法，请重新输入");
                    showAutoBitrateRangeInputDialog();
                }
            }
        });
    }

    private void showSocks5ProxyInputDialog() {
        final SPConfig.Socks5Proxy socks5Proxy = SPManager.getConfig().getSocks5Proxy();
        DialogUtils.showConfigInputDialog(MainActivity.this, EditorInfo.TYPE_CLASS_TEXT, new String[] { "socks5_ip", "socks5_port", "socks5_username", "socks5_pwd" }, new String[] { socks5Proxy.ip, String.valueOf(socks5Proxy.port), socks5Proxy.username, socks5Proxy.pwd }, new String[] { "IP", "端口", "用户名", "密码" }, new InputConfigClickListener() {

            @Override
            public void onResult(DialogInterface dialog, HashMap<String, String> result) {
                try {
                    socks5Proxy.ip = result.get("socks5_ip");
                    socks5Proxy.port = Integer.parseInt(result.get("socks5_port"));
                    socks5Proxy.username = result.get("socks5_username");
                    socks5Proxy.pwd = result.get("socks5_pwd");
                    socks5Proxy.enabled = true;
                    SPManager.setConfig(SPManager.getConfig().setSocks5Proxy(socks5Proxy));
                } catch (Exception e) {
                    showToast("输入参数不合法，请重新输入");
                    showSocks5ProxyInputDialog();
                }
            }
        });
    }
    /**
     * 返回当前设置的镜像参数
     * param[0]:预览是否镜像
     * param[1]:编码是否镜像
     * @param params
     */
    private void getMirrorParam(boolean params[]) {
        String previewStr = PreferenceUtil.getString(MainActivity.this, PreferenceUtil.KEY_PREVIEW_MIRROR + mOpenedCameraId);
        //如果用户没有设置预览镜像，默认前置摄像头预览镜像，后置摄像头不镜像
        params[0] = previewStr == null ? mOpenedCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT : Boolean.parseBoolean(previewStr) ;
        String encodeStr = PreferenceUtil.getString(MainActivity.this, PreferenceUtil.KEY_ENCODE_MIRROR + mOpenedCameraId);
        //如果用户没有设置编码镜像，默认编码不镜像
        params[1] = encodeStr == null ? false : Boolean.parseBoolean(encodeStr) ;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED && (Manifest.permission.CAMERA.equals(permissions[i]) || Manifest.permission.RECORD_AUDIO.equals(permissions[i]))) {
                SPManager.init(this, mSPConfig);
                SPManager.onResume();
                break;
            }
        }
    }

    private SPAudioPlayer.SPAudioPlayerListener mBgmListener = new SPAudioPlayer.SPAudioPlayerListener() {
        @Override
        public void onError(SPAudioPlayer ap, int what, String extra) {
            showToast("背景音乐解析失败");
            SPManager.releaseBgmPlayer(ap);
            mBgmPlayer = null;
            mIsPause = false;
            mCurrentBgmIndex = -1;
            if(mBgmPickDialog != null) {
                mBgmPickDialog.setActivePlayer(null);
            }
        }

        @Override
        public void onPrepared(SPAudioPlayer ap) {
            if(mBgmPlayer != ap) return;
            try {
                //必须等待onPrepared回调后才可以播放
                ap.start();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCompletion(SPAudioPlayer ap) {
            if(mBgmPlayer != ap) {
                SPManager.releaseBgmPlayer(ap);
            }
            if(mBgmPlayer != null) {
                SPManager.releaseBgmPlayer(mBgmPlayer);
                mBgmPlayer = null;
                if(mBgmPickDialog != null) {
                    mBgmPickDialog.setActivePlayer(null);
                }
            }
            if (mBgmFiles.size() == 0) {
                mCurrentBgmIndex = -1;
                return;
            }
            mCurrentBgmIndex++;
            if (mCurrentBgmIndex >= mBgmFiles.size()) {
                mCurrentBgmIndex = 0;
            }
            try {
                mBgmPlayer = SPManager.createBgmPlayer(mBgmFiles.get(mCurrentBgmIndex));
                mBgmPlayer.setPlayerListener(mBgmListener);
                if(mBgmFiles.size() == 1) {//单首歌曲设置循环播放
                    mBgmPlayer.setLooping(true);
                }
                if(mBgmPickDialog != null) {
                    mBgmPickDialog.setActivePlayer(mBgmPlayer);
                }
            } catch (Exception e) {
                showToast("背景音乐加载失败");
                mBgmPlayer = null;
            }
        }

        @Override
        public void onProgressChanged(SPAudioPlayer ap, long progress) {
            //do not call SPAudioPlayer.start(),SPAudioPlayer.stop(),SPManager.createBgmPlayer(),SPManager.releaseBgmPlayer() in this method
            //it will block the parse thread.
        }
    };

    private void showFloatView() {
        if(mFloatWindowService == null) {
            return;
        }
        ViewGroup viewGroup = (ViewGroup) mPreviewView.getParent();
        if(viewGroup != null) {
            viewGroup.removeView(mPreviewView);
        }
        if(checkOverlayPermission()) {
            mFloatWindowService.showFloatSurfaceView(mPreviewView, mVideoWidth, mVideoHeight);
        } else {
            showToast("没有弹窗权限，无法正常显示摄像头预览。");
        }
    }

    private void dismissFloatView() {
        if (mFloatWindowService == null) {
            return;
        }
        mFloatWindowService.dismissSurfaceView();
        ViewGroup viewGroup = (ViewGroup) mPreviewView.getParent();
        if(viewGroup != null) {
            viewGroup.removeView(mPreviewView);
        }
        mPreviewViewGroup.addView(mPreviewView, mPreviewLayoutParams);
    }

    private void bindFloatWindowService() {
        if (mFloatWindowServiceConnection != null) {
            return;
        }
        Intent intent = new Intent(this, FloatWindowService.class);
        mFloatWindowServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ALog.i(TAG, "FloatWindowService onServiceConnected...");
                mFloatWindowService = ((FloatWindowService.FloatBinder)service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                ALog.i(TAG, "FloatWindowService onServiceDisconnected... : " + name);
            }
        };
        bindService(intent, mFloatWindowServiceConnection , Context.BIND_AUTO_CREATE);
    }

    private void unbindFloatWindowService() {
        if (mFloatWindowServiceConnection == null) {
            return;
        }
        unbindService(mFloatWindowServiceConnection);
        mFloatWindowServiceConnection = null;

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            if (Math.abs(x) > 3 || Math.abs(y) > 3) {
                if (Math.abs(x) > Math.abs(y)) {
                    mFURenderer.setDeviceOrientation(x > 0 ? 0 : 180);
                } else {
                    mFURenderer.setDeviceOrientation(y > 0 ? 90 : 270);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
