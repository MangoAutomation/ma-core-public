/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Terry Packer
 *
 */
public class BaseErrorModel {

	private String errorMessage;
	private List<String> stackTrace;
	
	public BaseErrorModel(){ }
	
	public BaseErrorModel(String message, List<String> stack){
		this.errorMessage = message;
		this.stackTrace = stack;
	}

	/**
	 * @param e
	 */
	public BaseErrorModel(Exception e) {
		this.errorMessage = e.getMessage();
		this.stackTrace = new ArrayList<String>();
		for(StackTraceElement element : e.getStackTrace()){
			this.stackTrace.add(element.toString());
		}
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public List<String> getStackTrace() {
		return stackTrace;
	}

	public void setStackTrace(List<String> stackTrace) {
		this.stackTrace = stackTrace;
	}
	
	
	
}
