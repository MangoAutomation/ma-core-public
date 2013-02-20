package com.serotonin.m2m2.util;

import org.apache.commons.lang3.StringUtils;

public class HostUtils {
    public static boolean isWindows() {
        return StringUtils.contains(System.getProperty("os.name"), "Windows");
    }

    public static boolean isLinux() {
        return StringUtils.contains(System.getProperty("os.name"), "Linux");
    }
}
