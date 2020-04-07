package com.chinanetcenter.streampusherdemo.filter;

import android.util.Log;

import com.chinanetcenter.StreamPusher.sdk.SPVideoFilter;
import com.faceunity.nama.FURenderer;

import java.nio.FloatBuffer;

/**
 * Faceunity 美颜贴纸
 *
 * Created by hyj on 2018/11/1.
 */
public class FaceUnityFilter extends SPVideoFilter {
    private static final String TAG = "FUVideoFilterFactory";
    private FURenderer mFURenderer;
    private int mWidth;
    private int mHeight;

    public FaceUnityFilter(FURenderer mFURenderer) {
        super(null, null);
        this.mFURenderer = mFURenderer;
    }

    @Override
    protected void onInit() {
        super.onInit();
        Log.d(TAG, "onInit: ");
        mFURenderer.onSurfaceCreated();
    }

    @Override
    public void onOutputSizeChanged(int width, int height) {
        super.onOutputSizeChanged(width, height);
//        Log.d(TAG, "onOutputSizeChanged() width = [" + width + "], height = [" + height + "]");
        this.mWidth = width;
        this.mHeight = height;
    }

    @Override
    public int onDrawFrame(int texId, FloatBuffer floatBuffer, FloatBuffer floatBuffer1) {
//        Log.v(TAG, "onDrawFrame() called with: texId = [" + texId + "], thread:" + Thread.currentThread().getName() + ", egl:" + EGL14.eglGetCurrentContext());
        int fuTexId = mFURenderer.onDrawFrameSingleInput(texId, mWidth, mHeight);
        return super.onDrawFrame(fuTexId, floatBuffer, floatBuffer1);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        mFURenderer.onSurfaceDestroyed();
    }

}
