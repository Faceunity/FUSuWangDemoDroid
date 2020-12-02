
package com.chinanetcenter.streampusherdemo.view;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class FloatingView extends FrameLayout implements GestureDetector.OnGestureListener, View.OnTouchListener {
    private static final String TAG = FloatingView.class.getSimpleName();

    protected final Context mContext;
    protected WindowManager mWindowManager;
    private GestureDetector mGestureDetector;
    protected WindowManager.LayoutParams layoutParams;
    private float lastX, lastY;

    public FloatingView(Context context) {
        super(context);

        this.mContext = context;

        this.mGestureDetector = new GestureDetector(context, this);

    }

    public FloatingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        this.mGestureDetector = new GestureDetector(context, this);
    }

    public FloatingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        this.mGestureDetector = new GestureDetector(context, this);
    }

    public void showView(View view) {
        showView(view, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    public void showView(View view, int width, int height) {
        try{
            addView(view);
            mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            //TYPE_TOAST仅适用于4.4+系统，假如要支持更低版本使用TYPE_SYSTEM_ALERT(需要在manifest中声明权限)
            layoutParams = new WindowManager.LayoutParams();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else if(Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) {
                layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                layoutParams.type = WindowManager.LayoutParams.TYPE_TOAST;
            } else {
                layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            }
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
//            layoutParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            layoutParams.width = width;
            layoutParams.height = height;
            layoutParams.format = PixelFormat.TRANSLUCENT;
            mWindowManager.addView(this, layoutParams);
        }catch(Exception e){
            e.printStackTrace();
            Log.e(TAG, "showView Exception: " + e.getMessage());
        }
    }

    public void hideView() {
        if (null != mWindowManager) {
            mWindowManager.removeViewImmediate(this);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        lastX = e.getRawX();
        lastY = e.getRawY();
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float nowX, nowY, tranX, tranY;
        // 获取移动时的X，Y坐标
        nowX = e2.getRawX();
        nowY = e2.getRawY();
        // 计算XY坐标偏移量
        tranX = nowX - lastX;
        tranY = nowY - lastY;
        // 移动悬浮窗
        layoutParams.x += tranX;
        layoutParams.y += tranY;
        //更新悬浮窗位置
        mWindowManager.updateViewLayout(this ,layoutParams);
        //记录当前坐标作为下一次计算的上一次移动的位置坐标
        lastX = nowX;
        lastY = nowY;
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        //Toast.makeText(mContext, "onLongPress", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }
}
