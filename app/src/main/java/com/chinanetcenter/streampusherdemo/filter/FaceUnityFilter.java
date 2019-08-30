package com.chinanetcenter.streampusherdemo.filter;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;

import com.chinanetcenter.StreamPusher.sdk.SPVideoFilter;
import com.faceunity.beautycontrolview.FURenderer;

import java.nio.FloatBuffer;

/**
 * Created by hyj on 2018/11/1.
 */

public class FaceUnityFilter extends SPVideoFilter {
    private static final String TAG = "FUVideoFilterFactory";
    private FURenderer mFURenderer;
    private int cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private int width, height;
    private Context context;

    public FaceUnityFilter(Context context, FURenderer mFURenderer) {
        super(null, null);
        this.context = context;
        this.mFURenderer = mFURenderer;
    }

    @Override
    protected void onInit() {
        super.onInit();
        mFURenderer.onSurfaceCreated();
    }

    //调整过相机预览的宽高
    @Override
    public void onOutputSizeChanged(int width, int height) {
        Log.d(TAG, width + "--" + height);
        this.width = width;
        this.height = height;
        super.onOutputSizeChanged(width, height);
    }

    @Override
    public int onDrawFrame(int tId, FloatBuffer floatBuffer, FloatBuffer floatBuffer1) {
        Log.d("onDrawFrame3:", "cameraId=" + cameraId + "--tid=" + tId);
        tId = mFURenderer.onDrawFrameSingleInputTex(tId, width, height);
        return super.onDrawFrame(tId, floatBuffer, floatBuffer1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFURenderer.onSurfaceDestroyed();
    }

//    @Override
//    public void onCameraPreviewFrameCaptured(byte[] data, int width, int height, int cameraId, int rotation, long timeStamp) {
////        this.data = data;
////        this.cameraId = cameraId;
////        Log.d("onCameraPreview", "width=" + width + "--height=" + height + "--rotation=" + rotation);
//    }

    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
        mFURenderer.setCurrentCameraType(cameraId);
    }

}
