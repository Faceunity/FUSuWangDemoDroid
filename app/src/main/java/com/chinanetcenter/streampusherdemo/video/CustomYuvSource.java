package com.chinanetcenter.streampusherdemo.video;

import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.chinanetcenter.StreamPusher.sdk.SPManager;
import com.chinanetcenter.StreamPusher.sdk.SPSurfaceView;
import com.chinanetcenter.StreamPusher.utils.ALog;

import java.util.Iterator;
import java.util.List;

@SuppressWarnings("deprecation")
public class CustomYuvSource {
    private static final String TAG = "VideoSource";

    public static int SOURCE_PORTRAIT = 1;
    public static int source_LANDSCAPE = 2;

    protected VideoParameters mParameters = null;
    protected int  mRotateDegree = 0;;

    private Camera mCamera = null;
    private SPSurfaceView mSurfaceView = null;
    private SurfaceHolder.Callback mSurfaceHolderCallback = null;
    private static boolean mSurfaceReady = false;
    private CameraEventsHandler eventsHandler;

    public CustomYuvSource(int videoWidth, int videoHeight) {
        mParameters = new VideoParameters();
        mParameters.video_width = videoWidth;
        mParameters.video_height = videoHeight;
        mParameters.cameraid = getCameraId();
    }

    Camera.PreviewCallback callback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if(mRotateDegree == 90) {
                data = rotateYUVDegree90(data, mParameters.video_width, mParameters.video_height);
            } else if(mRotateDegree == 270) {
                data = rotateYUVDegree270(data, mParameters.video_width, mParameters.video_height);
            }
            SPManager.pushYuvFrame(ImageFormat.NV21, data, 0, data.length, System.nanoTime() / 1000);
            camera.addCallbackBuffer(data);
        }
    };

    public void setCameraId(int cameraId) {
        mParameters.cameraid = cameraId;
    }

    public void setVideoResolution(int width, int height) {
        mParameters.video_width = width;
        mParameters.video_height = height;
    }

    public synchronized void setDisplayPreview(SPSurfaceView view) {
        mSurfaceView = view;
        if (mSurfaceHolderCallback != null && mSurfaceView != null && mSurfaceView.getHolder() != null) {
            mSurfaceView.getHolder().removeCallback(mSurfaceHolderCallback);
        }
        if (mSurfaceView.getHolder() != null) {
            mSurfaceHolderCallback = new SurfaceHolder.Callback() {
                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mSurfaceReady = false;
                    // stop();
                    close();
                    ALog.d(TAG, "Surface destroyed !");
                }

                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    ALog.d(TAG, "Surface created !");
                    if (!mSurfaceReady && holder != null) {
                        ALog.d(TAG, "Surface created mSurfaceReady is false !");
                        mSurfaceReady = true;

                        open();
                        start();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    ALog.d(TAG, "Surface Changed !");
                }
            };
            mSurfaceView.getHolder().addCallback(mSurfaceHolderCallback);
        }
    }

    public synchronized void open() {
        if (mSurfaceView == null) {
            ALog.e(TAG, "Invalid SurfaceView !");
            return;
        }
        if(!mSurfaceReady) {
            ALog.e(TAG, "SurfaceView is not ready, try late !");
            return;
        }
        if (mParameters == null) {
            ALog.e(TAG, "Invalid Parameters !");
            return;
        }
        if (mCamera != null) {
            ALog.e(TAG, "Reopen Camera !");
            return;
        }

        if(mParameters.cameraid < 0) {
            if (eventsHandler != null) {
                eventsHandler.onCameraError("invalid camera id :" + mParameters.cameraid);
            }
            return;
        }

        try {
            if (eventsHandler != null) {
                eventsHandler.onCameraOpening(mParameters.cameraid);
            }
            mCamera = Camera.open(mParameters.cameraid);
            mCamera.setPreviewDisplay(mSurfaceView.getHolder());
        } catch (Exception e) {
            ALog.e(TAG, "Exception : ", e);
            if (eventsHandler != null) {
                eventsHandler.onCameraError("Camera launch failed");
            }
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }
            return;
        }

        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mParameters.video_width, mParameters.video_height);
        parameters.setPreviewFormat(ImageFormat.NV21);

        int[] max = getFps(parameters);
        parameters.setPreviewFpsRange(max[0], max[1]);

        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            ALog.d(TAG, "FOCUS_MODE_CONTINUOUS_VIDEO");
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else {
            String focusMode = parameters.getFocusMode();
            ALog.d(TAG, "focusMode " + focusMode);
            if (focusMode == Camera.Parameters.FOCUS_MODE_AUTO || focusMode == Camera.Parameters.FOCUS_MODE_MACRO) {
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        ALog.d(TAG, "onAutoFocus " + success);
                    }
                });
            }
        }
        List localList = parameters.getSupportedAntibanding(); // auto 50hz 60hz
        if(localList != null) {
            if(localList.contains(Camera.Parameters.ANTIBANDING_50HZ)) {
                parameters.setAntibanding(Camera.Parameters.ANTIBANDING_50HZ);
            }else if(localList.contains(Camera.Parameters.ANTIBANDING_AUTO)) {
                parameters.setAntibanding(Camera.Parameters.ANTIBANDING_AUTO);
            }
        }
        if (parameters.isVideoStabilizationSupported()) {
            parameters.setVideoStabilization(true);
        }
        mCamera.setParameters(parameters);

        int rotation_temp = mRotateDegree = getRotation();

        if (mParameters.cameraid == CameraInfo.CAMERA_FACING_FRONT) {
            // compensate the mirror
            rotation_temp = (360 - mRotateDegree) % 360;
        } else {
            rotation_temp = mRotateDegree;
        }
        mCamera.setDisplayOrientation(rotation_temp);
        ALog.d(TAG, "open done !");
    }

    public synchronized void close() {
        if (mCamera == null) {
            ALog.w(TAG, "Invalid Camera !");
            return;
        }
        stop();

        ALog.d(TAG, "close done !");
    }

    public synchronized void start() {
        if (mCamera == null) {
            return;
        }

        for (int i = 0; i < 2; i++) {
            mCamera.addCallbackBuffer(new byte[mParameters.video_width * mParameters.video_height * 3 / 2]);
        }
        mCamera.setPreviewCallbackWithBuffer(callback);
        try {
            mCamera.startPreview();
        } catch (Exception e) {
            ALog.e(TAG, "Exception : ", e);
        }

        ALog.d(TAG, "start done !");
    }

    public synchronized void stop() {
        ALog.d(TAG, "stop ...");
        if (mCamera != null) {
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        ALog.d(TAG, "stop done !");
    }

    private static int[] getFps(Camera.Parameters parameters) {
        int[] maxFps = new int[] { 0, 0 };
        String supportedFpsRangesStr = "Supported frame rates: ";
        List<int[]> supportedFpsRanges = parameters.getSupportedPreviewFpsRange();
        for (Iterator<int[]> it = supportedFpsRanges.iterator(); it.hasNext();) {
            int[] interval = it.next();
            supportedFpsRangesStr += interval[0] / 1000 + "-" + interval[1] / 1000 + "fps" + (it.hasNext() ? ", " : "");
            if (interval[1] > maxFps[1] || (interval[0] > maxFps[0] && interval[1] == maxFps[1])) {
                maxFps = interval;
            }
        }
        ALog.d(TAG, supportedFpsRangesStr);
        return maxFps;
    }

    public int getRotation() {
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(mParameters.cameraid, info);
        Display display = ((WindowManager) mSurfaceView.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
        case Surface.ROTATION_0:
            degrees = 0;
            break;
        case Surface.ROTATION_90:
            degrees = 90;
            break;
        case Surface.ROTATION_180:
            degrees = 180;
            break;
        case Surface.ROTATION_270:
            degrees = 270;
            break;
        }

        int result;
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        ALog.d(TAG, "getRotation rotation: " + rotation + ", degrees: " + degrees + ", result: " + result + ", info.orientation: " + info.orientation + ", info.facing: " + info.facing);
        return result;
    }

    public static  Camera.Size getPreviewSize(Context context, Camera camera) {
        if (camera == null || context == null)
            return null;

        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point screenSize = new Point();
        display.getSize(screenSize);
        float screenRatio = (float)screenSize.x / screenSize.y;
        if(screenSize.x < screenSize.y) {
            screenRatio = (float)screenSize.y / screenSize.x;
        }
        screenRatio = Math.round(screenRatio * 100) / 100;//小数点后两位
        ALog.i(TAG, "screen width: " + screenSize.x + ", height: " + screenSize.y + ", ratio :" + screenRatio);
        //获取当前环境下最佳的camera size
        List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
        Camera.Size optimalSize = null;
        float optimalRatio = 0.0f;
        float supportRatio = 0.0f;
        for (Camera.Size size : sizes) {
            supportRatio = size.width / size.height;
            supportRatio = Math.round(optimalRatio * 100) / 100;//小数点后两位
            if(optimalSize == null) {
                optimalSize = size;
                optimalRatio = supportRatio;
                continue;
            }
            float diff = Math.abs(supportRatio - screenRatio) - Math.abs(optimalRatio - screenRatio);
            if(diff < 0 || (diff == 0.0f && (size.height > 360 && size.height < optimalSize.height || size.height <= 360 && size.height > optimalSize.height))) {
                optimalSize = size;
                optimalRatio = supportRatio;
            }
        }
        Log.i(TAG, "preview width: " + optimalSize.width + ", height: " + optimalSize.height);
        return optimalSize;
    }

    public static int getCameraId(){
        int cameraNum = Camera.getNumberOfCameras();
        int availableId = -1;
        if(cameraNum == 1) {
            availableId = CameraInfo.CAMERA_FACING_BACK;
        } else if (cameraNum > 1){
            availableId = CameraInfo.CAMERA_FACING_FRONT;
        }
        return availableId;
    }

    private byte[] rotateYUVDegree270(byte[] data, int imageWidth, int imageHeight){
        byte[] yuv =new byte[imageWidth*imageHeight*3/2];
        // Rotate the Y luma
        int i =0;
        for(int x = imageWidth-1;x >=0;x--){
            for(int y =0;y < imageHeight;y++){
                        yuv[i]= data[y*imageWidth+x];
                        i++;
            }
        }// Rotate the U and V color components
        i = imageWidth*imageHeight;
        for(int x = imageWidth-1;x >0;x=x-2){
            for(int y =0;y < imageHeight/2;y++){
                yuv[i]= data[(imageWidth*imageHeight)+(y*imageWidth)+(x-1)];
                i++;
                yuv[i]= data[(imageWidth*imageHeight)+(y*imageWidth)+x];
                i++;
            }
        }
        return yuv;
    }

    private byte[] rotateYUVDegree90(byte[] data, int imageWidth, int imageHeight)
    {
        byte [] yuv = new byte[imageWidth*imageHeight*3/2];
        // Rotate the Y luma
        int i = 0;
        for(int x = 0;x < imageWidth;x++)
        {
            for(int y = imageHeight-1;y >= 0;y--)
            {
                yuv[i] = data[y*imageWidth+x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth*imageHeight*3/2-1;
        for(int x = imageWidth-1;x > 0;x=x-2)
        {
            for(int y = 0;y < imageHeight/2;y++)
            {
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+x];
                i--;
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+(x-1)];
                i--;
            }
        }
        return yuv;
    }

    public interface DataListener {
        void onData(byte[] data);
    }

    public static interface CameraEventsHandler {

        void onCameraError(String errorDescription);

        void onCameraOpening(int cameraId);

        void onCameraOpened(int cameraId);

        void onCameraClosed();
    }

    private class VideoParameters {
        public int video_width = 0;
        public int video_height = 0;
        public int cameraid = 0;
    }
}
