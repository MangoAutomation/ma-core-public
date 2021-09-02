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
    private final Pattern pattern;

    public DefaultPingUtility(MangoJavaScriptService service, PermissionService permissionService) {
        super(service, permissionService);

        var os = java.lang.System.getProperty("os.name");
        this.isWindows = os != null && os.toLowerCase().contains("windows");
        if (isWindows) {
            this.pattern = Pattern.compile("Minimum = \\d+ms, Maximum = \\d+ms, Average = (\\d+)ms");
        } else {
            this.pattern = Pattern.compile("rtt min/avg/max/mdev = [\\d.]+/[\\d.]+/([\\d.]+)/[\\d.]+ ms");
        }
    }

    @Override
    public String getContextKey() {
        return PingUtilityDefinition.CONTEXT_KEY;
    }

    @Override
    public float ping(String hostname, int count) throws IOException, InterruptedException {
        var countArg = isWindows ? "-n" : "-c";
        var command = new String[] {"ping", countArg, "1", hostname};

        var avgRtt = -1f;
        var process = Runtime.getRuntime().exec(command);
        var success = process.waitFor() == 0;
        if (!success) {
            throw new IOException("Ping process failed");
        }

        try (var stream = process.getInputStream()) {
            var output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = pattern.matcher(output);
            if (matcher.find()) {
                avgRtt = Float.parseFloat(matcher.group(1));
            }
        }

        return avgRtt;
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
