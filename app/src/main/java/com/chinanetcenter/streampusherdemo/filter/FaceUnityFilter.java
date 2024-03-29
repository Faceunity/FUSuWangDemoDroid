package com.chinanetcenter.streampusherdemo.filter;

import android.content.Context;
import android.opengl.EGL14;
import android.util.Log;

import com.chinanetcenter.StreamPusher.sdk.SPVideoFilter;
import com.chinanetcenter.streampusherdemo.faceunity.profile.CSVUtils;
import com.chinanetcenter.streampusherdemo.faceunity.profile.Constant;
import com.faceunity.core.faceunity.FUAIKit;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.model.facebeauty.FaceBeautyBlurTypeEnum;
import com.faceunity.nama.FUConfig;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.utils.FuDeviceUtils;

import java.io.File;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
    private CSVUtils mCSVUtils;
    private Context mContext;


    public FaceUnityFilter(Context context, FURenderer mFURenderer) {
        super(null, null);
        this.mFURenderer = mFURenderer;
        mContext = context;
    }

    @Override
    protected void onInit() {
        super.onInit();
        Log.d(TAG, "onInit: ");
        mFURenderer.prepareRenderer();
        initCsvUtil(mContext);
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
        Log.v(TAG, "onDrawFrame() called with: texId = [" + texId + "], thread:" + Thread.currentThread().getName() + ", egl:" + EGL14.eglGetCurrentContext());

        if (FUConfig.DEVICE_LEVEL > FuDeviceUtils.DEVICE_LEVEL_MID) {
            cheekFaceNum();
        }

        long start = System.nanoTime();
        int fuTexId = mFURenderer.onDrawFrameDualInput(null, texId, mWidth, mHeight);
        long time = System.nanoTime() - start;
        mCSVUtils.writeCsv(null, time);
        return super.onDrawFrame(fuTexId, floatBuffer, floatBuffer1);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        mFURenderer.release();
        mCSVUtils.close();
    }

    private void initCsvUtil(Context context) {
        mCSVUtils = new CSVUtils(context);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
        String dateStrDir = format.format(new Date(System.currentTimeMillis()));
        dateStrDir = dateStrDir.replaceAll("-", "").replaceAll("_", "");
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault());
        String dateStrFile = df.format(new Date());
        String filePath = Constant.filePath + dateStrDir + File.separator + "excel-" + dateStrFile + ".csv";
        Log.d(TAG, "initLog: CSV file path:" + filePath);
        StringBuilder headerInfo = new StringBuilder();
        headerInfo.append("version：").append(FURenderer.getInstance().getVersion()).append(CSVUtils.COMMA)
                .append("机型：").append(android.os.Build.MANUFACTURER).append(android.os.Build.MODEL)
                .append("处理方式：Texture").append(CSVUtils.COMMA);
        mCSVUtils.initHeader(filePath, headerInfo);
    }

    private void cheekFaceNum() {
        //根据有无人脸 + 设备性能 判断开启的磨皮类型
        float faceProcessorGetConfidenceScore = FUAIKit.getInstance().getFaceProcessorGetConfidenceScore(0);
        if (faceProcessorGetConfidenceScore >= 0.95) {
            //高端手机并且检测到人脸开启均匀磨皮，人脸点位质
            if (FURenderKit.getInstance().getFaceBeauty() != null && FURenderKit.getInstance().getFaceBeauty().getBlurType() != FaceBeautyBlurTypeEnum.EquallySkin) {
                FURenderKit.getInstance().getFaceBeauty().setBlurType(FaceBeautyBlurTypeEnum.EquallySkin);
                FURenderKit.getInstance().getFaceBeauty().setEnableBlurUseMask(true);
            }
        } else {
            if (FURenderKit.getInstance().getFaceBeauty() != null && FURenderKit.getInstance().getFaceBeauty().getBlurType() != FaceBeautyBlurTypeEnum.FineSkin) {
                FURenderKit.getInstance().getFaceBeauty().setBlurType(FaceBeautyBlurTypeEnum.FineSkin);
                FURenderKit.getInstance().getFaceBeauty().setEnableBlurUseMask(false);
            }
        }
    }

}
