/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.rt.script.ping;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.script.ScriptUtility;
import com.serotonin.m2m2.module.definitions.script.PingUtilityDefinition;

public class DefaultPingUtility extends ScriptUtility implements PingUtility {

    private final boolean isWindows;
    private final Pattern minMaxAvgPattern;
    private final Pattern packetLossPattern;

    public DefaultPingUtility(MangoJavaScriptService service, PermissionService permissionService) {
        super(service, permissionService);

        var os = java.lang.System.getProperty("os.name");
        this.isWindows = os != null && os.toLowerCase().contains("windows");
        if (isWindows) {
            this.minMaxAvgPattern = Pattern.compile("Minimum = (?<min>\\d+)ms, Maximum = (?<max>\\d+)ms, Average = (?<avg>\\d+)ms");
            this.packetLossPattern = Pattern.compile("\\((?<pl>\\d+)% loss\\)");
        } else {
            this.minMaxAvgPattern = Pattern.compile("rtt min/avg/max/mdev = (?<min>[\\d.]+)/(?<avg>[\\d.]+)/(?<max>[\\d.]+)/[\\d.]+ ms");
            this.packetLossPattern = Pattern.compile("(?<pl>\\d+)% packet loss");
        }
    }

    @Override
    public String getContextKey() {
        return PingUtilityDefinition.CONTEXT_KEY;
    }

    @Override
    public PingStats ping(String hostname, int count) throws IOException, InterruptedException {
        var countArg = isWindows ? "-n" : "-c";
        var command = new String[] {"ping", countArg, "1", hostname};

        Float min = null, max = null, avg = null, pl = null;
        var process = Runtime.getRuntime().exec(command);
        var success = process.waitFor() == 0;
        if (!success) {
            throw new IOException("Ping process failed");
        }

        try (var stream = process.getInputStream()) {
            var output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            Matcher minMaxAvgMatcher = minMaxAvgPattern.matcher(output);
            if (minMaxAvgMatcher.find()) {
                min = Float.parseFloat(minMaxAvgMatcher.group("min"));
                max = Float.parseFloat(minMaxAvgMatcher.group("max"));
                avg = Float.parseFloat(minMaxAvgMatcher.group("avg"));
            }

            Matcher packetLossMatcher = packetLossPattern.matcher(output);
            if (packetLossMatcher.find()) {
                pl = Float.parseFloat(packetLossMatcher.group("pl"));
            }
        }

        return new PingStats(count, min, max, avg, pl);
    }

    @Override
    public boolean isReachable(String hostname, int timeout) throws IOException {
        InetAddress address = InetAddress.getByName(hostname);
        return address.isReachable(timeout);
    }

    @Override
    public float isReachablePing(String hostname, int timeout) throws IOException {
        InetAddress address = InetAddress.getByName(hostname);
        long start = System.nanoTime();
        if (!address.isReachable(timeout)) {
            throw new IOException("Host is not reachable");
        }
        long duration = System.nanoTime() - start;
        return duration / 1_000_000f;
    }
}
