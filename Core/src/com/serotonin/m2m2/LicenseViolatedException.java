package com.serotonin.m2m2;

import com.serotonin.m2m2.i18n.TranslatableMessage;

public class LicenseViolatedException extends RuntimeException {

	private final TranslatableMessage errorMessage;
	
	public LicenseViolatedException(TranslatableMessage errorMessage) {
		super();
		this.errorMessage = errorMessage;
	}
	
	public TranslatableMessage getErrorMessage() {
		return errorMessage;
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
