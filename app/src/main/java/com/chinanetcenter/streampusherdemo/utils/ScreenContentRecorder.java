package com.chinanetcenter.streampusherdemo.utils;

import com.chinanetcenter.StreamPusher.sdk.SPScreenShot;
import com.chinanetcenter.StreamPusher.sdk.SPSurfaceView;
import com.chinanetcenter.StreamPusher.utils.ALog;
import com.chinanetcenter.streampusherdemo.R;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.Toast;

public class ScreenContentRecorder {
    private static final String TAG = "ScreenContentRecorder";
    private Context mContext = null;
    private Handler mUiHandler = null;
    private Thread mSurfaceShotThread = null;

    public ScreenContentRecorder(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null");
        }
        mContext = context;
        mUiHandler = new Handler(Looper.getMainLooper());
    }
    
    public void takeShotOptional(final SPSurfaceView surfaceView, final View contentView){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            takeSurfaceShotAsync(surfaceView, contentView);
            return;
        }
        DialogUtils.showSingleChoiceDialog(mContext, "请选择截屏方式", new String[] { "全屏截屏", "视图截屏" }, -1, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                case 0:
                    mUiHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            takeFullScreenShot();
                        }
                    }, 200);//做个延时，防止截图截到菜单窗口,菜单窗口的关闭动画是150ms
                    break;
                case 1:
                    takeSurfaceShotAsync(surfaceView, contentView);
                    break;
                default:
                    break;
                }
            }
        });
        
    }

    public void takeFullScreenShot() {
        SPScreenShot shotter = new SPScreenShot(mContext);
        shotter.takeGlobalScreenShot();
        Log.i(TAG, " take screenshot start : " + System.currentTimeMillis());
        shotter.setScreenShotListener(new SPScreenShot.ScreenCaptureListener() {
            @Override
            public void onScreenCaptured(final Bitmap bitmap) {
                Log.i(TAG, " take screenshot onScreenCaptured : " + System.currentTimeMillis());
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // 截屏动画,必须在ui线程执行
                        ScreenShotAnimationExcutor excutor = new ScreenShotAnimationExcutor(mContext);
                        excutor.excuteFullScreenAnimation(bitmap);

                    }
                });
            }

            @Override
            public void onScreenCaptureWillStart() {
                Log.i(TAG, " take screenshot onScreenCaptureWillStart : " + System.currentTimeMillis());
            }

            @Override
            public void onScreenCaptureFinish() {
                Log.i(TAG, " take screenshot onScreenCaptureFinish : " + System.currentTimeMillis());
            }

            @Override
            public void onScreenCaptureError(int errorCode, String errorDetail) {
            }
        });
    }
    
    public void takeSurfaceShotAsync(final SPSurfaceView surfaceView, final View contentView) {
        if(mSurfaceShotThread != null) {
            Log.i(TAG, "Surface is shoting , ignore this time");
            return;
        }
        mSurfaceShotThread = new Thread(){
            @Override
            public void run() {
                View surfaceBgView = (View)contentView.findViewById(R.id.surface_group);
                View layoutView = (View)contentView.findViewById(R.id.view_group);
                Bitmap bitmapCaptured = null;
                if(surfaceBgView != null && layoutView != null) {
                    bitmapCaptured = Bitmap.createBitmap(contentView.getWidth(), contentView.getHeight(), Bitmap.Config.ARGB_8888 );
                    Canvas canvas = new Canvas(bitmapCaptured);
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                    canvas.drawBitmap(getViewDrawingCache(surfaceBgView)
                            , null
                            , new Rect(surfaceBgView.getLeft(), surfaceBgView.getTop(), surfaceBgView.getRight(), surfaceBgView.getBottom())
                            , paint);//draw surfaceBg
                    Bitmap surfaceContent = surfaceView.takeSurfaceShot();
                    int[] contentViewLocation = new int[2];
                    contentView.getLocationOnScreen(contentViewLocation);//相对屏幕坐标，0是left，1是top
                    int[] surfaceViewLocation = new int[2];
                    surfaceView.getLocationOnScreen(surfaceViewLocation);
                    int positionX = surfaceViewLocation[0] - contentViewLocation[0];
                    int positionY = surfaceViewLocation[1] - contentViewLocation[1];
                    if(surfaceContent != null){
                        canvas.drawBitmap(surfaceContent
                                , null
                                , new Rect(positionX, positionY, positionX + surfaceView.getWidth(), positionY + surfaceView.getHeight())
                                , paint);//draw surface
                    }
                    canvas.drawBitmap(getViewDrawingCache(layoutView)
                            , null
                            , new Rect(layoutView.getLeft(), layoutView.getTop(), layoutView.getRight(), layoutView.getBottom())
                            , paint);//draw layoutView
                    
                } else {
                    bitmapCaptured = surfaceView.takeSurfaceShot();//可能耗时
                }
                
                final Bitmap finalBitmapCaptured = bitmapCaptured;
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (finalBitmapCaptured != null) {
                            // 截屏动画,必须在ui线程执行
                            ScreenShotAnimationExcutor excutor = new ScreenShotAnimationExcutor(mContext);
                            excutor.excuteFullScreenAnimation(finalBitmapCaptured);
                        } else {
                            Toast.makeText(mContext, "截屏失败，未截取到任何数据", Toast.LENGTH_SHORT).show();
                        }
                        mSurfaceShotThread = null;
                    }
                });
            };
        };
        mSurfaceShotThread.start();
    }
 
    private Bitmap getViewDrawingCache(View view) {
        int viewWidth = view.getWidth();
        int viewHeight = view.getHeight();
        if (viewWidth == 0 && viewHeight == 0) {
            view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            viewWidth = view.getMeasuredWidth();
            viewHeight = view.getMeasuredHeight();
            view.layout(0, 0, viewWidth, viewHeight);
        }
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true);
        Bitmap bitmap = view.getDrawingCache(true);
        if(bitmap == null) {
            ALog.e(TAG, "getViewDrawingCache failed, cached null");
            return null;
        }
        bitmap = bitmap.copy(bitmap.getConfig(), true);//copy this bitmap before destroyDrawingCache() method recycle it
        view.destroyDrawingCache();
        view.setDrawingCacheEnabled(false);
        return bitmap;
    }

}
