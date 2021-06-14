/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.util;

import com.serotonin.m2m2.i18n.TranslatableMessage;

public enum ILifecycleState {
    PRE_INITIALIZE,
    INITIALIZING,
    RUNNING,
    TERMINATING,
    TERMINATED;

    public TranslatableMessage getMessage() {
        return new TranslatableMessage("common.lifecycle.state." + name());
    }
}
