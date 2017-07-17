/**
 * *****************************************************************
 * Copyright    :   新英体育传媒集团                          © 2013 ssports.com 版权所有
 * <p/>
 * Filename     :   .java
 * Author       :   zhangxyfs
 * Date         :   2013-4-15
 * Version      :   V1.00
 * Description  :
 * <p/>
 * History      :   Modify Id  |  Date  |  Origin  |  Description
 * *****************************************************************
 */
package com.z7dream.upload.lib.tool;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Dev_MountInfo implements IDev {
    /**
     * ***
     */
    public final String HEAD = "dev_mount";
    public final String LABEL = "<label>";
    public final String MOUNT_POINT = "<mount_point>";
    public final String PATH = "<part>";
    public final String SYSFS_PATH = "<sysfs_path1...>";

    /**
     * Label for the volume
     */
    private final int NLABEL = 1;
    /**
     * Partition
     */
    private final int NPATH = 2;
    /**
     * Where the volume will be mounted
     */
    private final int NMOUNT_POINT = 3;
    private final int NSYSFS_PATH = 4;

    private final int DEV_INTERNAL = 0;
    private final int DEV_EXTERNAL = 1;

    private ArrayList<String> cache = new ArrayList<String>();

    private static Dev_MountInfo dev;
    private DevInfo info;

    private final File VOLD_FSTAB = new File(Environment.getRootDirectory()
            .getAbsoluteFile()
            + File.separator
            + "etc"
            + File.separator
            + "vold.fstab");

    public static Dev_MountInfo getInstance() {
        if (null == dev)
            dev = new Dev_MountInfo();
        return dev;
    }

    private DevInfo getInfo(final int device) {
        // for(String str:cache)
        // System.out.println(str);

        if (null == info)
            info = new DevInfo();

        try {
            initVoldFstabToCache();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (device >= cache.size())
            return null;
        String[] sinfo = cache.get(device).split(" ");

        info.setLabel(sinfo[NLABEL]);
        info.setMount_point(sinfo[NMOUNT_POINT]);
        info.setPath(sinfo[NPATH]);
        info.setSysfs_path(sinfo[NSYSFS_PATH]);

        return info;
    }

    private void initVoldFstabToCache() throws IOException {
        cache.clear();
        BufferedReader br = new BufferedReader(new FileReader(VOLD_FSTAB));
        String tmp = null;
        while ((tmp = br.readLine()) != null) {
            if (tmp.startsWith(HEAD)) {
                cache.add(tmp);
            }
        }
        br.close();
        cache.trimToSize();
    }

    public class DevInfo {
        private String label, mount_point, path, sysfs_path;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getMount_point() {
            return mount_point;
        }

        public void setMount_point(String mount_point) {
            this.mount_point = mount_point;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getSysfs_path() {
            return sysfs_path;
        }

        public void setSysfs_path(String sysfs_path) {
            this.sysfs_path = sysfs_path;
        }

    }

    @Override
    public DevInfo getInternalInfo() {
        return getInfo(DEV_INTERNAL);
    }

    @Override
    public DevInfo getExternalInfo() {
        return getInfo(DEV_EXTERNAL);
    }
}

interface IDev {
    Dev_MountInfo.DevInfo getInternalInfo();

    Dev_MountInfo.DevInfo getExternalInfo();
}