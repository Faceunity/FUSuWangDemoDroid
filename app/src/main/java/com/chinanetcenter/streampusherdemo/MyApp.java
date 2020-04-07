package com.chinanetcenter.streampusherdemo;

import android.app.Application;

import com.chinanetcenter.StreamPusher.utils.ErrorHandler;

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

        ErrorHandler.getInstance().attach(this);
        // refWatcher = LeakCanary.install(this);
    }

    public static MyApp getMyInstance() {
        return myApp;
    }

    public void onTerminate() {
        super.onTerminate();
        ErrorHandler.getInstance().dettach(this);
    }
}
