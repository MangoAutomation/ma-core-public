package com.serotonin.m2m2.module.license;

import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;

public interface ITimedLicenseDefinition {
	public void addLicenseErrors(List<TranslatableMessage> errors);
	public void addLicenseWarnings(List<TranslatableMessage> warnings);
	/**
	 * 
	 * @return long < 0 means no shutdown timeout, long > 0 = timeout milliseconds to expiry
	 */
	public long licenseCheck();
	public String getShutdownDescriptionKey();
}
