/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.message;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Terry Packer
 *
 */
public class RestProcessResult<T> {

	private HttpHeaders headers;
	private List<RestMessage> restMessages;
	private HttpStatus highestStatus; //Higher numbers indicate errors
	
	/**
	 * Create a Result with Ok status
	 */
	public RestProcessResult(){
		this.headers = new HttpHeaders();
		this.restMessages = new ArrayList<RestMessage>();
		this.highestStatus = HttpStatus.CONTINUE; //Lowest level
	}
	
	/**
	 * @param ok
	 */
	public RestProcessResult(HttpStatus status) {
		this.headers = new HttpHeaders();
		this.restMessages = new ArrayList<RestMessage>();
		this.highestStatus = status;
	}

	public void addRestMessage(RestMessage message){
	
		this.restMessages.add(message);
		
		//Save the highest status
		if(message.getStatus().value() > this.highestStatus.value())
			this.highestStatus = message.getStatus();
		
		if(message instanceof ResourceCreatedMessage){
			ResourceCreatedMessage msg = (ResourceCreatedMessage)message;
			this.headers.setLocation(msg.getLocation());
		}
		
	}
	
	/**
	 * Add a generic Message
	 * @param status
	 * @param message
	 */
	public void addRestMessage(HttpStatus status, TranslatableMessage message){
		this.addRestMessage(new RestMessage(status, message));
	}

	public List<RestMessage> getRestMessages() {
		return restMessages;
	}
	
	
	/**
	 * @return
	 */
	public boolean hasErrors() {
		if(highestStatus.value() >= 400)
			return true;
		else
			return false;
	}
	
	public HttpStatus getHighestStatus(){
		return this.highestStatus;
	}
	
	public ResponseEntity<T> createResponseEntity(){
		return new ResponseEntity<T>(
				this.addMessagesToHeaders(headers),
				this.highestStatus);
	}

	public ResponseEntity<List<T>> createResponseEntity(List<T> body){
		return new ResponseEntity<List<T>>(
				body,
				this.addMessagesToHeaders(headers),
				this.highestStatus);
	}

	
	public ResponseEntity<T> createResponseEntity(T body){
		return new ResponseEntity<T>(
				body,
				this.addMessagesToHeaders(headers),
				this.highestStatus);
	}
	
	
	/**
	 * Create headers, adding errors if necessary
	 * 
	 * @param response
	 * @return
	 */
	protected HttpHeaders addMessagesToHeaders(HttpHeaders headers) {
		
			StringBuilder headerErrors = new StringBuilder();
			StringBuilder headerMessages = new StringBuilder();
			
			for (int i=0; i<this.restMessages.size(); i++) {
				RestMessage message = this.restMessages.get(i);

				if(message.getStatus().value() >= 400){
					headerErrors.append(message.getMessage());
					if(i < this.restMessages.size() - 1)
						headerErrors.append(" ");
				}else{
					headerMessages.append(message.getMessage());
					if(i < this.restMessages.size() - 1)
						headerMessages.append(" ");
				}
				
				
				
			}
			
			//Always add, even if empty
			headers.add("messages", headerMessages.toString());
			headers.add("errors", headerErrors.toString());
			
			return headers;
	}

	/**
	 * @return
	 */
	public boolean isOk() {
		return this.highestStatus == HttpStatus.OK;
	}

	/**
	 * @param loginDefaultUriHeader
	 * @param uri
	 */
	public void addHeader(String headerName, String headerValue) {
		this.headers.add(headerName, headerValue);
	}
	
}
