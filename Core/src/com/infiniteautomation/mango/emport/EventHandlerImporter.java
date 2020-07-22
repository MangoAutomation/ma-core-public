package com.infiniteautomation.mango.emport;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.spring.service.EventHandlerService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.module.EventHandlerDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;

public class EventHandlerImporter extends Importer {

    private final EventHandlerService service;

    public EventHandlerImporter(JsonObject json, EventHandlerService service) {
        super(json);
        this.service = service;
    }

    @Override
    protected void importImpl() {
        AbstractEventHandlerVO handler = null;
        String xid = json.getString("xid");
        if (StringUtils.isBlank(xid)) {
            xid = service.generateUniqueXid();
        }else {
            try {
                handler = service.get(xid);
            }catch(NotFoundException e) {
                //Nothing, done below
            }
        }

        if (handler == null) {
            String typeStr = json.getString("handlerType");
            if (StringUtils.isBlank(typeStr))
                addFailureMessage("emport.eventHandler.missingType", xid, ModuleRegistry.getEventHandlerDefinitionTypes());
            else {
                EventHandlerDefinition<?> def = ModuleRegistry.getEventHandlerDefinition(typeStr);
                if (def == null)
                    addFailureMessage("emport.eventHandler.invalidType", xid, typeStr,
                            ModuleRegistry.getEventHandlerDefinitionTypes());
                else {
                    handler = def.baseCreateEventHandlerVO();
                    handler.setXid(xid);
                }
            }
        }

        JsonObject et = json.getJsonObject("eventType");
        JsonArray ets = json.getJsonArray("eventTypes");

        if(handler != null) {
            try {
                ctx.getReader().readInto(handler, json);

                Set<EventType> eventTypes;
                if(handler.getEventTypes() == null) {
                    eventTypes = new HashSet<>();
                }else {
                    eventTypes = new HashSet<>(handler.getEventTypes());
                }
                // Find the event type.
                if(et != null)
                    eventTypes.add(ctx.getReader().read(EventType.class, et));
                else if(ets != null) {
                    Iterator<JsonValue> iter = ets.iterator();
                    while(iter.hasNext())
                        eventTypes.add(ctx.getReader().read(EventType.class, iter.next()));
                }

                if(eventTypes.size() > 0)
                    handler.setEventTypes(new ArrayList<>(eventTypes));

                boolean isnew = handler.getId() == Common.NEW_ID;
                if(isnew) {
                    service.insert(handler);
                    addSuccessMessage(true, "emport.eventHandler.prefix", xid);
                }else {
                    service.update(handler.getId(), handler);
                    addSuccessMessage(false, "emport.eventHandler.prefix", xid);
                }

            }catch(ValidationException e) {
                setValidationMessages(e.getValidationResult(), "emport.eventHandler.prefix", xid);
            }
            catch (TranslatableJsonException e) {
                addFailureMessage("emport.eventHandler.prefix", xid, e.getMsg());
            }
            catch (JsonException e) {
                addFailureMessage("emport.eventHandler.prefix", xid, getJsonExceptionMessage(e));
            }
        }
    }
}
