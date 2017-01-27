package com.serotonin.m2m2.module.license;

import com.serotonin.provider.Provider;

public interface ITimedLicenseRegistrar extends Provider {
	public void registerTimedLicense(ITimedLicenseDefinition license);
	public void checkLicenses(boolean initialization);
	
	/**
	 * De-activate any alarms and close up shop
	 */
	public void shutdown();
}
