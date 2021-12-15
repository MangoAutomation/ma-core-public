package com.serotonin.m2m2.module.license;

import java.util.List;

import com.serotonin.m2m2.i18n.TranslatableMessage;

public interface ITimedLicenseDefinition {
	public void addLicenseErrors(List<TranslatableMessage> errors);
	public void addLicenseWarnings(List<TranslatableMessage> warnings);
	/**
	 * 
	 * @return long less than 0 means no shutdown timeout, long greater than 0 means timeout milliseconds to expiry
	 */
	public long licenseCheck();
	public String getShutdownDescriptionKey();
}
