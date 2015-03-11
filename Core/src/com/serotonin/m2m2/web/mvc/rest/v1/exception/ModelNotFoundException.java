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
public class ModelNotFoundException extends IOException {

	private String modelType;
	/**
	 * 
	 */
	private static final long serialVersionUID = 6126782892194564463L;
	
	public ModelNotFoundException(String modelType){
		this.modelType = modelType;
	}
	
	@Override
	public String getMessage(){
		return new TranslatableMessage("rest.exception.modelNotFound", this.modelType).translate(Common.getTranslations());
	}
	

}
