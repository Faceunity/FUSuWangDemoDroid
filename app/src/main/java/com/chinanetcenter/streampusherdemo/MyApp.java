package com.chinanetcenter.streampusherdemo;

import com.chinanetcenter.StreamPusher.utils.ErrorHandler;
import com.faceunity.beautycontrolview.FURenderer;

import android.app.Application;

//import com.squareup.leakcanary.LeakCanary;
//import com.squareup.leakcanary.watcher.RefWatcher;

public class MyApp extends Application {
    // public static RefWatcher getRefWatcher(Context context) {
    // MyApp application = (MyApp) context.getApplicationContext();
    // return application.refWatcher;
    // }

    // public RefWatcher refWatcher;
    //
    // @Override
    public void onCreate() {
        super.onCreate();
        ErrorHandler.getInstance().attach(this);
        // refWatcher = LeakCanary.install(this);
        FURenderer.initFURenderer(this);
    }

    public void onTerminate() {
        super.onTerminate();
        ErrorHandler.getInstance().dettach(this);
    }
}