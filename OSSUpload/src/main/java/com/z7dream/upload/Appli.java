package com.z7dream.upload;

import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import android.support.multidex.MultiDexApplication;

import com.z7dream.upload.lib.db.bean.MyObjectBox;

import io.objectbox.BoxStore;

/**
 *  Created by xiaoyu.zhang on 2016/11/7 13:28
 *  
 */
public class Appli extends MultiDexApplication {
    private static Context context;
    private static BoxStore boxStore;

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }
        boxStore = MyObjectBox.builder().androidContext(this).build();
    }

    public static BoxStore getBoxStore() {
        return boxStore;
    }
    public static Context getContext() {
        return context;
    }
}
