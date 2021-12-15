/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.systemSettings;

import java.util.List;

/**
 * Allows monitoring of System Settings for CRUD Operations
 *
 * @author Terry Packer
 *
 */
public interface SystemSettingsListener {

    /**
     * A setting is being saved
     */
    public void systemSettingsSaved(String key, String oldValue, String newValue);

    /**
     * A setting is being removed
     * @param defaultValue the default value for the setting, which is now the value
     */
    public default void systemSettingsRemoved(String key, String lastValue, String defaultValue) {
        if (defaultValue != null) {
            this.systemSettingsSaved(key, lastValue, defaultValue);
        }
    }

    /**
     * Return a list of any settings you want to listen for changes to.
     * Must remain constant across invocations.
     */
    public List<String> getKeys();

}
