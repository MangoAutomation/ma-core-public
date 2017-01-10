/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module;

/**
 * Listen for notifications on Module upgrades, downloads 
 * @author Terry Packer
 */
public interface ModuleNotificationListener {
	
	/**
	 * When a module is downloaded from the store
	 * @param name
	 * @param version
	 */
	public void moduleDownloaded(String name, String version);
	
	/**
	 * When an upgrade is available
	 * @param name
	 * @param version
	 */
	public void moduleUpgradeAvailable(String name, String version);

	/**
	 * When the upgrade process state changes
	 * @param stage
	 */
	public void upgradeStateChanged(String stage);

	/**
	 * New Module Available from Store
	 * @param name
	 * @param version
	 */
	public void newModuleAvailable(String name, String version);

}
