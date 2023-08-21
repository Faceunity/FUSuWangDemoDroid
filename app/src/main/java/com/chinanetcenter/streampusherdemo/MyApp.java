package com.chinanetcenter.streampusherdemo;

import android.app.Application;

import com.chinanetcenter.StreamPusher.utils.ErrorHandler;
import com.faceunity.nama.FUConfig;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.utils.FuDeviceUtils;

//import com.squareup.leakcanary.LeakCanary;
//import com.squareup.leakcanary.watcher.RefWatcher;

public class MyApp extends Application {

    private static MyApp myApp;
    // public static RefWatcher getRefWatcher(Context context) {
    // MyApp application = (MyApp) context.getApplicationContext();
    // return application.refWatcher;
    // }

    // public RefWatcher refWatcher;
    //
    // @Override
    public void onCreate() {
        super.onCreate();
        myApp = this;
        FUConfig.DEVICE_LEVEL = FuDeviceUtils.judgeDeviceLevelGPU();
        ErrorHandler.getInstance().attach(this);
        // refWatcher = LeakCanary.install(this);
        FURenderer.getInstance().setup(this);
    }

    public static MyApp getMyInstance() {
        return myApp;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        ErrorHandler.getInstance().dettach(this);
    }
}
