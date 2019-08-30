package com.chinanetcenter.streampusherdemo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceUtil {
    
    public static final String KEY_PREVIEW_WIDTH = "preview_width";
    public static final String KEY_PREVIEW_HEIGHT = "preview_height";
    public static final String KEY_PREVIEW_MIRROR = "preview_mirror_";
    public static final String KEY_ENCODE_MIRROR = "encode_mirror_";
    public static final String KEY_FACEUNITY_ISON = "faceunity_ison";

    public static boolean persistString(Context context, String key, String value) {
        if(context == null) return false;
        SharedPreferences defaultPreference = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            defaultPreference.edit().putString(key, value).commit();
        } catch (Exception e) {
            return false;
        }
        return true;
    }
    
    public static String getString(Context context, String key) {
        if(context == null) return null;
        SharedPreferences defaultPreference = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return defaultPreference.getString(key, null);
        } catch (Exception e) {
            return null;
        }
    }

}
