package com.z7dream.upload.lib.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.TrafficStats;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.z7dream.upload.lib.tool.NetUtils;
import com.z7dream.upload.lib.tool.Utils;
import com.z7dream.upload.lib.tool.rxjava.RxBus;
import com.z7dream.upload.lib.tool.rxjava.RxSchedulersHelper;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import static com.z7dream.upload.lib.manager.UploadManager.NET_IDLE_OBSERVABLE;
import static com.z7dream.upload.lib.tool.NetUtils.NetState.NET_NO;
import static com.z7dream.upload.lib.tool.RxConstant.CHECKING_NET_CONNECTION_OBSERVABLE;


/**
 * 网络探针（用于网速监控，流量监控，网络状态监控）
 * 该service用于闲时上传数据
 */
public class NetProbeService extends Service {
    private boolean isNetNotBad = false, isHasNet = true;
    private int trafficTime;
    private double rxtxSpeed = 1.0f;
    private long rxtxTotal = 0;

    private int MAX_TIME = 20;
    private int MAX_KB = 100;

    private Disposable trafficIntervalDisposable;
    private Observable<Boolean> netStateObservable;


    public NetProbeService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        netStateObservable = RxBus.get().register(CHECKING_NET_CONNECTION_OBSERVABLE, Boolean.class);
        netStateObservable.compose(RxSchedulersHelper.io())//网络监听处理，用于android7.0以下
                .subscribe(isConnect -> isHasNet = isConnect, error -> {
                });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {//网络监听处理，用于android7.0以上
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.requestNetwork(new NetworkRequest.Builder().build(), new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    isHasNet = NetUtils.getNetState() != NET_NO;
                }
            });
        }

        trafficIntervalDisposable = Flowable.interval(1000, TimeUnit.MILLISECONDS).compose(RxSchedulersHelper.fio())
                .subscribe(x -> {
                    isNetNotBad = false;
                    long tempSum = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes();
                    long rxtxLast = tempSum - rxtxTotal;
                    double tempSpeed = rxtxLast * 1000 / 2000;
                    rxtxTotal = tempSum;
                    if ((tempSpeed / 1024d) < MAX_KB && (rxtxSpeed / 1024d) < MAX_KB) {
                        trafficTime += 1;
                    } else {
                        trafficTime = 0;
                    }
                    if (!isHasNet) {
                        trafficTime = 0;
                    }
                    rxtxSpeed = tempSpeed;
                    if (trafficTime >= MAX_TIME) {
                        // 连续20秒内速度低于20kb，代表网络空闲
                        isNetNotBad = true;
                        Log.i("NetProbeService", "当前网络有空闲 " + tempSpeed / 1024d + " " + rxtxSpeed / 1024d);
                        trafficTime = 0; // 重新检测
                    }

                    if (isNetNotBad && isHasNet) {
                        RxBus.get().post(NET_IDLE_OBSERVABLE, true);
                    }
                }, error -> {
                });
    }


    public static void startService(Context context) {
        if (!Utils.isServiceRunning(context, NetProbeService.class.getName())) {
            context.startService(new Intent(context, NetProbeService.class));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (trafficIntervalDisposable != null && !trafficIntervalDisposable.isDisposed()) {
            trafficIntervalDisposable.dispose();
            trafficIntervalDisposable = null;
        }
        RxBus.get().unregister(CHECKING_NET_CONNECTION_OBSERVABLE, netStateObservable);
    }

    public static void stopService(Context context) {
        if (Utils.isServiceRunning(context, NetProbeService.class.getName())) {
            context.stopService(new Intent(context, NetProbeService.class));
        }
    }
}
