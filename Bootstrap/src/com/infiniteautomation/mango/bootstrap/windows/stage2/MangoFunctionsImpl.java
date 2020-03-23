/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.bootstrap.windows.stage2;

import com.infiniteautomation.mango.bootstrap.windows.MangoFunctions;
import com.serotonin.m2m2.IMangoLifecycle;
import com.serotonin.provider.Providers;

/**
 * @author Jared Wiltshire
 */
public class MangoFunctionsImpl implements MangoFunctions {

    @Override
    public void start() throws Exception {
        com.serotonin.m2m2.Main.main(new String[0]);
    }

    @Override
    public void stop() throws Exception {
        IMangoLifecycle lifecycle = Providers.get(IMangoLifecycle.class);
        lifecycle.terminate();
    }

}
