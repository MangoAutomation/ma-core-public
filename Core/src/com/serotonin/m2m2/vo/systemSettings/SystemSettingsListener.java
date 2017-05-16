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
	public void SystemSettingsSaved(String key, String oldValue, String newValue);
	
	/**
	 * A setting is being removed
	 * @param key
	 * @param lastValue
	 */
	public void SystemSettingsRemoved(String key, String lastValue);

	/**
	 * Return a list of any settings you want to listen for changes to
	 * @return
	 */
	public List<String> getKeys();
	
}
