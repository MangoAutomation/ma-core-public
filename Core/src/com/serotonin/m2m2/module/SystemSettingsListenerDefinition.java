/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

import org.springframework.beans.factory.annotation.Autowired;

import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.vo.systemSettings.SystemSettingsEventDispatcher;
import com.serotonin.m2m2.vo.systemSettings.SystemSettingsListener;

/**
 * Module Definition to allow registering for System Settings changes
 *
 * @author Terry Packer
 */
public abstract class SystemSettingsListenerDefinition extends ModuleElementDefinition implements SystemSettingsListener{

    @Autowired
    protected SystemSettingsDao systemSettingsDao;

    /**
     * Register the listener, called in the Lifecycle
     */
    public void registerListener(){
        SystemSettingsEventDispatcher.INSTANCE.addListener(this);
    }

}
