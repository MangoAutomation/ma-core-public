/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2.mapping;

import java.io.IOException;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.vo.User;

/**
 * Serialize a TranslatableMessage into a useful model
 * @author Terry Packer
 */
public class TranslatableMessageSerializer extends JsonSerializer<TranslatableMessage>{

	private static final String KEY = "key";
	private static final String MESSAGE = "message";
	private static final String ARGS = "args";

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
	 */
	@Override
	public void serialize(TranslatableMessage msg, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException {
		
		if(msg != null){
			jgen.writeStartObject();
			User user = getCurrentUser();
			jgen.writeStringField(KEY, msg.getKey());
			jgen.writeStringField(MESSAGE, msg.translate(Translations.getTranslations(getLocale(user))));
			jgen.writeObjectField(ARGS, msg.getArgs());
			jgen.writeEndObject();
		}else
			jgen.writeNull();
		
	}
	

	/**
	 * Get the current user from the SecurityContext
	 * @return
	 */
	private User getCurrentUser(){
		//Check for the User via Spring Security
	    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	    if(auth != null){
	    	Object principle = auth.getPrincipal();
	    	if(principle != null){
	    		//At this point User could be of type AnonymousUser
	    		if(principle instanceof User)
	    			return (User)principle;
	    	}
	    }
	    
	    return null;
	}
	
	/**
	 * Get the local for a user if there isn't one use the System's Local
	 * @param user
	 * @return
	 */
	private Locale getLocale(User user){
		String localeStr;
		
		if(user != null){
			localeStr = user.getLocale();
			if(!StringUtils.isEmpty(localeStr)){
		        String[] parts = localeStr.split("_");
		        if (parts.length == 1)
		            return new Locale(parts[0]);
		        else if (parts.length == 2)
		            return new Locale(parts[0], parts[1]);
		        else if (parts.length == 3)
		            return new Locale(parts[0], parts[1], parts[2]);
			}
        	throw new IllegalArgumentException("Locale for given language not found: " + localeStr);	
		}else
			return Common.getLocale();
	}
	
}
