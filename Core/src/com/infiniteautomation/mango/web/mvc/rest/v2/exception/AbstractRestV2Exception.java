/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.web.mvc.rest.v2.exception;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Base Class for Mango Rest Exceptions
 * 
 * @author Terry Packer
 */
@JsonIgnoreProperties("suppressed")
public abstract class AbstractRestV2Exception extends RuntimeException{

	private static final long serialVersionUID = 1L;
	//Code for Error (may be HTTP code or Custom Mango Error Code?)
	protected HttpStatus httpCode;
	protected MangoRestErrorCode mangoCode;
	protected TranslatableMessage translatableMessage;
	
	/**
	 * @param httpCode
	 * @param mangoCode
	 * @param message
	 */
	public AbstractRestV2Exception(HttpStatus httpCode, MangoRestErrorCode mangoCode, TranslatableMessage message) {
		this.httpCode = httpCode;
		this.mangoCode = mangoCode;
		this.translatableMessage = message;
	}
	
	public AbstractRestV2Exception(HttpStatus httpCode, MangoRestErrorCode mangoCode, Exception e){
		super(e);
		this.httpCode = httpCode;
		this.mangoCode = mangoCode;
		this.translatableMessage = new TranslatableMessage("common.default", e.getMessage());
	}
	
	@JsonIgnore
	public HttpStatus getStatus(){
		return this.httpCode;
	}
	
	public int getHttpStatusCode(){
		return httpCode.value();
	}
	
	public int getMangoStatusCode(){
		if(mangoCode != null)
			return mangoCode.getCode();
		else
			return -1;
	}
	
	public String getMangoStatusName(){
		if(mangoCode != null)
			return mangoCode.name();
		else
			return null;
	}
	
	public TranslatableMessage getTranslatableMessage(){
		return this.translatableMessage;
	}

	/* (non-Javadoc)
	 * @see java.lang.Throwable#getMessage()
	 */
	@JsonIgnore
	@Override
	public String getMessage() {
		if(this.translatableMessage != null)
			return this.translatableMessage.translate(Common.getTranslations());
		else
			return null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Throwable#getCause()
	 */
	@JsonIgnore
	@Override
	public synchronized Throwable getCause() {
		return super.getCause();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Throwable#getStackTrace()
	 */
	@JsonIgnore
	@Override
	public StackTraceElement[] getStackTrace() {
		// TODO Auto-generated method stub
		return super.getStackTrace();
	}
}
