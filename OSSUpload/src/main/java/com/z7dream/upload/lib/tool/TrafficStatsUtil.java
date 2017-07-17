package com.z7dream.upload.lib.tool;

import android.content.Context;
import android.net.TrafficStats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Z7Dream on 2017/6/21 16:19.
 * Email:zhangxyfs@126.com
 */

public class TrafficStatsUtil {
    /**
     * 根据应用id,统计的接受字节数，包含Mobile和WiFi等
     *
     * @return 上传及下载流量，单位Kb
     */
    public static long getBytesWithUid(int uid) {
        //下载流量
        long rxBytes = TrafficStats.getUidRxBytes(uid);
        //上传流量
        long txBytes = TrafficStats.getUidTxBytes(uid);
        long result = rxBytes + txBytes;
        return result > 0 ? result / 1024 : 0;
    }

    /**
     * 获取本应用的接受字节数，包含Mobile和WiFi等
     *
     * @return 上传及下载流量，单位Kb
     */
    public static long getLocalBytes(Context context) {
        //本应用的进程id
        int uid = context.getApplicationInfo().uid;
        return getBytesWithUid(uid);
    }

    /**
     * 获取本应用通过Mobile的流量
     *
     * @return 上传及下载流量，单位Kb
     */
    public static long getLocalBytesWithNet(Context context) {
        //本应用的进程id
        int uid = context.getApplicationInfo().uid;
        File file = new File("/proc/net/xt_qtaguid/stats");
        List<String> list = readFile2List(file, "utf-8");
        long rxBytesNet = 0L;
        long txBytesNet = 0L;
        for (String stat : list) {
            String[] split = stat.split(" ");
            try {
                int idx = Integer.parseInt(split[3]);
                if (uid == idx) {
                    long rx_bytes = Long.parseLong(split[5]);
                    long tx_bytes = Long.parseLong(split[7]);
                    if (split[1].startsWith("rmnet_data")) {
                        rxBytesNet += rx_bytes;
                        txBytesNet += tx_bytes;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
        long result = rxBytesNet + txBytesNet;
        return result > 0 ? result / 1024 : 0;
    }

    /**
     * 获取本应用通过Wlan的流量
     *
     * @return 上传及下载流量，单位Kb
     */
    public static long getLocalBytesWithWlan(Context context) {
        //本应用的进程id
        int uid = context.getApplicationInfo().uid;
        File file = new File("/proc/net/xt_qtaguid/stats");
        List<String> list = readFile2List(file, "utf-8");
        long rxBytesWlan = 0L;
        long txBytesWlan = 0L;
        for (String stat : list) {
            String[] split = stat.split(" ");
            try {
                int idx = Integer.parseInt(split[3]);
                if (uid == idx) {
                    long rx_bytes = Long.parseLong(split[5]);
                    long tx_bytes = Long.parseLong(split[7]);
                    if (split[1].startsWith("wlan")) {
                        rxBytesWlan += rx_bytes;
                        txBytesWlan += tx_bytes;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
        long result = rxBytesWlan + txBytesWlan;
        return result > 0 ? result / 1024 : 0;
    }

    /**
     * 获取总的接受字节数，包含Mobile和WiFi等
     *
     * @return 上传及下载流量，单位Kb
     */
    public static long getTotalBytes() {
        //总的下载流量
        long totalRxBytes = TrafficStats.getTotalRxBytes();
        //总的上传你流量
        long totalTxBytes = TrafficStats.getTotalTxBytes();
        long result = totalRxBytes + totalTxBytes;
        return result > 0 ? result / 1024 : 0;
    }


    /**
     * 获取通过Mobile连接收到的字节总数，不包含WiFi
     *
     * @return 上传及下载流量，单位Kb
     */
    public static long getMobileBytes() {
        //数据的下载流量
        long mobileRxBytes = TrafficStats.getMobileRxBytes();
        //数据的上传你流量
        long mobileTxBytes = TrafficStats.getMobileTxBytes();
        long result = mobileRxBytes + mobileTxBytes;
        return result > 0 ? result / 1024 : 0;
    }

    /**
     * 指定编码按行读取文件到链表中
     *
     * @param file        文件
     * @param charsetName 编码格式
     * @return 包含从start行到end行的list
     */
    public static List<String> readFile2List(File file, String charsetName) {
        if (file == null) {
            return null;
        }
        BufferedReader reader = null;
        try {
            String line;
            int curLine = 1;
            List<String> list = new ArrayList<>();
            if ((charsetName == null || charsetName.trim().length() == 0)) {
                reader = new BufferedReader(new FileReader(file));
            } else {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charsetName));
            }
            while ((line = reader.readLine()) != null) {
                if (0 <= curLine) {
                    list.add(line);
                }
                ++curLine;
            }
            return list;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
