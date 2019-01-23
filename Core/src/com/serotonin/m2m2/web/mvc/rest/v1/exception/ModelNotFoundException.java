/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.exception;

import com.infiniteautomation.mango.util.exception.TranslatableRuntimeException;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Terry Packer
 *
 */
public class ModelNotFoundException extends TranslatableRuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6126782892194564463L;
	
	public ModelNotFoundException(String modelType){
		super(new TranslatableMessage("rest.exception.modelNotFound", modelType));
	}

}
