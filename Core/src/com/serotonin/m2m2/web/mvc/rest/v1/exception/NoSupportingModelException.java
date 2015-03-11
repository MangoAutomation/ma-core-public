/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.exception;

import java.io.IOException;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Terry Packer
 *
 */
public class NoSupportingModelException extends IOException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Class<?> clazz;
	
	/**
	 * @param clazz2
	 */
	public NoSupportingModelException(Class<?> clazz) {
		this.clazz = clazz;
	}

	@Override
	public String getMessage(){
		return new TranslatableMessage("rest.exception.noSupportingModel", this.clazz.getCanonicalName()).translate(Common.getTranslations());
	}
	

}
