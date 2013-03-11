package com.serotonin.m2m2.util;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
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

    // TODO this only works for linux
    public static String getHwaddr(String iface) throws IOException {
        ProcessEPoll pep = Providers.get(ProcessEPollProvider.class).getProcessEPoll();
        String content = ProcessEpollUtils.getProcessInput(pep, 3000, "ifconfig", iface);
        return com.serotonin.util.StringUtils.findGroup(PATTERN_HWADDR, content);
    }

    public static List<NICInfo> getLocalInet4Addresses(boolean includeLoopback) throws SocketException {
        List<NICInfo> list = new ArrayList<NICInfo>();

        Enumeration<NetworkInterface> eni = NetworkInterface.getNetworkInterfaces();
        while (eni.hasMoreElements()) {
            NetworkInterface netint = eni.nextElement();
            if (netint.isLoopback() && !includeLoopback)
                continue;

            Enumeration<InetAddress> addrs = netint.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress addr = addrs.nextElement();
                if (addr instanceof Inet4Address) {
                    NICInfo ni = new NICInfo();
                    ni.setInterfaceName(netint.getName());
                    ni.setInetAddress(addr);
                    list.add(ni);
                }
            }
        }

        return list;
    }

    public static class NICInfo {
        private String interfaceName;
        private InetAddress inetAddress;

        public String getInterfaceName() {
            return interfaceName;
        }

        public void setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
        }

        public InetAddress getInetAddress() {
            return inetAddress;
        }

        public void setInetAddress(InetAddress inetAddress) {
            this.inetAddress = inetAddress;
        }
    }
}
