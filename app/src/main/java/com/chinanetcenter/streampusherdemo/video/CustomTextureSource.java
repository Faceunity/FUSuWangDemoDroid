package com.chinanetcenter.streampusherdemo.video;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.view.SurfaceHolder;

import com.chinanetcenter.StreamPusher.sdk.SPManager;
import com.chinanetcenter.StreamPusher.utils.ALog;
import com.chinanetcenter.streampusherdemo.opengl.OpenglUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class CustomTextureSource {
    private static final String TAG = "CustomTextureSource";

    private static final String VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            "uniform mat4 vMatrix;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "\n" +
            "varying vec2 textureCoordinate;\n" +
            "\n" +
            "void main(){\n" +
            "    gl_Position=vMatrix*position;\n" +
            "    textureCoordinate=inputTextureCoordinate.xy;\n" +
            "}";

    private static final String FRAGMENT_SHADER = "precision mediump float;"
            + "varying highp vec2 textureCoordinate;\n"
            + "uniform sampler2D inputImageTexture;\n" + "\n"
            + "void main()\n" + "{\n"
            + "gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n"
            + "}";

    private static final String TIME_FORMAT = "HH:mm:ss";
    private static final float TEXT_SIZE_TIME = 64.0f;
    private static final float TEXT_SIZE_FRAM_RATE = 24.0f;
    private static final int FRAME_RATE = 35;

    private static final int MSG_REQUEST_RENDER = 0;
    private static final int MSG_UPDATE_BITMAP = 1;

    protected int mVideoWidth, mVideoHeight;
    protected int mSurfaceWidth, mSurfaceHeight;

    //for opengl
    private boolean mEGLCreated = false;
    private GLSurfaceView mSurfaceView = null;
    private SurfaceHolder.Callback mSurfaceHolderCallback = null;
    private int[] mTimeTextureID = new int[]{OpenglUtil.NO_TEXTURE};
    private float[] mViewMatrix = new float[16];//模型视图矩阵
    private float[] mProjectMatrix = new float[16];//投影矩阵
    private float[] mMVPMatrix = new float[16];//变换矩阵
    private int mProgramId;
    private int mGLAttribPosition;
    private int mGLUniformMatrix;
    private int mGLUniformTexture;
    private int mGLAttribTextureCoordinate;
    private final FloatBuffer mVertexBuffer;
    private final FloatBuffer mTextureBuffer;

    //for bitmap
    private Bitmap mBitmap;
    private boolean mBitmapUpdated;
    private Canvas mCanvas;
    private Paint mPaint;
    private SimpleDateFormat mSimpleDateFormat;
    private int mFrameRate = 0;

    //for update
    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null;
    private Handler.Callback mHandlerCallback = null;
    private volatile int mFrameCount = 0;
    private long mLastUpdateTime = 0;



    public CustomTextureSource(int videoWidth, int videoHeight) {
        mVideoWidth = videoWidth;
        mVideoHeight = videoHeight;
        mVertexBuffer = ByteBuffer.allocateDirect(OpenglUtil.CUBE.length * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mVertexBuffer.put(OpenglUtil.CUBE).position(0);

        mTextureBuffer = ByteBuffer.allocateDirect(OpenglUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureBuffer.put(OpenglUtil.TEXTURE_NO_ROTATION).position(0);
        mHandlerCallback = new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_REQUEST_RENDER:
                        if(mSurfaceView != null) {
                            mSurfaceView.requestRender();
                        }
                        mHandler.sendEmptyMessageDelayed(MSG_REQUEST_RENDER, 1000 / FRAME_RATE);
                        break;
                    case MSG_UPDATE_BITMAP:
                        long currentTime = SystemClock.elapsedRealtime();
                        mFrameRate = (int) (mFrameCount * 1000 / (currentTime - mLastUpdateTime));
                        mLastUpdateTime = currentTime;
                        mFrameCount = 0;//重新开始统计

                        refreshBitmap();
                        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BITMAP, 500);
                        break;
                    default:
                        break;
                }
                return false;
            }
        };
    }

    public synchronized void setDisplayPreview(final GLSurfaceView glSurfaceView) {
        if (glSurfaceView == null) return;
        mSurfaceView = glSurfaceView;
        if (mSurfaceHolderCallback != null && mSurfaceView.getHolder() != null) {
            mSurfaceView.getHolder().removeCallback(mSurfaceHolderCallback);
        }
        if (mSurfaceView.getHolder() != null) {
            mSurfaceHolderCallback = new SurfaceHolder.Callback() {
                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    //这里只是借助SurfaceView生命周期回调释放资源环境，实际开发中不建议这么做
                    ALog.d(TAG, "Surface destroyed !");
                    //释放资源opengl资源要在绘制线程中进行，所以使用GLSurfaceView的queueEvent方法，
                    glSurfaceView.queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            //释放opengl资源
                            ALog.d(TAG, "opengl environment destroyed !");
                            GLES20.glDeleteProgram(mProgramId);
                            GLES20.glDeleteTextures(1, mTimeTextureID, 0);
                            mTimeTextureID[0] = OpenglUtil.NO_TEXTURE;
                            mEGLCreated = false;
                            //释放线程
                            if (mHandlerThread != null) {
                                mHandlerThread.quit();
                                try {
                                    mHandlerThread.join(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                mHandlerThread = null;
                            }
                            //释放图片资源
                            mBitmapUpdated = false;
                            if(mBitmap != null) {
                                mBitmap.recycle();
                                mBitmap = null;
                            }
                        }
                    });

                }

                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    ALog.d(TAG, "Surface created !");
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    ALog.d(TAG, "Surface Changed !");
                }
            };
            mSurfaceView.getHolder().addCallback(mSurfaceHolderCallback);
        }
        //SPManager.pushTextureFrame方法只支持EGL14上下文环境，故这里EGL版本号设置为2
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(new CustomRender());
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private void refreshBitmap() {
        if (mBitmap == null) {
            mBitmap = Bitmap.createBitmap(mVideoWidth, mVideoHeight, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            mPaint = new Paint();
            mPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, 1));
            mPaint.setAntiAlias(true);
            mPaint.setDither(true);
            mPaint.setFilterBitmap(true);
        }
        if (mSimpleDateFormat == null) {
            mSimpleDateFormat = new SimpleDateFormat(TIME_FORMAT);
        }
        mPaint.setColor(Color.BLACK);
        mPaint.setTextSize(TEXT_SIZE_TIME);
        String timeStr = mSimpleDateFormat.format(new Date(System.currentTimeMillis()));
        float textWidth = mPaint.measureText(timeStr);
        mCanvas.drawColor(Color.LTGRAY);
        mCanvas.drawText(timeStr, (mVideoWidth - textWidth) / 2, mVideoHeight / 2, mPaint);
        mPaint.setColor(Color.GRAY);
        mPaint.setTextSize(TEXT_SIZE_FRAM_RATE);
        String frameRateStr = "绘制帧率：" + String.format("%02d",mFrameRate);
        textWidth = mPaint.measureText(frameRateStr);
        mCanvas.drawText(frameRateStr, (mVideoWidth - textWidth) / 2, mVideoHeight / 4, mPaint);
        mBitmapUpdated = true;
    }


    private class CustomRender implements GLSurfaceView.Renderer {
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glClearColor(0, 0, 0, 1.0f);
            mSurfaceWidth = width;
            mSurfaceHeight = height;

            float videoRatio = mVideoWidth / (float) mVideoHeight;
            float surfaceRatio = mSurfaceWidth / (float) mSurfaceHeight;
            if (mSurfaceWidth > mSurfaceHeight) {
                if (videoRatio > surfaceRatio) {
                    Matrix.orthoM(mProjectMatrix, 0, -surfaceRatio * videoRatio, surfaceRatio * videoRatio, -1, 1, 3, 5);
                } else {
                    Matrix.orthoM(mProjectMatrix, 0, -surfaceRatio / videoRatio, surfaceRatio / videoRatio, -1, 1, 3, 5);
                }
            } else {
                if (videoRatio > surfaceRatio) {
                    Matrix.orthoM(mProjectMatrix, 0, -1, 1, -1 / surfaceRatio * videoRatio, 1 / surfaceRatio * videoRatio, 3, 5);
                } else {
                    Matrix.orthoM(mProjectMatrix, 0, -1, 1, -videoRatio / surfaceRatio, videoRatio / surfaceRatio, 3, 5);
                }
            }
            //设置相机位置
            Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 5.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
            //计算变换矩阵
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0);

        }

        @Override
        public void onDrawFrame(GL10 gl) {
            /**
             * 初始化环境，正常情况下应该在onSurfaceCreated回调方法中进行，
             * 这里为了减少MainActivity的代码量，环境的销毁放在了SurfaceHolder.Callback的surfaceDestroyed方法中，
             * 这样资源的释放可以由Surface的生命周期控制。
             */
            if(!mEGLCreated) {
                //初始化异步线程，不断刷新纹理贴图和界面绘制
                if (mHandlerThread == null) {
                    mHandlerThread = new HandlerThread(TAG);
                    mHandlerThread.start();
                    mHandler = new Handler(mHandlerThread.getLooper(), mHandlerCallback);
                    mHandler.sendEmptyMessage(MSG_UPDATE_BITMAP);
                    mHandler.sendEmptyMessage(MSG_REQUEST_RENDER);
                    ALog.d(TAG, "HandlerThread start !");

                }

                //初始化Opengl环境
                GLES20.glClearColor(0, 0, 0, 1.0f);
                mProgramId = OpenglUtil.loadProgram(VERTEX_SHADER, FRAGMENT_SHADER);
                mGLAttribPosition = GLES20.glGetAttribLocation(mProgramId, "position");
                mGLUniformMatrix = GLES20.glGetUniformLocation(mProgramId, "vMatrix");
                mGLUniformTexture = GLES20.glGetUniformLocation(mProgramId, "inputImageTexture");
                mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mProgramId, "inputTextureCoordinate");
                mEGLCreated = true;
            }

            if(mBitmapUpdated) {//图片更新后重新加载贴图
                mTimeTextureID = OpenglUtil.loadTexture(mBitmap, mTimeTextureID[0], false);
                mBitmapUpdated = false;
            }
            //注意：SPManager.pushTextureFrame内部会使用opengl环境，故调用完该方法后应重新设置opengl环境，以防止内部环境影响到外部绘制
            SPManager.pushTextureFrame(mTimeTextureID[0], 0, System.nanoTime() / 1000);
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            //注意：SPManager.pushTextureFrame内部会将视口设置为mVideoWidth和mVideoHeight；
            //此处是将纹理绘制到界面上，故视口的尺寸应该为mSurfaceWidth和mSurfaceHeight
            GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
            GLES20.glUseProgram(mProgramId);
            GLES20.glUniformMatrix4fv(mGLUniformMatrix, 1, false, mMVPMatrix, 0);
            //加载顶点矩阵
            mVertexBuffer.position(0);
            GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
            GLES20.glEnableVertexAttribArray(mGLAttribPosition);
            //加载纹理矩阵
            mTextureBuffer.position(0);
            GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);
            GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
            //激活纹理
            if (mTimeTextureID[0] != OpenglUtil.NO_TEXTURE) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTimeTextureID[0]);
                GLES20.glUniform1i(mGLUniformTexture, 0);
            }
            //纹理贴图
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLES20.glDisableVertexAttribArray(mGLAttribPosition);
            GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

            mFrameCount++;//用于统计绘制帧率
        }
    }


}
