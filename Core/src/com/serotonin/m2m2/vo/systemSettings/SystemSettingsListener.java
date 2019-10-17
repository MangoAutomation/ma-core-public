/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
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
     * @param key
     * @param oldValue
     * @param newValue
     */
    public void systemSettingsSaved(String key, String oldValue, String newValue);

    /**
     * A setting is being removed
     * @param key
     * @param lastValue
     * @param the default value for the setting, which is now the value
     */
    public void systemSettingsRemoved(String key, String lastValue, String defaultValue);

    /**
     * Return a list of any settings you want to listen for changes to.
     * Must remain constant across invocations.
     * @return
     */
    public List<String> getKeys();

}
