/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.rt.script.ping;

import java.io.IOException;

import com.infiniteautomation.mango.spring.service.MangoJavaScriptService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.script.ScriptUtility;
import com.serotonin.m2m2.module.definitions.script.PingUtilityDefinition;

public class TestPingUtility extends ScriptUtility implements PingUtility {

    public TestPingUtility(MangoJavaScriptService service, PermissionService permissionService) {
        super(service, permissionService);
    }

    @Override
    public String getContextKey() {
        return PingUtilityDefinition.CONTEXT_KEY;
    }

    @Override
    public float ping(String hostname, int count) {
        return 10f;
    }

    @Override
    public boolean isReachable(String hostname, int timeout) {
        return true;
    }

    @Override
    public float isReachablePing(String hostname, int timeout) throws IOException {
        return 10f;
    }
}
