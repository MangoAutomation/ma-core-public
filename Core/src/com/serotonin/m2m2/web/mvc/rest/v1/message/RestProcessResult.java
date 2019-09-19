/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Terry Packer
 *
 */
public class RestProcessResult<T> {

    private static final Pattern CR_OR_LF = Pattern.compile("\\r|\\n");

    private HttpHeaders headers;
    private List<RestMessage> restMessages;
    private Map<String,String> validationMessages;
    private HttpStatus highestStatus; //Higher numbers indicate errors

    /**
     * Create a Result with Ok status
     */
    public RestProcessResult(){
        this.headers = new HttpHeaders();
        this.restMessages = new ArrayList<RestMessage>();
        this.highestStatus = HttpStatus.CONTINUE; //Lowest level
        this.validationMessages = null;
    }

    /**
     * @param ok
     */
    public RestProcessResult(HttpStatus status) {
        this.headers = new HttpHeaders();
        this.restMessages = new ArrayList<RestMessage>();
        this.highestStatus = status;
        this.validationMessages = null;
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

    /**
     * Create a response entity with many objects and
     * a custom Data Type
     * @param models
     * @param octetStream
     * @return
     */
    public ResponseEntity<List<T>> createResponseEntity(
            List<T> body,
            MediaType mediaType) {
        this.headers.setContentType(mediaType);
        return new ResponseEntity<List<T>>(
                body,
                this.addMessagesToHeaders(headers),
                this.highestStatus);
    }

    /**
     * Create a response entity containing one object
     * @param body
     * @return
     */
    public ResponseEntity<T> createResponseEntity(T body){
        return new ResponseEntity<T>(
                body,
                this.addMessagesToHeaders(headers),
                this.highestStatus);
    }

    /**
     * Create a response entity containing one object
     * and a specific content type
     * @param body
     * @return
     */
    public ResponseEntity<T> createResponseEntity(T body, MediaType mediaType){
        this.headers.setContentType(mediaType);
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
    public HttpHeaders addMessagesToHeaders(HttpHeaders headers) {

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
        headers.add("messages", stripAndTrimHeader(headerMessages.toString(), -1));
        headers.add("errors", stripAndTrimHeader(headerErrors.toString(), 200));

        return headers;
    }

    /**
     * @return
     */
    public boolean isOk() {
        return this.highestStatus.is2xxSuccessful();
    }

    /**
     * @param loginDefaultUriHeader
     * @param uri
     */
    public void addHeader(String headerName, String headerValue) {
        this.headers.add(headerName, headerValue);
    }

    /**
     * Add messages from a validation
     *
     * @param validation
     */
    public void addValidationMessages(ProcessResult validation) {

        if(this.validationMessages == null)
            this.validationMessages = new HashMap<String,String>();
        if(validation.getHasMessages()) {
            this.highestStatus = HttpStatus.UNPROCESSABLE_ENTITY;
            for(ProcessMessage message : validation.getMessages()){
                this.validationMessages.put(message.getContextKey(), message.getContextualMessage().translate(Common.getTranslations()));
            }
        }

    }

    public void addValidationMessage(ProcessMessage message){
        if(this.validationMessages == null)
            this.validationMessages = new HashMap<String,String>();
        this.highestStatus = HttpStatus.UNPROCESSABLE_ENTITY;
        this.validationMessages.put(message.getContextKey(), message.getContextualMessage().translate(Common.getTranslations()));
    }

    /**
     * @return
     */
    public Map<String,String> getValidationMessages() {
        return this.validationMessages;
    }

    public HttpHeaders getHeaders(){
        return this.headers;
    }
    public void setContentType(MediaType mediaType){
        this.headers.setContentType(mediaType);
    }


    /**
     * Util to remove CR/LF and ensure max length
     */
    public static String stripAndTrimHeader(String message, int maxLength) {
        message = StringUtils.strip(StringUtils.trimToEmpty(message));
        Matcher matcher = CR_OR_LF.matcher(message);
        if (matcher.find()) {
            message = message.substring(0, matcher.start());
        }

        if (maxLength < 0) {
            return message;
        }

        return StringUtils.truncate(message, maxLength);
    }
}
