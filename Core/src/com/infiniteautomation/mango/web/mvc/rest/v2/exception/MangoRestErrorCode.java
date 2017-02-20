/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.web.mvc.rest.v2.exception;

/**
 * Error Codes for Mango Rest Errors
 * 
 * 4000 Series are for Bad Request Http Status
 * 
 * 5000 Series are for Internal Server Error Http Status
 * 
 * 
 * @author Terry Packer
 */
public enum MangoRestErrorCode {
	
	//***** 4000 Series *****
	
	RQL_PARSE_FAILURE(4001),
	VALIDATION_FAILED(4002),
	
	//***** 5000 Series *****
	GENERIC_500(5000);
	
	private final int code;
	
	private MangoRestErrorCode(int code){
		this.code = code;
	}
	
	public int getCode(){
		return this.code;
	}
}
