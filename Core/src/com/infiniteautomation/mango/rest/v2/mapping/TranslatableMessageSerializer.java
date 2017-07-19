/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2.mapping;

import java.io.IOException;
import java.util.Locale;

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

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
	 */
	@Override
	public void serialize(TranslatableMessage msg, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException {
		if(msg != null){
			User user = Common.getHttpUser();
			jgen.writeString(msg.translate(Translations.getTranslations(getLocale(user))));
		}else
			jgen.writeNull();
	}
	
	/**
	 * Get the local for a user if there isn't one use the System's Local
	 * @param user
	 * @return
	 */
	private Locale getLocale(User user) {
	    if (user != null) {
	        String localeStr = user.getLocale();
	        if (localeStr != null && !localeStr.isEmpty()) {
	            return Locale.forLanguageTag(user.getLocale());
	        }
	    }
        return Common.getLocale();
	}
}
