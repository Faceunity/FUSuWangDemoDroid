package com.chinanetcenter.streampusherdemo.sticker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.chinanetcenter.StreamPusher.sdk.SPStickerController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 一个实现了在图像中添加时间贴图的类<br/>
 * 本类中默认实现在左上角显示时间<br/>
 * <pre>
 * TimeStickerController stickerObject = new TimeStickerController(this);
 * stickerObject.zOrder = 2;//设置绘制顺序
 * mChatManager.addSticker(stickerObject);
 *
 * 移动的实现方式是sdk每次回调onDrawSticker方法，我们在onDrawSticker方法中更新图片的时间信息。
 * </pre>
 */
public class TimeStickerController extends SPStickerController {
    /**
     * 时间字体的高度
     */
    private static final float TEXT_HEIGHT = 0.05f;
    /**
     * 时间字体的颜色
     */
    private static final int TEXT_COLOR = Color.WHITE;
    /**
     * 时间字符串的格式
     */
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private boolean mIsFirstFrame = true;
    private boolean mBmpUpdated = false;
    private boolean mPreviewBmpUpdated = false;
    private boolean mMovable = false;
    /**
     * 控制图像移动速度，200代表200帧一个循环
     */
    private int loopCount = 200;
    private Context mAppContext;
    private int mDrawWidth,mDrawHeight;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private Paint mTimePaint = null;
    private SimpleDateFormat mDateFormat = new SimpleDateFormat(TIME_FORMAT);
    private long mCurrentTimeSecond;
    private AtomicInteger mControllerRefCount = new AtomicInteger();

    public TimeStickerController(Context context) {
        mAppContext = context.getApplicationContext();
        mControllerRefCount.set(0);
    }


    @Override
    public void onInit(boolean mirror) {
        if(mControllerRefCount.getAndIncrement() > 0) return;
        mIsFirstFrame = true;
    }

    @Override
    public boolean onDrawSticker(int drawWidth, int drawHeight, boolean mirror, DrawStickerType type) {
        mDrawWidth = drawWidth;
        mDrawHeight = drawHeight;
        long newTimeSecond = SystemClock.uptimeMillis() / 1000;
        if(newTimeSecond != mCurrentTimeSecond) {
            prepareStickerBmp();
            mCurrentTimeSecond = newTimeSecond;
            if(mIsFirstFrame) {
                this.width = (float) stickerBmp.getWidth() / mDrawWidth;
                this.height = (float) stickerBmp.getHeight() / mDrawHeight;
                if(mMovable) {//左上角开始运动
                    this.x = -this.width;
                } else {
                    if(mDrawHeight > mDrawWidth) {
                        //竖屏
                        this.x = this.y = 0.02f;
                    } else {
                        //横屏
                        this.x = 0.02f;
                        this.y = 0.01f;
                    }
                }
                mIsFirstFrame = false;
            }
        }
        if(mMovable && type != DrawStickerType.PRIVIEW) {
            float step = (1.0f + 2 * this.width) / loopCount;
            this.x += step;
            if(this.x > 1.0f) {
                this.x = - this.width;
            }
        }
        if(type == DrawStickerType.PRIVIEW) {
            boolean result = mPreviewBmpUpdated;
            mPreviewBmpUpdated = false;
            return result;
        } else {
            boolean result = mBmpUpdated;
            mBmpUpdated = false;
            return result;
        }
    }

    @Override
    public void onDestroy(boolean mirror) {
        if(mControllerRefCount.decrementAndGet() > 0) return;
        mControllerRefCount.set(0);
        mIsFirstFrame = false;
        mBmpUpdated = false;
        mPreviewBmpUpdated = false;
        stickerBmp = null;
    }

    /**
     * 设置是否可以转移
     * @param movable
     */
    public void setMovable(boolean movable) {
        mMovable = movable;
    }

    /**
     * 覆盖父类方法，不支持此方法的调用
     * @param sticker
     */
    @Override
    public synchronized void setSticker(Bitmap sticker) {
        //do nothing
    }

    /**
     * 准备时间图像
     */
    private void prepareStickerBmp() {
        if(mTimePaint == null) {
            //计算水印文字大小
            int textSizePx;
            if(mDrawHeight > mDrawWidth) {
                //竖屏
                textSizePx = (int) (mDrawWidth * TEXT_HEIGHT);
            } else {
                //横屏
                textSizePx = (int) (mDrawHeight * TEXT_HEIGHT);
            }
            mTimePaint = new Paint();
            mTimePaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, 1));
            mTimePaint.setColor(TEXT_COLOR);
            mTimePaint.setTextSize(textSizePx);
            mTimePaint.setAntiAlias(true);
            mTimePaint.setFilterBitmap(true);
        }
        int timeStrWidth = (int) Math.ceil(mTimePaint.measureText(TIME_FORMAT));
        int timeStrHeight = (int) Math.ceil(mTimePaint.getFontMetrics().descent - mTimePaint.getFontMetrics().ascent);
        if(stickerBmp == null || stickerBmp.getWidth() != timeStrWidth || stickerBmp.getHeight() != timeStrHeight) {
            stickerBmp = Bitmap.createBitmap(timeStrWidth, timeStrHeight, Bitmap.Config.ARGB_8888);
        }
        Canvas timeCanvas = new Canvas(stickerBmp);
        timeCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        String timeStr = mDateFormat.format(new Date(System.currentTimeMillis()));
        timeCanvas.drawText(timeStr, 0f, (mTimePaint.getFontMetrics().leading - mTimePaint.getFontMetrics().ascent), mTimePaint);
        mBmpUpdated = true;
        mPreviewBmpUpdated = true;
    }
}
