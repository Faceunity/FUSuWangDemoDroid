package com.chinanetcenter.streampusherdemo.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BaseActivity extends Activity {
    private static final String TAG = "BaseActivity";
    protected String[] PERMISSIONS = new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.BLUETOOTH};
    protected String[] PERMISSION_TOAST_STRING = new String[] { "存储", "相机", "麦克风", "蓝牙" };
    
    protected static long EXIT_INTERVAL = 2*1000;
    private long mBackKeyLastPressedTime;
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - mBackKeyLastPressedTime > EXIT_INTERVAL) {
                Toast.makeText(this, "再按一次离开界面", Toast.LENGTH_SHORT).show();
                mBackKeyLastPressedTime = currentTime;
                return false;
            }
            return super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.BLACK);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected boolean checkAndRequestPermission() {

        ArrayList<String> lackedPermissions = new ArrayList<String>(PERMISSIONS.length);
        for (int i = 0; i < PERMISSIONS.length; i++) {
            int result = PermissionChecker.checkCallingOrSelfPermission(this, PERMISSIONS[i]);
            if (result != PackageManager.PERMISSION_GRANTED) {
                lackedPermissions.add(PERMISSIONS[i]);
            }
        }

        if (lackedPermissions.size() > 0) {
            String[] rP = new String[lackedPermissions.size()];
            lackedPermissions.toArray(rP);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M - 1) {
                requestPermissions(rP, 0);
                Log.i(TAG, "requestPermissions " + lackedPermissions.toString() + " !");
            } else {
                int[] grantResults = new int[rP.length];
                for (int i = 0; i < grantResults.length; i++) {
                    grantResults[i] = PackageManager.PERMISSION_GRANTED;
                }
                onRequestPermissionsResult(0, rP, grantResults);
                Log.i(TAG, "the platform versin below 23 M , cann't request permissions  !");
            }
            return false;
        }
        Log.i(TAG, "checkPermission success , All permission has granted !");
        return true;
    }

    protected List<String> checkPermissions() {

        ArrayList<String> lackedPermissions = new ArrayList<String>(PERMISSIONS.length);
        for (int i = 0; i < PERMISSIONS.length; i++) {
            int result = PermissionChecker.checkCallingOrSelfPermission(this, PERMISSIONS[i]);
            if (result != PackageManager.PERMISSION_GRANTED) {
                lackedPermissions.add(PERMISSIONS[i]);
            }
        }
        return lackedPermissions;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "permissions :" + Arrays.toString(permissions));
        Log.i(TAG, "grantResults :" + Arrays.toString(grantResults));
        ArrayList<String> lackedPermissions = new ArrayList<String>(PERMISSIONS.length);
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                lackedPermissions.add(permissions[i]);
            }
        }
        showRequestPermissinDialog(lackedPermissions);
    }

    protected void showRequestPermissinDialog(List<String> mLackedPermissions) {
        if (mLackedPermissions == null || mLackedPermissions.size() == 0)
            return;
        StringBuilder stringBuilder = new StringBuilder();
        for (String permission : mLackedPermissions) {
            for (int i = 0; i < PERMISSIONS.length; i++) {
                if (PERMISSIONS[i].equals(permission)) {
                    stringBuilder.append(PERMISSION_TOAST_STRING[i]).append(",");
                }
            }
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);

        AlertDialog.Builder builder = new Builder(this);
        builder.setMessage("应用需要如下权限： " + stringBuilder.toString() + "，请从“设置”中打开相应权限。");
        builder.setTitle(android.R.string.dialog_alert_title);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do nothing

                // Intent intent = new Intent();
                // intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                // intent.setData(Uri.fromParts("package", getPackageName(),
                // null));
                // startActivity(intent);

            }
        });
        builder.setCancelable(false);
        builder.create().show();
    }
    
    protected void requestFullScreen() {
        Log.i(TAG, "addFlags FLAG_FULLSCREEN");
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }
    
    
    protected void showToast(final String text) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(BaseActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    protected void lockScreenToCurrentOrientation() {
        int orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        switch (wm.getDefaultDisplay().getRotation()) {
        case Surface.ROTATION_90:
            orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            break;
        case Surface.ROTATION_180:
            orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            break;
        case Surface.ROTATION_270:
            orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            break;
        case Surface.ROTATION_0:
        default:
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            break;
        }
        setRequestedOrientation(orientation);
    }

}
