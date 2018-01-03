/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.module;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;

/**
 * Listen for notifications on Module upgrades, downloads 
 * @author Terry Packer
 */
public interface ModuleNotificationListener {
    
    public enum UpgradeState {
        IDLE,
        STARTED,
        BACKUP,
        DOWNLOAD,
        INSTALL,
        RESTART,
        DONE,
        CANCELLED;
        
        
        public String getDescription() {
            switch(this.name()) {
                case "IDLE":
                    return Common.translate("startup.state.notStarted");
                case "STARTED":
                    return Common.translate("modules.downloadUpgrades.stage.start");
                case "BACKUP":
                    return Common.translate("modules.downloadUpgrades.stage.backup");
                case "DOWNLOAD":
                    return Common.translate("modules.downloadUpgrades.stage.download");
                case "INSTALL":
                    return Common.translate("modules.downloadUpgrades.stage.install");
                case "RESTART":
                    return Common.translate("modules.downloadUpgrades.stage.restart");
                case "DONE":
                    return Common.translate("modules.downloadUpgrades.stage.done");
                case "CANCELLED":
                    return Common.translate("common.cancelled");
                default:
                    throw new ShouldNeverHappenException("Unknown upgrade state: " + this.name());
            }
        }
    }
	
	/**
	 * When a module is downloaded from the store
	 * @param name
	 * @param version
	 */
	public void moduleDownloaded(String name, String version);
	
	/**
	 * When a module fails to download from the store
	 * @param name
	 * @param version
	 * @param reason
	 */
	public void moduleDownloadFailed(String name, String version, String reason);
	
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
	public void upgradeStateChanged(UpgradeState stage);

	/**
	 * When the upgrade process fails
	 * @param error
	 */
	public void upgradeError(String error);
	
	/**
	 * When the upgrade process is complete or has failed on error
	 */
	public void upgradeTaskFinished();
	
	/**
	 * New Module Available from Store
	 * @param name
	 * @param version
	 */
	public void newModuleAvailable(String name, String version);

}
