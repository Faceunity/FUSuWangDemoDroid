package com.chinanetcenter.streampusherdemo.sticker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.bumptech.glide.DrawableTypeRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.chinanetcenter.StreamPusher.sdk.SPStickerController;
import com.chinanetcenter.StreamPusher.utils.ALog;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 向画面中提供一个gif图像的控制器<br/>
 * <pre>
 * 使用方法：
 *  GifStickerController gifStickerObject = new GifStickerController(this);
 *  gifStickerObject.x = 0.1f;//设置显示的x坐标
 *  gifStickerObject.y = 0.4f;//设置显示的y坐标
 *  gifStickerObject.width = 0.8f;//设置宽度
 *  gifStickerObject.height = 0.6f;//设置高度
 *  gifStickerObject.setDataSource(R.raw.fireworks);//设置gif资源文件
 *  gifStickerObject.setLoopCount(GifStickerController.LOOP_FOREVER);//设置播放循环次数
 *  gifStickerObject.zOrder = 5;//显示顺序
 *  mLinkMicManager.addSticker(gifStickerObject);
 *
 *
 * 该类使用Glide加载并解析gif资源，
 * 通过onResourceReady回调确认gif加载成功；
 * 通过invalidateDrawable不断更新图片资源。
 * </pre>
 */
public class GifStickerController extends SPStickerController implements RequestListener<Object, GifDrawable>, Drawable.Callback {

    private static final String TAG = "GifStickerController";
    /**
     * 无限循环
     */
    public static final int LOOP_FOREVER = GlideDrawable.LOOP_FOREVER;
    /**
     * gif内置播放次数
     */
    public static final int LOOP_INTRINSIC = GlideDrawable.LOOP_INTRINSIC;
    private boolean mIsFirstFrame = true;
    private boolean mBmpUpdated = false;
    private boolean mPreviewBmpUpdated = false;
    private Context mAppContext;
    private DrawableTypeRequest mDrawableTypeRequest = null;
    private Target mTarget;
    private int mDrawWidth,mDrawHeight;
    private int mLoopCount = 0;
    private int mMaxLoopCount = LOOP_FOREVER;
    private GifDrawable mGifDrawable = null;
    private GifListener mGifFinishListener = null;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    /**
     * 配合复写{@link #getSticker(boolean, SPStickerController.DrawStickerType)}，缓存一张图片，防止因不停创建图片导致不断gc
     */
    private Queue<Bitmap> mDecodeBitmapPool = new LinkedList<>();
    private boolean mBitmapPoolCleared = true;
    private AtomicInteger mControllerRefCount = new AtomicInteger();

    public GifStickerController(Context context) {
        mAppContext = context.getApplicationContext();
        mControllerRefCount.set(0);
    }

    /**
     * 设置数据源，该数据源会在第一帧画面到来时加载
     * @param resourceId
     */
    public void setDataSource(int resourceId) {
        mDrawableTypeRequest = Glide.with(mAppContext).load(resourceId);
    }

    /**
     * 设置数据源，该数据源会在第一帧画面到来时加载
     * @param url
     */
    public void setDataSource(String url) {
        mDrawableTypeRequest = Glide.with(mAppContext).load(url);
    }

    /**
     * 设置gif循环播放次数<br/>
     * {@link #LOOP_FOREVER}无限循环<br/>
     * {@link #LOOP_INTRINSIC}gif文件内置循环次数<br/>
     * 如果gif设置循环次数不是{@link #LOOP_FOREVER}，请调用{@link #setGifListener(GifListener)}注册回调<br/>
     * 并在回调方法中调用{@link com.chinanetcenter.StreamPusher.sdk.SPManager#removeSticker(SPStickerController)}及时将此控制器移除。<br/>
     * @param loopCount
     */
    public void setLoopCount(int loopCount) {
        mMaxLoopCount = loopCount;
    }

    public void setGifListener(GifListener listener) {
        mGifFinishListener = listener;
    }

    @Override
    public void onInit(boolean mirror) {
        if(mControllerRefCount.getAndIncrement() > 0) return;
        mIsFirstFrame = true;
        ALog.i(TAG,"onInit ... " + this);

    }

    @Override
    public boolean onDrawSticker(int drawWidth, int drawHeight, boolean mirror, DrawStickerType type) {
        if(mIsFirstFrame) {
            synchronized (mDecodeBitmapPool) {
                mBitmapPoolCleared = false;
            }
            mDrawWidth = drawWidth;
            mDrawHeight = drawHeight;
            if(mDrawableTypeRequest != null) {
                mTarget = mDrawableTypeRequest.asGif().skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.NONE).dontAnimate().listener(this).into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
                mIsFirstFrame = false;
                return false;
            } else if(stickerBmp != null) {
                mBmpUpdated = true;
                mPreviewBmpUpdated = true;
                updateStickerBmpAndSize();
            }
            mIsFirstFrame = false;
        }
        boolean result;
        if(type == DrawStickerType.PRIVIEW) {
            result = mPreviewBmpUpdated;
            mPreviewBmpUpdated = false;

        } else {
            result = mBmpUpdated;
            mBmpUpdated = false;
        }
        return result;
    }

    @Override
    public void onDestroy(boolean mirror) {
        if(mControllerRefCount.decrementAndGet() > 0) return;
        mControllerRefCount.set(0);
        ALog.i(TAG,"onDestroy ... " + this);
        mIsFirstFrame = false;
        mBmpUpdated = false;
        mPreviewBmpUpdated = false;
        //清空缓存图像
        synchronized (mDecodeBitmapPool) {
            for(Bitmap bitmap : mDecodeBitmapPool) {
                bitmap.recycle();
            }
            mDecodeBitmapPool.clear();
            mBitmapPoolCleared = true;
            stickerBmp = null;
        }
        if (mTarget != null) {
            final Target target = mTarget;
            mTarget = null;
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mGifDrawable != null) {
                        mGifDrawable.stop();
                        mGifDrawable.recycle();
                    }
                    Glide.clear(target);
                    try {
                        //释放gif占用的缓存空间
                        Glide.get(mAppContext).clearMemory();
                    }catch (Exception e) {
                        ALog.e(TAG, "clear gif memory cache failed!");
                    }

                }
            });
        }
    }

    /**
     * 当sdk内部调用此方法时，证明sdk正在请求下一帧图像，之前的贴图图像必然已经不再需要，故将上一帧的图像重新放回缓冲池；<br/>
     * 由于sdk内部每次都是先绘制非镜像的编码图像，而我们的gif镜像或非镜像为同一张，故只在非镜像的数据请求时更新贴纸图像。<br/>
     * @param mirror
     * @return
     */
    @Override
    public synchronized Bitmap getSticker(boolean mirror, DrawStickerType type){
        if(!mirror) {
            synchronized (mDecodeBitmapPool) {
                if(stickerBmp != null) {
                    mDecodeBitmapPool.offer(stickerBmp);
                }
                stickerBmp = mDecodeBitmapPool.poll();
            }
        }
        return stickerBmp;
    }

    /**
     * 加载图像失败后回调此方法
     * @param e
     * @param model
     * @param target
     * @param isFirstResource
     * @return
     */
    @Override
    public boolean onException(Exception e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
        stickerBmp = null;
        mBmpUpdated = true;
        mPreviewBmpUpdated = true;
        if(mGifFinishListener != null) {
            mGifFinishListener.onGifLoadError(this);
        }
        return false;
    }

    /**
     * gif 资源加载成功回调，在此方法中启动gif播放
     * @param resource
     * @param model
     * @param target
     * @param isFromMemoryCache
     * @param isFirstResource
     * @return
     */
    @Override
    public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
        if(mGifFinishListener != null) {
            mGifFinishListener.onGifLoadSuccess(this);
        }
        //mTarget为null，证明该controller已经被销毁，忽略此次加载结果
        if(mTarget == null) return true;
        //获取首帧图像
        stickerBmp = resource.getFirstFrame();
        //根据首帧图像宽高计算在controller的宽高，保持比例，防止图像被拉伸
        updateStickerBmpAndSize();
        if(resource.getFrameCount() > 1) {
            //首帧不需要在这里设置，invalidateDrawable方法还会绘制首帧
            stickerBmp = null;
            //设置GifDrawable全局引用，以便在onDestroy方法中停止gif播放，释放资源
            mGifDrawable = resource;
            //设置使用gif内置循环播放次数时，循环次数从解码器中获取
            if (mMaxLoopCount == LOOP_INTRINSIC) {
                mMaxLoopCount = resource.getDecoder().getLoopCount();
                //很多gif默认循环次数为0，这里将循环次数改为1，播放一次
                if(mMaxLoopCount == 0) {
                    mMaxLoopCount = 1;
                }
            }
            mLoopCount = 0;
            //设置输出宽高，默认0,0，不输出
            mGifDrawable.setBounds(0, 0, resource.getIntrinsicWidth(), resource.getIntrinsicHeight());
            //设置回调，不设置gif动画会自动停止
            mGifDrawable.setCallback(this);
            //设置gif循环播放次数
            mGifDrawable.setLoopCount(mMaxLoopCount);
            //启动gif动画
            mGifDrawable.start();
        } else {
            //单帧图像的gif这里会一直显示，您可以定义自己的逻辑
            mBmpUpdated = true;
            mPreviewBmpUpdated = true;
        }
        return false;
    }

    /**
     * 根据解码的图像和用户设置的宽高计算一个实际不拉伸图像的宽高
     */
    protected void updateStickerBmpAndSize() {
        if(stickerBmp == null) return;
        int setWidth = (int) (this.width * mDrawWidth);
        int setHeight = (int) (this.height * mDrawHeight);
        int bmpWidth = stickerBmp.getWidth();
        int bmpHeight = stickerBmp.getHeight();

        float aspect = (float) bmpWidth / bmpHeight;
        int fitWidth = Math.min(setWidth, (int)(setHeight * aspect));
        int fitHeight = Math.min(setHeight, (int)(setWidth / aspect));
        this.width = (float) fitWidth / mDrawWidth;
        this.height = (float) fitHeight / mDrawHeight;
    }

    /**
     * Glide每解析一帧gif图像都会回调此方法，我们在此方法中通过{@link Drawable#draw(Canvas)}方法获取解码的图像；<br/>
     * @param dr
     */
    @Override
    public void invalidateDrawable(@NonNull Drawable dr) {
        if (dr != null && dr instanceof GifDrawable) {
            GifDrawable gifDr = (GifDrawable)dr;
            // update cached drawable dimensions if they've changed
            final int w = gifDr.getIntrinsicWidth();
            final int h = gifDr.getIntrinsicHeight();
            synchronized (mDecodeBitmapPool) {
                if(mBitmapPoolCleared) {
                    return;
                }
                //当前贴纸图片为空，说明贴纸数据已经被加载到界面中，此时加载下一张贴纸图片
                Bitmap bitmap = mDecodeBitmapPool.poll();
                boolean needCreateBitmap = false;
                if(bitmap == null) {
                    needCreateBitmap = true;
                } else if(bitmap.getWidth() != w || bitmap.getHeight() != h){
                    bitmap.recycle();
                    needCreateBitmap = true;
                }
                if(needCreateBitmap) {
                    //此处创建房间使用带透明度的ARGB_8888格式，否则SDK内部将图片加载到gpu时将会带黑色背景
                    bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    ALog.i(TAG, "createBitmap w = " + w + " , h = " + h);
                }
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                gifDr.draw(canvas);
                mDecodeBitmapPool.offer(bitmap);
                stickerBmp = bitmap;
                mBmpUpdated = true;
                mPreviewBmpUpdated = true;
            }
            GifDecoder decoder = gifDr.getDecoder();
            if (decoder.getCurrentFrameIndex() == decoder.getFrameCount() - 1) {
                mLoopCount++;
            }
            if (mMaxLoopCount != LOOP_FOREVER && mLoopCount >= mMaxLoopCount) {
                gifDr.stop();
                if(mGifFinishListener != null) {
                    mGifFinishListener.onGifPlayFinish(this);
                }
            }
        }
    }

    /**
     * 不需要实现
     * @param who
     * @param what
     * @param when
     */
    @Override
    public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
    }

    /**
     * 不需要实现
     * @param who
     * @param what
     */
    @Override
    public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
    }

    /**
     * gif解码和播放监听器
     */
    public interface GifListener {
        /**
         * gif图像自动播放结束后回调此方法<br/>
         * 此时界面中gif会显示最后一帧，要在此回调中及时调用{@link com.chinanetcenter.StreamPusher.sdk.SPManager#removeSticker(SPStickerController)}将此控制器移除。<br/>
         * @param gifStickerController
         */
        void onGifPlayFinish(GifStickerController gifStickerController);

        /**
         * gif加载出错后回调
         * @param gifStickerController
         */
        void onGifLoadError(GifStickerController gifStickerController);
        /**
         * gif加载成功后回调
         * @param gifStickerController
         */
        void onGifLoadSuccess(GifStickerController gifStickerController);
    }

}
