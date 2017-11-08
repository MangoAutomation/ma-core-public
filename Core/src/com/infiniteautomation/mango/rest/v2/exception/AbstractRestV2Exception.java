/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2.exception;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Base Class for Mango Rest Exceptions
 * 
 * @author Terry Packer
 */
@JsonIgnoreProperties({"stackTrace", "message", "suppressed"})
public abstract class AbstractRestV2Exception extends RuntimeException {

	private static final long serialVersionUID = 1L;
	//Code for Error (may be HTTP code or Custom Mango Error Code?)
	protected final HttpStatus httpCode;
	protected final IMangoRestErrorCode mangoCode;
	protected final TranslatableMessage translatableMessage;
	
	public AbstractRestV2Exception(HttpStatus httpCode) {
        this(httpCode, null, new TranslatableMessage("httpStatus." + httpCode.value()));
	}
	
    public AbstractRestV2Exception(HttpStatus httpCode, Exception e) {
        this(httpCode, null, new TranslatableMessage("httpStatus." + httpCode.value()), e);
    }

	public AbstractRestV2Exception(HttpStatus httpCode, IMangoRestErrorCode mangoCode) {
	    this(httpCode, mangoCode, new TranslatableMessage("httpStatus." + httpCode.value()));
	}

    public AbstractRestV2Exception(HttpStatus httpCode, IMangoRestErrorCode mangoCode, Exception e) {
        this(httpCode, mangoCode, new TranslatableMessage("httpStatus." + httpCode.value()), e);
    }

    public AbstractRestV2Exception(HttpStatus httpCode, IMangoRestErrorCode mangoCode, TranslatableMessage message) {
        this.httpCode = httpCode;
        this.mangoCode = mangoCode;
        this.translatableMessage = message;
    }
    
	public AbstractRestV2Exception(HttpStatus httpCode, IMangoRestErrorCode mangoCode, TranslatableMessage message, Exception e) {
		super(e);
		this.httpCode = httpCode;
		this.mangoCode = mangoCode;
		this.translatableMessage = message;
	}

	@JsonIgnore
	public HttpStatus getStatus(){
		return this.httpCode;
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

    @JsonProperty("cause")
    public String getCauseMessage() {
        Throwable cause = this.getCause();
        if (cause != null) {
            return cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        return null;
    }
    
	@JsonProperty("localizedMessage")
    public TranslatableMessage getTranslatableMessage() {
        return this.translatableMessage;
    }

	@Override
	public String getMessage() {
	    return this.translatableMessage.translate(Common.getTranslations());
	}
}
