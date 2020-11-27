package com.chinanetcenter.streampusherdemo.sticker;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.chinanetcenter.StreamPusher.sdk.SPStickerController;
import com.chinanetcenter.StreamPusher.utils.ALog;

import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.Nullable;

/**
 * 一个实现了在图像中添加简单图片贴图的类<br/>
 * <pre>
 * 使用方法：
 * MoveStickerController stickerObject = new MoveStickerController(this);
 * stickerObject.x = 0.85f;//设置x坐标
 * stickerObject.y = 0.0f;//设置y坐标
 * stickerObject.width = stickerObject.height = 0.15f;//设置宽高
 * stickerObject.setDataSource(R.drawable.ic_launcher);//设置图片源
 * stickerObject.zOrder = 3;//设置渲染顺序
 * mChatManager.addSticker(stickerObject);/
 *
 * 该类使用Glide加载图片资源，
 * 通过onResourceReady回调确认图片加载成功，并更新到界面中。
 *
 * 移动的实现方式是sdk每次回调onDrawSticker方法，我们在onDrawSticker方法中修改x和y的值，达到移动的效果。
 * </pre>
 */
public class MoveStickerController extends SPStickerController implements RequestListener<Bitmap> {
    private boolean mIsFirstFrame = true;
    private boolean mBmpUpdated = false;
    private boolean mPreviewBmpUpdated = false;
    private boolean mMovable = false;
    /**
     * 控制图像移动速度，200代表200帧一个循环
     */
    private int loopCount = 200;
    private Context mAppContext;
    private RequestBuilder<Bitmap> mDrawableTypeRequest = null;
    private Target mTarget;
    private int mDrawWidth,mDrawHeight;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private AtomicInteger mControllerRefCount = new AtomicInteger();
    private DrawStickerType mPreStickerType = null;
    private int mTypeDoubleMirror = -1;//-1未初始化，0 不镜像，1镜像

    public MoveStickerController(Context context) {
        mAppContext = context.getApplicationContext();
        mControllerRefCount.set(0);
    }

    /**
     * 设置图片资源的id
     * @param resourceId
     */
    public void setDataSource(int resourceId) {
        mDrawableTypeRequest = Glide.with(mAppContext).asBitmap().load(resourceId);
    }

    /**
     * 设置图片资源的加载url
     * @param url
     */
    public void setDataSource(String url) {
        mDrawableTypeRequest = Glide.with(mAppContext).asBitmap().load(url);
    }

    @Override
    public void onInit(boolean mirror) {
        if(mControllerRefCount.getAndIncrement() > 0) return;
        mIsFirstFrame = true;
        mPreStickerType = null;
        mTypeDoubleMirror = -1;

    }

    @Override
    public boolean onDrawSticker(int drawWidth, int drawHeight, boolean mirror, DrawStickerType type) {
        if(mIsFirstFrame) {
            mDrawWidth = drawWidth;
            mDrawHeight = drawHeight;
            if(mDrawableTypeRequest != null) {
                mTarget = mDrawableTypeRequest.diskCacheStrategy(DiskCacheStrategy.ALL).listener(this).fitCenter().submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
                mIsFirstFrame = false;
                return false;
            } else if(stickerBmp != null) {
                mBmpUpdated = true;
                mPreviewBmpUpdated = true;
            }
            updateStickerBmpAndSize();
            mIsFirstFrame = false;
            if(mMovable) {//左上角开始运动
                this.x = -this.width;
            }
            mPreStickerType = type;
        } else if(checkSceneChanged(mirror, type)){
            ALog.i("MoveStickerController", "DrawSticker scene changed");
            mPreStickerType = type;
            mBmpUpdated = true;
            mPreviewBmpUpdated = true;
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
        if (mTarget != null) {
            final Target target = mTarget;
            mTarget = null;
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(target != null) {
                        Glide.with(mAppContext).clear(target);
                    }
                }
            });
        }
    }

    /**
     * 设置是否自动移动，默认实现从左上角平移到右上角
     * @param movable
     */
    public void setMovable(boolean movable) {
        mMovable = movable;
    }

    /**
     * Glide加载图像失败后回调此方法
     * @param e
     * @param model
     * @param target
     * @param isFirstResource
     * @return
     */
    @Override
    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
        stickerBmp = null;
        mBmpUpdated = true;
        mPreviewBmpUpdated = true;
        return false;
    }

    /**
     * Glide加载图片资源成功后回调此方法
     * @param resource
     * @param model
     * @param target
     * @param dataSource
     * @param isFirstResource
     * @return
     */
    @Override
    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
        stickerBmp = resource;
        mBmpUpdated = true;
        mPreviewBmpUpdated = true;
        updateStickerBmpAndSize();
        return false;
    }

    /**
     * 根据解码的图像和用户设置的宽高计算一个实际不拉伸图像的宽高
     */
    private void updateStickerBmpAndSize() {
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

    private boolean checkSceneChanged(boolean mirror, DrawStickerType type) {
        if(mPreStickerType == null) {
            return true;
        }
        if(type == DrawStickerType.DOUBLE || mPreStickerType == DrawStickerType.DOUBLE ) {
            if(mPreStickerType == type) {
                int newMirror = mirror ? 1 : 0;
                if(mTypeDoubleMirror != newMirror) {
                    mTypeDoubleMirror = newMirror;
                    return true;
                }else {
                    return false;
                }
            }else {
                return true;
            }
        }else {
            return false;
        }
    }
}
