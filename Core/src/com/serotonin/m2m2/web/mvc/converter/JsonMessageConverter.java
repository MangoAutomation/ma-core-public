/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.converter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;

import org.apache.log4j.Logger;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Message Converter to allow Use of Serotonin JSON Parser Core
 * 
 * 
 * 
 * @see org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
 * @author Terry Packer
 * 
 */
public class JsonMessageConverter extends AbstractHttpMessageConverter<Object> {
	private static Logger LOG = Logger.getLogger(JsonMessageConverter.class);
	
	public JsonMessageConverter() {
		super(new MediaType("application", "json", Common.UTF8_CS));
		
		//Quick hack to fix up 
		
		
	}

	/* (non-Javadoc)
	 * @see org.springframework.http.converter.AbstractHttpMessageConverter#supports(java.lang.Class)
	 */
	@Override
	protected boolean supports(Class<?> paramClass) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.http.converter.AbstractHttpMessageConverter#readInternal(java.lang.Class, org.springframework.http.HttpInputMessage)
	 */
	@Override
	protected Object readInternal(
			Class<? extends Object> clazz,
			HttpInputMessage inputMessage) throws IOException,
			HttpMessageNotReadableException {
		
		
		Reader inputReader = new BufferedReader(new InputStreamReader(inputMessage.getBody(), Common.UTF8_CS));
		 //JsonTypeReader reader = new JsonTypeReader(inputReader);
		 JsonReader reader = new JsonReader(Common.JSON_CONTEXT, inputReader);
	        try {
	        	return reader.read(clazz);
	        }
	        catch (ClassCastException e) {
	        	LOG.error(e);
            	throw new HttpMessageNotReadableException(translate("emport.parseError", e.getMessage()));
	        }
	        catch (TranslatableJsonException e) {
	        	LOG.error(e);
            	throw new HttpMessageNotReadableException(translate("emport.parseError", e.getMessage()));
	        }
	        catch (IOException e) {
	        	LOG.error(e);
            	throw new HttpMessageNotReadableException(translate("emport.parseError", e.getMessage()));
	        }
	        catch (JsonException e) {
	        	LOG.error(e);
            	throw new HttpMessageNotReadableException(translate("emport.parseError", e.getMessage()));
	        } 
		
	}

	/* (non-Javadoc)
	 * @see org.springframework.http.converter.AbstractHttpMessageConverter#writeInternal(java.lang.Object, org.springframework.http.HttpOutputMessage)
	 */
	@Override
	protected void writeInternal(Object object,
			HttpOutputMessage outputMessage) throws IOException,
			HttpMessageNotWritableException {
		
		//TODO See here for a fix for this:
		//http://wiki.fasterxml.com/JacksonMixInAnnotations
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputMessage.getBody()));
		JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, bw);
		//TODO Make optional somehow
        int prettyIndent = 3;
        writer.setPrettyIndent(prettyIndent);
        writer.setPrettyOutput(true);
        
		try {
			writer.writeObject(object);
			writer.flush();
			bw.close();
		} catch (JsonException e) {
			LOG.error(e);
			//Give it a try Via Jackson
			//General Catchall, TODO add to readInternal
			try{
				MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
				converter.write(object,MediaType.APPLICATION_JSON, outputMessage);
			}catch(Exception e2){
				LOG.error(e2);
				throw new HttpMessageNotWritableException(e.getMessage());
			}
		}
	}

	/**
	 * Translate a message
	 * @param key
	 * @param args
	 * @return
	 */
	private String translate(String key, Object... args){
		return new TranslatableMessage(key, args).translate(Common.getTranslations());
	}
	
	
}