package com.chinanetcenter.streampusherdemo.utils;

import com.chinanetcenter.streampusherdemo.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.support.v4.os.AsyncTaskCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.widget.ImageView;

public class ScreenShotAnimationExcutor {
  private static final String TAG = "ScreenshotAnimationExcutor";

  private static final int SCREENSHOT_FLASH_TO_PEAK_DURATION = 130;
  private static final int SCREENSHOT_DROP_IN_DURATION = 430;
  private static final int SCREENSHOT_DROP_OUT_DELAY = 500;
  private static final int SCREENSHOT_DROP_OUT_DURATION = 430;
  private static final int SCREENSHOT_DROP_OUT_SCALE_DURATION = 370;
  private static final int SCREENSHOT_FAST_DROP_OUT_DURATION = 320;
  private static final float BACKGROUND_ALPHA = 0.5f;
  private static final float SCREENSHOT_SCALE = 1f;
  private static final float SCREENSHOT_DROP_IN_MIN_SCALE = SCREENSHOT_SCALE * 0.725f;
  private static final float SCREENSHOT_DROP_OUT_MIN_SCALE = SCREENSHOT_SCALE * 0.45f;
  private static final float SCREENSHOT_FAST_DROP_OUT_MIN_SCALE = SCREENSHOT_SCALE * 0.6f;
  private static final float SCREENSHOT_DROP_OUT_MIN_SCALE_OFFSET = 0f;

  private Context mContext;
  private WindowManager mWindowManager;
  private WindowManager.LayoutParams mWindowLayoutParams;
  private Display mDisplay;
  private DisplayMetrics mDisplayMetrics;

  private Bitmap mScreenBitmap;
  private View mScreenshotLayout;
  private ImageView mBackgroundView;
  private ImageView mScreenshotView;
  private ImageView mScreenshotFlash;

  private AnimatorSet mScreenshotAnimation;

  private float mBgPadding;
  private float mBgPaddingScale;

  /**
   * @param context everything needs a context :(
   */
  public ScreenShotAnimationExcutor(Context context) {
    Resources r = context.getResources();
    mContext = context;
    LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    mScreenshotLayout = layoutInflater.inflate(R.layout.screenshot, null);
    mBackgroundView = (ImageView) mScreenshotLayout.findViewById(R.id.screenshot_background);
    mScreenshotView = (ImageView) mScreenshotLayout.findViewById(R.id.screenshot);
    mScreenshotFlash = (ImageView) mScreenshotLayout.findViewById(R.id.screenshot_flash);
    mScreenshotLayout.setFocusable(true);
    mScreenshotLayout.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, android.view.MotionEvent event) {
        //拦截所有触摸事件
        return true;
      }
    });

    //初始化布局参数
    mWindowLayoutParams = new WindowManager.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0, 0,
        WindowManager.LayoutParams. TYPE_TOAST,//WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
        WindowManager.LayoutParams.FLAG_FULLSCREEN
            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
        PixelFormat.TRANSLUCENT);
    mWindowLayoutParams.setTitle("ScreenshotAnimation");
    mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

    mDisplay = mWindowManager.getDefaultDisplay();
    mDisplayMetrics = new DisplayMetrics();
    mDisplay.getRealMetrics(mDisplayMetrics);

    mBgPadding = (float) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, mDisplayMetrics);
    mBgPaddingScale = mBgPadding / mDisplayMetrics.widthPixels;

  }

  /**
   * 截屏动画
   * @param bitmap
   */
  public void excuteFullScreenAnimation(Bitmap bitmap) {
    mScreenBitmap = bitmap;

    if (mScreenBitmap == null) {
      return;
    }
    mScreenBitmap.setHasAlpha(false);//设置图片不透明
    mScreenBitmap.prepareToDraw();

    //开始播放截屏动画
    startAnimation(mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels, false, false);
  }


  /**
   * 截屏动画
   */
  private void startAnimation(int w, int h, boolean statusBarVisible,
                              boolean navBarVisible) {
    // 设置图片
    mScreenshotView.setImageBitmap(mScreenBitmap);
    mScreenshotLayout.requestFocus();

    // 以防万一，先清空之前的动画和监听
    if (mScreenshotAnimation != null) {
      mScreenshotAnimation.end();
      mScreenshotAnimation.removeAllListeners();
    }

    mWindowManager.addView(mScreenshotLayout, mWindowLayoutParams);
    ValueAnimator screenshotDropInAnim = createScreenshotDropInAnimation();
    ValueAnimator screenshotFadeOutAnim = createScreenshotDropOutAnimation(w, h, statusBarVisible, navBarVisible);
    mScreenshotAnimation = new AnimatorSet();
    mScreenshotAnimation.playSequentially(screenshotDropInAnim, screenshotFadeOutAnim);//顺序执行动画
    mScreenshotAnimation.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        mWindowManager.removeView(mScreenshotLayout);
        //此处不再使用回调，直接简单处理，保存图片
        BitmapSaveTask saveTask = new BitmapSaveTask(mContext);
        AsyncTaskCompat.executeParallel(saveTask, mScreenBitmap);
        // 清空BitMap引用
        mScreenBitmap = null;
        mScreenshotView.setImageBitmap(null);

      }
    });
    mScreenshotLayout.post(new Runnable() {
      @Override
      public void run() {
        mScreenshotView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mScreenshotView.buildLayer();
        mScreenshotAnimation.start();//启动动画
      }
    });
  }

  private ValueAnimator createScreenshotDropInAnimation() {
    final float flashPeakDurationPct = ((float) (SCREENSHOT_FLASH_TO_PEAK_DURATION) / SCREENSHOT_DROP_IN_DURATION);
    final float flashDurationPct = 2f * flashPeakDurationPct;
    final Interpolator flashAlphaInterpolator = new Interpolator() {
      @Override
      public float getInterpolation(float x) {
        // Flash the flash view in and out quickly
        if (x <= flashDurationPct) {
          return (float) Math.sin(Math.PI * (x / flashDurationPct));
        }
        return 0;
      }
    };
    final Interpolator scaleInterpolator = new Interpolator() {
      @Override
      public float getInterpolation(float x) {
        if (x < flashPeakDurationPct) {
          return 0;
        }
        return (x - flashDurationPct) / (1f - flashDurationPct);
      }
    };
    ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
    anim.setDuration(SCREENSHOT_DROP_IN_DURATION);
    anim.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationStart(Animator animation) {
        mBackgroundView.setAlpha(0f);
        mBackgroundView.setVisibility(View.VISIBLE);
        mScreenshotView.setAlpha(0f);
        mScreenshotView.setTranslationX(0f);
        mScreenshotView.setTranslationY(0f);
        mScreenshotView.setScaleX(SCREENSHOT_SCALE + mBgPaddingScale);
        mScreenshotView.setScaleY(SCREENSHOT_SCALE + mBgPaddingScale);
        mScreenshotView.setVisibility(View.VISIBLE);
        mScreenshotFlash.setAlpha(0f);
        mScreenshotFlash.setVisibility(View.VISIBLE);
      }

      @Override
      public void onAnimationEnd(Animator animation) {
        mScreenshotFlash.setVisibility(View.GONE);

//        mScreenshotView.setScaleX(SCREENSHOT_SCALE);
//        mScreenshotView.setScaleY(SCREENSHOT_SCALE);
      }
    });
    anim.addUpdateListener(new AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        float t = (Float) animation.getAnimatedValue();
        float scaleT = (SCREENSHOT_SCALE + mBgPaddingScale)
            - scaleInterpolator.getInterpolation(t) * (SCREENSHOT_SCALE - SCREENSHOT_DROP_IN_MIN_SCALE);
        mBackgroundView.setAlpha(scaleInterpolator.getInterpolation(t) * BACKGROUND_ALPHA);
        mScreenshotView.setAlpha(t);
        mScreenshotView.setScaleX(scaleT);
        mScreenshotView.setScaleY(scaleT);
        mScreenshotFlash.setAlpha(flashAlphaInterpolator.getInterpolation(t));
      }
    });
    return anim;
  }

  private ValueAnimator createScreenshotDropOutAnimation(int w, int h, boolean statusBarVisible,
                                                         boolean navBarVisible) {
    ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
    anim.setStartDelay(SCREENSHOT_DROP_OUT_DELAY);
    anim.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        mBackgroundView.setVisibility(View.GONE);
        mScreenshotView.setVisibility(View.GONE);
        mScreenshotView.setLayerType(View.LAYER_TYPE_NONE, null);
      }
    });

    if (!statusBarVisible || !navBarVisible) {
      // There is no status bar/nav bar, so just fade the screenshot away in place
      anim.setDuration(SCREENSHOT_FAST_DROP_OUT_DURATION);
      anim.addUpdateListener(new AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
          float t = (Float) animation.getAnimatedValue();
          float scaleT = (SCREENSHOT_DROP_IN_MIN_SCALE + mBgPaddingScale)
              - t * (SCREENSHOT_DROP_IN_MIN_SCALE - SCREENSHOT_FAST_DROP_OUT_MIN_SCALE);
          mBackgroundView.setAlpha((1f - t) * BACKGROUND_ALPHA);
          mScreenshotView.setAlpha(1f - t);
          mScreenshotView.setScaleX(scaleT);
          mScreenshotView.setScaleY(scaleT);
        }
      });
    } else {
      // In the case where there is a status bar, animate to the origin of the bar (top-left)
      final float scaleDurationPct = (float) SCREENSHOT_DROP_OUT_SCALE_DURATION / SCREENSHOT_DROP_OUT_DURATION;
      final Interpolator scaleInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float x) {
          if (x < scaleDurationPct) {
            // Decelerate, and scale the input accordingly
            return (float) (1f - Math.pow(1f - (x / scaleDurationPct), 2f));
          }
          return 1f;
        }
      };

      // Determine the bounds of how to scale
      float halfScreenWidth = (w - 2f * mBgPadding) / 2f;
      float halfScreenHeight = (h - 2f * mBgPadding) / 2f;
      final float offsetPct = SCREENSHOT_DROP_OUT_MIN_SCALE_OFFSET;
      final PointF finalPos = new PointF(
          -halfScreenWidth + (SCREENSHOT_DROP_OUT_MIN_SCALE + offsetPct) * halfScreenWidth,
          -halfScreenHeight + (SCREENSHOT_DROP_OUT_MIN_SCALE + offsetPct) * halfScreenHeight);

      // Animate the screenshot to the status bar
      anim.setDuration(SCREENSHOT_DROP_OUT_DURATION);
      anim.addUpdateListener(new AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
          float t = (Float) animation.getAnimatedValue();
          float scaleT = (SCREENSHOT_DROP_IN_MIN_SCALE + mBgPaddingScale)
              - scaleInterpolator.getInterpolation(t)
              * (SCREENSHOT_DROP_IN_MIN_SCALE - SCREENSHOT_DROP_OUT_MIN_SCALE);
          mBackgroundView.setAlpha((1f - t) * BACKGROUND_ALPHA);
          mScreenshotView.setAlpha(1f - scaleInterpolator.getInterpolation(t));
          mScreenshotView.setScaleX(scaleT);
          mScreenshotView.setScaleY(scaleT);
          mScreenshotView.setTranslationX(t * finalPos.x);
          mScreenshotView.setTranslationY(t * finalPos.y);
        }
      });
    }
    return anim;
  }

}