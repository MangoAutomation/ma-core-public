/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.script;

import com.infiniteautomation.mango.util.script.ScriptUtility;
import com.serotonin.m2m2.module.MangoJavascriptContextObjectDefinition;
import com.serotonin.m2m2.rt.script.ping.DefaultPingUtility;
import com.serotonin.m2m2.rt.script.ping.TestPingUtility;

public class PingUtilityDefinition extends MangoJavascriptContextObjectDefinition {

    public static final String CONTEXT_KEY = "pingUtility";

    @Override
    protected Class<? extends ScriptUtility> getUtilityClass() {
        return DefaultPingUtility.class;
    }

    @Override
    protected Class<? extends ScriptUtility> getTestUtilityClass() {
        return TestPingUtility.class;
    }

}
