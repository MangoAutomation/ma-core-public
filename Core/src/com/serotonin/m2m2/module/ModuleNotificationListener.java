/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.i18n.TranslatableMessage;

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
        
        
        public TranslatableMessage getDescription() {
            switch(this.name()) {
                case "IDLE":
                    return new TranslatableMessage("startup.state.notStarted");
                case "STARTED":
                    return new TranslatableMessage("modules.downloadUpgrades.stage.start");
                case "BACKUP":
                    return new TranslatableMessage("modules.downloadUpgrades.stage.backup");
                case "DOWNLOAD":
                    return new TranslatableMessage("modules.downloadUpgrades.stage.download");
                case "INSTALL":
                    return new TranslatableMessage("modules.downloadUpgrades.stage.install");
                case "RESTART":
                    return new TranslatableMessage("modules.downloadUpgrades.stage.restart");
                case "DONE":
                    return new TranslatableMessage("modules.downloadUpgrades.stage.done");
                case "CANCELLED":
                    return new TranslatableMessage("common.cancelled");
                default:
                    throw new ShouldNeverHappenException("Unknown upgrade state: " + this.name());
            }
        }
    }
	
	/**
	 * When a module is downloaded from the store
     */
	public void moduleDownloaded(String name, String version);
	
	/**
	 * When a module fails to download from the store
     */
	public void moduleDownloadFailed(String name, String version, String reason);
	
	/**
	 * When an upgrade is available
     */
	public void moduleUpgradeAvailable(String name, String version);

	/**
	 * When the upgrade process state changes
     */
	public void upgradeStateChanged(UpgradeState stage);

	/**
	 * When the upgrade process fails
     */
	public void upgradeError(String error);
	
	/**
	 * When the upgrade process is complete or has failed on error
	 */
	public void upgradeTaskFinished();
	
	/**
	 * New Module Available from Store
     */
	public void newModuleAvailable(String name, String version);

}
