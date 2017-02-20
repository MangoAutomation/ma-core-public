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
	protected final HttpStatus httpCode;
	protected final MangoRestErrorCode mangoCode;
	protected final TranslatableMessage translatableMessage;
	
	public AbstractRestV2Exception(HttpStatus httpCode, MangoRestErrorCode mangoCode, TranslatableMessage message) {
		super(message.translate(Common.getTranslations()));
		this.httpCode = httpCode;
		this.mangoCode = mangoCode;
		this.translatableMessage = message;
	}
	
	public AbstractRestV2Exception(HttpStatus httpCode, TranslatableMessage message) {
		super(message.translate(Common.getTranslations()));
		this.httpCode = httpCode;
		this.mangoCode = null;
		this.translatableMessage = message;
	}

	
	public AbstractRestV2Exception(HttpStatus httpCode, MangoRestErrorCode mangoCode, Exception e){
		super(e);
		this.httpCode = httpCode;
		this.mangoCode = mangoCode;
		this.translatableMessage = null;
	}
	
	public AbstractRestV2Exception(HttpStatus httpCode, Exception e){
		super(e);
		this.httpCode = httpCode;
		this.mangoCode = null;
		this.translatableMessage = null;
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
			return super.getMessage();
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
		return super.getStackTrace();
	}
}
