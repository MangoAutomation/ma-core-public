package com.serotonin.m2m2.util;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.epoll.ProcessEPoll;
import com.serotonin.epoll.ProcessEpollUtils;
import com.serotonin.provider.ProcessEPollProvider;
import com.serotonin.provider.Providers;

public class HostUtils {
    public static boolean isWindows() {
        return StringUtils.contains(System.getProperty("os.name"), "Windows");
    }

    public static boolean isLinux() {
        return StringUtils.contains(System.getProperty("os.name"), "Linux");
    }

    private static final Pattern PATTERN_HWADDR = Pattern.compile("HWaddr\\s+(.*?)\\s");

    public static String getHwaddr(String iface) throws IOException {
        ProcessEPoll pep = Providers.get(ProcessEPollProvider.class).getProcessEPoll();
        String content = ProcessEpollUtils.getProcessInput(pep, 3000, "ifconfig", iface);
        return com.serotonin.util.StringUtils.findGroup(PATTERN_HWADDR, content);
    }
}
