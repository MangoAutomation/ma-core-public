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
			Locale locale = user == null ? Common.getLocale() : user.getLocaleObject();
			jgen.writeString(msg.translate(Translations.getTranslations(locale)));
		}else
			jgen.writeNull();
	}
}
