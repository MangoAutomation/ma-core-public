/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.systemSettings;

/**
 * Allows monitoring of System Settings for CRUD Operations
 * 
 * 
 * 
 * @author Terry Packer
 *
 */
public interface SystemSettingsListener {

	
	public void SystemSettingsSaved(String key, Object oldValue, Object newValue);
	
	
	public void SystemSettingsRemoved(String key, Object lastValue);
		
	
	
}
