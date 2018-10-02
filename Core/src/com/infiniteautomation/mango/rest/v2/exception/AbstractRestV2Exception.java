/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2.exception;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.infiniteautomation.mango.util.exception.TranslatableExceptionI;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Base Class for Mango Rest Exceptions
 *
 * @author Terry Packer
 */
@JsonIgnoreProperties({"stackTrace", "message", "suppressed"})
public abstract class AbstractRestV2Exception extends RuntimeException implements TranslatableExceptionI {

    private static final long serialVersionUID = 1L;
    //Code for Error (may be HTTP code or Custom Mango Error Code?)
    protected final HttpStatus httpCode;
    protected final IMangoRestErrorCode mangoCode;
    protected final TranslatableMessage translatableMessage;

    public AbstractRestV2Exception(HttpStatus httpCode) {
        this(httpCode, null, new TranslatableMessage("rest.httpStatus." + httpCode.value()));
    }

    public AbstractRestV2Exception(HttpStatus httpCode, Throwable cause) {
        this(httpCode, null, new TranslatableMessage("rest.httpStatus." + httpCode.value()), cause);
    }

    public AbstractRestV2Exception(HttpStatus httpCode, IMangoRestErrorCode mangoCode) {
        this(httpCode, mangoCode, new TranslatableMessage("rest.httpStatus." + httpCode.value()));
    }

    public AbstractRestV2Exception(HttpStatus httpCode, IMangoRestErrorCode mangoCode, Throwable cause) {
        this(httpCode, mangoCode, new TranslatableMessage("rest.httpStatus." + httpCode.value()), cause);
    }

    public AbstractRestV2Exception(HttpStatus httpCode, IMangoRestErrorCode mangoCode, TranslatableMessage message) {
        this.httpCode = httpCode;
        this.mangoCode = mangoCode;
        this.translatableMessage = message;
    }

    public AbstractRestV2Exception(HttpStatus httpCode, IMangoRestErrorCode mangoCode, TranslatableMessage message, Throwable cause) {
        super(cause);
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
            String causeInfo = cause.getClass().getSimpleName();
            if(!StringUtils.isEmpty(cause.getMessage()))
                return causeInfo + ": " + cause.getMessage();
            else
                return causeInfo;
        }
        return null;
    }

    @Override
    @JsonProperty("localizedMessage")
    public TranslatableMessage getTranslatableMessage() {
        return this.translatableMessage;
    }

    @Override
    public String getMessage() {
        return this.translatableMessage.translate(Common.getTranslations());
    }
}
