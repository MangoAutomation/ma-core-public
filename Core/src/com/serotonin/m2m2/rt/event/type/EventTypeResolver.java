/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.event.type;

import java.lang.reflect.Type;

import com.serotonin.json.JsonException;
import com.serotonin.json.spi.TypeResolver;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.EventType.EventTypeNames;

public class EventTypeResolver implements TypeResolver {
    @Override
    public Type resolve(JsonValue jsonValue) throws JsonException {
        if (jsonValue == null)
            throw new TranslatableJsonException("emport.error.eventType.null");

        JsonObject json = jsonValue.toJsonObject();

        String text = json.getString("sourceType");
        if (text == null)
            throw new TranslatableJsonException("emport.error.eventType.missing", "sourceType",
                    EventType.SOURCE_NAMES.getCodeList());

        if (!EventType.SOURCE_NAMES.hasCode(text))
            throw new TranslatableJsonException("emport.error.eventType.invalid", "sourceType", text,
                    EventType.SOURCE_NAMES.getCodeList());

        if (text.equalsIgnoreCase(EventTypeNames.DATA_POINT))
            return DataPointEventType.class;
        if (text.equalsIgnoreCase(EventTypeNames.DATA_SOURCE))
            return DataSourceEventType.class;
        if (text.equalsIgnoreCase(EventTypeNames.SYSTEM))
            return SystemEventType.class;
        if (text.equalsIgnoreCase(EventTypeNames.PUBLISHER))
            return PublisherEventType.class;
        if (text.equalsIgnoreCase(EventTypeNames.AUDIT))
            return AuditEventType.class;

        EventTypeDefinition def = ModuleRegistry.getEventTypeDefinition(text);
        if (def != null)
            return def.getEventTypeClass();

        return null;
    }
}
