/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.converter;

import java.lang.reflect.Type;

import com.serotonin.json.JsonException;
import com.serotonin.json.spi.TypeResolver;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.i18n.TranslatableJsonException;

/**
 * @author Terry Packer
 *
 */
public class PointLocatorResolver implements TypeResolver{
	
	/* (non-Javadoc)
	 * @see com.serotonin.json.spi.TypeResolver#resolve(com.serotonin.json.type.JsonValue)
	 */
	@Override
	public Type resolve(JsonValue jsonValue) throws JsonException {
		
		JsonObject json = jsonValue.toJsonObject();

        String type = json.getString("type");
        if (type == null)
            throw new TranslatableJsonException("emport.error.text.missing", "type", PointLocatorRegistry.instance.getExportTypes());

        PointLocatorDefinition def = null;

        for (PointLocatorDefinition id : PointLocatorRegistry.instance.getDefinitions()) {
            if (id.getExportName().equalsIgnoreCase(type)) {
                def = id;
                break;
            }
        }

        if (def == null)
            throw new TranslatableJsonException("emport.error.text.invalid", "type", type, PointLocatorRegistry.instance.getExportTypes());
        else
        	return def.getTypeClass();

	}

}
