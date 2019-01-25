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
import com.serotonin.m2m2.db.dao.EventHandlerDao;
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
        }else {
            //We want to only add event types via import so load existing in first
            handler.setEventTypes(EventHandlerDao.getInstance().getEventTypesForHandler(handler.getId()));
        }

        JsonObject et = json.getJsonObject("eventType");
        JsonArray ets = json.getJsonArray("eventTypes");
        
        try {
            ctx.getReader().readInto(handler, json);
            
            List<EventType> eventTypes = handler.getEventTypes();
            if(eventTypes == null) {
                eventTypes = new ArrayList<>();
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
                handler.setEventTypes(eventTypes);

            // Now validate it. Use a new response object so we can distinguish errors in this vo from other errors.
            ProcessResult voResponse = new ProcessResult();
            handler.validate(voResponse);
            if (voResponse.getHasMessages())
                setValidationMessages(voResponse, "emport.eventHandler.prefix", xid);
            else {
                // Sweet.
                boolean isnew = handler.getId() == Common.NEW_ID;
                // Save it.
                ctx.getEventHandlerDao().saveFull(handler);
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
