/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.web.mvc.rest.v2.exception;

import org.springframework.http.HttpStatus;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * 
 * @author Terry Packer
 */
public class InvalidRQLRestException extends AbstractRestV2Exception{

	private static final long serialVersionUID = 1L;
	private String query;
	private String parserMessage;
	
	public InvalidRQLRestException(String query, String message){
		super(HttpStatus.BAD_REQUEST, MangoRestErrorCode.RQL_PARSE_FAILURE, new TranslatableMessage("common.invalidRql"));
		this.query = query;
		this.parserMessage = message;
	}
	
	public String getQuery(){
		return query;
	}
	
	public String getParserMessage(){
		return parserMessage;
	}
	
}
