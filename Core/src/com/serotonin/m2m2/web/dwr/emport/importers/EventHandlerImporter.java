package com.serotonin.m2m2.web.dwr.emport.importers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.module.EventHandlerDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.web.dwr.emport.Importer;

public class EventHandlerImporter extends Importer {
    public EventHandlerImporter(JsonObject json) {
        super(json);
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");
        if (StringUtils.isBlank(xid))
            xid = ctx.getEventHandlerDao().generateUniqueXid();

        AbstractEventHandlerVO<?> handler = ctx.getEventHandlerDao().getEventHandler(xid);
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
        EventType eventType = null;
        List<EventType> eventTypes = null;
        
        try {
            // Find the event type.
            if(et != null)
                eventType = ctx.getReader().read(EventType.class, et);
            else if(ets != null) {
                eventTypes = new ArrayList<EventType>(ets.size());
                Iterator<JsonValue> iter = ets.iterator();
                while(iter.hasNext())
                    eventTypes.add(ctx.getReader().read(EventType.class, iter.next()));
            }

            ctx.getReader().readInto(handler, json);

            // Now validate it. Use a new response object so we can distinguish errors in this vo from other errors.
            ProcessResult voResponse = new ProcessResult();
            handler.validate(voResponse);
            if (voResponse.getHasMessages())
                setValidationMessages(voResponse, "emport.eventHandler.prefix", xid);
            else {
                // Sweet.
                boolean isnew = handler.getId() == Common.NEW_ID;

                //Commented out at decoupling handlers and detectors because in theory it could be a different eventType
                // and both should remain.
//                if (!isnew && et != null) {
//                    // Check if the event type has changed.
//                    EventType oldEventType = ctx.getEventHandlerDao().getEventType(handler.getId());
//                    if (!oldEventType.equals(eventType)) {
//                        // Event type has changed. Delete the old one.
//                        ctx.getEventHandlerDao().deleteEventHandler(handler.getId());
//
//                        // Call it new
//                        handler.setId(Common.NEW_ID);
//                        isnew = true;
//                    }
//                }

                // Save it.
                if(et != null)
                    ctx.getEventHandlerDao().saveEventHandler(eventType, handler);
                else
                    ctx.getEventHandlerDao().saveEventHandler(handler);
                
                if(eventTypes != null)
                    for(EventType type : eventTypes)
                        ctx.getEventHandlerDao().addEventHandlerMappingIfMissing(handler.getId(), type);
                
                addSuccessMessage(isnew, "emport.eventHandler.prefix", xid);
            }
        }
        catch (TranslatableJsonException e) {
            addFailureMessage("emport.eventHandler.prefix", xid, e.getMsg());
        }
        catch (JsonException e) {
            addFailureMessage("emport.eventHandler.prefix", xid, getJsonExceptionMessage(e));
        }
    }
}
