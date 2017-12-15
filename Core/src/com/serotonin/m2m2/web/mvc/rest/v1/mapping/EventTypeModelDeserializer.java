/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.rest.v1.mapping;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.web.mvc.rest.v1.exception.ModelNotFoundException;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.AuditEventTypeModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.DataPointEventTypeModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.DataSourceEventTypeModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.EventTypeModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.MissingEventTypeModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.PublisherEventTypeModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.eventType.SystemEventTypeModel;

/**
 * 
 * @author Terry Packer
 */
public class EventTypeModelDeserializer extends StdDeserializer<EventTypeModel> {

    private static final long serialVersionUID = 1L;

    /**
     * @param vc
     */
    protected EventTypeModelDeserializer() {
        super(EventTypeModel.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.
     * JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
     */
    @Override
    public EventTypeModel deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        JsonNode tree = jp.readValueAsTree();
        String typeName = tree.get("typeName").asText();
        if (StringUtils.isEmpty(typeName))
            throw new ModelNotFoundException(typeName);

        EventTypeModel model = null;
        switch (typeName) {
            case EventType.EventTypeNames.DATA_POINT:
                model = mapper.treeToValue(tree, DataPointEventTypeModel.class);
                break;
            case EventType.EventTypeNames.DATA_SOURCE:
                model = mapper.treeToValue(tree, DataSourceEventTypeModel.class);
                break;
            case EventType.EventTypeNames.AUDIT:
                model = mapper.treeToValue(tree, AuditEventTypeModel.class);
                break;
            case EventType.EventTypeNames.PUBLISHER:
                model = mapper.treeToValue(tree, PublisherEventTypeModel.class);
                break;
            case EventType.EventTypeNames.SYSTEM:
                model = mapper.treeToValue(tree, SystemEventTypeModel.class);
                break;
            case EventType.EventTypeNames.MISSING:
                model = mapper.treeToValue(tree, MissingEventTypeModel.class);
                break;
        }
        if (model == null) {
            EventTypeDefinition def = ModuleRegistry.getEventTypeDefinition(typeName);
            if (def == null)
                throw new ModelNotFoundException(typeName);
            return (EventTypeModel) mapper.treeToValue(tree, def.getModelClass());
        } else
            return model;
    }
}
