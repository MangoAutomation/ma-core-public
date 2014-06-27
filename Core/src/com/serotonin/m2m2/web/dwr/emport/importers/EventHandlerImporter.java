package com.serotonin.m2m2.web.dwr.emport.importers;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.event.EventHandlerVO;
import com.serotonin.m2m2.web.dwr.emport.Importer;

public class EventHandlerImporter extends Importer {
    public EventHandlerImporter(JsonObject json) {
        super(json);
    }

    @Override
    protected void importImpl() {
        String xid = json.getString("xid");
        if (StringUtils.isBlank(xid))
            xid = ctx.getEventDao().generateUniqueXid();

        EventHandlerVO handler = ctx.getEventDao().getEventHandler(xid);
        if (handler == null) {
            handler = new EventHandlerVO();
            handler.setXid(xid);
        }

        try {
            // Find the event type.
            EventType eventType = ctx.getReader().read(EventType.class, json.getJsonObject("eventType"));

            ctx.getReader().readInto(handler, json);

            // Now validate it. Use a new response object so we can distinguish errors in this vo from other errors.
            ProcessResult voResponse = new ProcessResult();
            handler.validate(voResponse);
            if (voResponse.getHasMessages())
                setValidationMessages(voResponse, "emport.eventHandler.prefix", xid);
            else {
                // Sweet.
                boolean isnew = handler.getId() == Common.NEW_ID;

                if (!isnew) {
                    // Check if the event type has changed.
                    EventType oldEventType = ctx.getEventDao().getEventHandlerType(handler.getId());
                    if (!oldEventType.equals(eventType)) {
                        // Event type has changed. Delete the old one.
                        ctx.getEventDao().deleteEventHandler(handler.getId());

                        // Call it new
                        handler.setId(Common.NEW_ID);
                        isnew = true;
                    }
                }

                // Save it.
                ctx.getEventDao().saveEventHandler(eventType, handler);
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
