
package com.chinanetcenter.streampusherdemo.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.chinanetcenter.StreamPusher.sdk.SPSurfaceView;
import com.chinanetcenter.streampusherdemo.R;
import com.chinanetcenter.streampusherdemo.activity.MainActivity;
import com.chinanetcenter.streampusherdemo.view.FloatingView;

public class FloatWindowService extends Service {
    
    private static final String TAG = "FloatWindowService";

    private FloatingView mFloatView;
    private int mFloatViewWidth, mFloatViewHeight;
    private SPSurfaceView mShowingSurfaceView;
    private boolean mAutoCameraControl;
    private boolean mRecording = false;

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return new FloatBinder();
    }

    public class FloatBinder extends Binder {
        public FloatWindowService getService() {
            return FloatWindowService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        mFloatView = new FloatingView(this) {

            @Override
            public boolean onSingleTapUp(MotionEvent event) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            }

            @Override
            protected void onConfigurationChanged(Configuration newConfig) {
                //可在此方法中监控宽高变化，然后根据宽高从新设置view在屏幕中的位置
                super.onConfigurationChanged(newConfig);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(mFloatViewWidth, View.MeasureSpec.EXACTLY);
                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(mFloatViewHeight, View.MeasureSpec.EXACTLY);
                Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                int rotation = display.getRotation();
                if((rotation % 2 == 0 /*竖屏*/ && mFloatViewWidth > mFloatViewHeight) || (rotation % 2 != 0 /*横屏*/ && mFloatViewWidth < mFloatViewHeight)) {//
                    setMeasuredDimension(mFloatViewHeight, mFloatViewWidth);
                    measureChildren(heightMeasureSpec, widthMeasureSpec);
                } else {
                    setMeasuredDimension(mFloatViewWidth, mFloatViewHeight);
                    measureChildren(widthMeasureSpec, heightMeasureSpec);
                }


            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        return START_STICKY;
    }

    public void showFloatSurfaceView(SPSurfaceView view, int videoWidth, int videoHeight){
        if(mShowingSurfaceView != null) {
            return;
        }
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        display.getMetrics(displayMetrics);
        int displayWidth = displayMetrics.widthPixels;
        int displayHeight = displayMetrics.heightPixels;
        if(videoWidth > videoHeight != displayWidth > displayHeight) {
            displayWidth = displayMetrics.heightPixels;
            displayHeight = displayMetrics.widthPixels;
        }
        //占1/4屏幕宽高
        displayWidth /= 4;
        displayHeight /= 4;
        float aspect = (float) videoWidth / videoHeight;
        mFloatViewWidth = Math.min(displayWidth, (int)(displayHeight * aspect));
        mFloatViewHeight = Math.min(displayHeight, (int)(displayWidth / aspect));
        mShowingSurfaceView = view;
        mAutoCameraControl = mShowingSurfaceView.isAutoCameraControl();
        mShowingSurfaceView.setAutoCameraControl(false);
        mFloatView.showView(mShowingSurfaceView, mFloatViewWidth, mFloatViewHeight);

    }
    
    public void dismissSurfaceView(){
        if(mShowingSurfaceView != null) {
            mShowingSurfaceView.setAutoCameraControl(mAutoCameraControl);
            mShowingSurfaceView = null;
            mFloatView.hideView();
        }
    }
    
    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        dismissSurfaceView();
        stopForeground(true);// 停止前台服务--参数：表示是否移除之前的通知
        super.onDestroy();
    }

    public void showRecordingNotification() {
        mRecording = true;
        showNotification("正在推流直播");
    }

    public void cancleRecordingNotification() {
        stopForeground(true);
        mRecording = false;
    }
    public void showScreenShotNotification() {
        //android 10 以后，调用系统录屏接口，必须启动前端服务
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            showNotification("截取屏幕");
        }
    }

    public void cancleScreenShotNotification() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if(!mRecording) {
                stopForeground(true);
            } else {
                showRecordingNotification();
            }
        }
    }

    private void showNotification(String contentText) {
        String channelId = getPackageName();
        String channelName = getResources().getString(R.string.app_name);

        Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this.getApplicationContext(), channelId);
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        } else {
            builder = new Notification.Builder(this.getApplicationContext()); //获取一个Notification构造器
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        builder.setContentIntent(contentIntent) // 设置PendingIntent
                .setContentTitle("网宿推流器") // 设置下拉列表里的标题
                .setSmallIcon(R.drawable.ic_launcher) // 设置状态栏内的小图标
                .setContentText(contentText) // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间
        Notification notification = builder.build(); // 获取构建好的Notification
        startForeground(1, notification);
    }

}
