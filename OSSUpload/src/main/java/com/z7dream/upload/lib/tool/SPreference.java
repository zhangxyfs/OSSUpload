package com.z7dream.upload.lib.tool;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.z7dream.upload.Appli;

/**
 * Created by Z7Dream on 2017/7/17 15:01.
 * Email:zhangxyfs@126.com
 */

public class SPreference {
    private static final String USER_SETTING = "com.z7dream.upload.userData";
    private static final String USER_ID = "userId";

    private static SharedPreferences getBase() {
        return Appli.getContext().getApplicationContext().getSharedPreferences(USER_SETTING, Context.MODE_PRIVATE);
    }

    private static SharedPreferences getBase(@NonNull String key) {
        return Appli.getContext().getSharedPreferences(key, Context.MODE_PRIVATE);
    }

    private static void putString(@NonNull String key, @NonNull String value) {
        SharedPreferences.Editor edit = getBase().edit();
        edit.putString(key, value);
        edit.apply();
    }

    private static String getString(@NonNull String key) {
        return getBase().getString(key, null);
    }

    private static void putInt(@NonNull String key, @NonNull int value) {
        SharedPreferences.Editor edit = getBase().edit();
        edit.putInt(key, value);
        edit.apply();
    }

    /**
     * 默认值为-1
     *
     * @param key
     * @return
     */
    private static int getInt(@NonNull String key) {
        return getBase().getInt(key, -1);
    }

    private static void putBoolean(@NonNull String key, @NonNull boolean value) {
        SharedPreferences.Editor edit = getBase().edit();
        edit.putBoolean(key, value);
        edit.apply();
    }

    private static boolean getBoolean(@NonNull String key) {
        return getBase().getBoolean(key, false);
    }

    private static void putLong(@NonNull String key, @NonNull long value) {
        SharedPreferences.Editor edit = getBase().edit();
        edit.putLong(key, value);
        edit.apply();
    }

    private static long getLong(@NonNull String key) {
        return getBase().getLong(key, 0L);
    }

    public static Long getUserId() {
        return getLong(USER_ID);
    }
}
