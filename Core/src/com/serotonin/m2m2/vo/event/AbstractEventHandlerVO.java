/*
    Copyright (C) 2016 Infinite Automation Systems Inc. All rights reserved.
    @author Terry Packer
 */
package com.serotonin.m2m2.vo.event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.db.dao.AbstractDao;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventHandlerDefinition;
import com.serotonin.m2m2.rt.event.handlers.EventHandlerRT;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.events.handlers.AbstractEventHandlerModel;
import com.serotonin.validation.StringValidation;

public abstract class AbstractEventHandlerVO<T extends AbstractEventHandlerVO<T>> extends AbstractVO<T> {
    public static final String XID_PREFIX = "EH_";

    //TODO Replace this with superclass name,enabled when we redo the UI
    // caution about JSON emport
    @JsonProperty
    private String alias;
    @JsonProperty
    private boolean disabled;
    
    private EventHandlerDefinition<T> definition;
    
    List<EventType> addedEventTypes = null;

    /**
     * Create the runtime handler
     * @return
     */
    public abstract EventHandlerRT<?> createRuntime();
    
    /**
     * Return a model of this
     * @return
     */
    public abstract AbstractEventHandlerModel<?> asModel();

    public TranslatableMessage getMessage() {
        if (!StringUtils.isBlank(name))
            return new TranslatableMessage("common.default", name);
        return getTypeMessage();
    }

    private TranslatableMessage getTypeMessage() {
        return new TranslatableMessage(this.definition.getDescriptionKey());
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

	public EventHandlerDefinition<T> getDefinition() {
		return definition;
	}

	@SuppressWarnings("unchecked")
	public void setDefinition(EventHandlerDefinition<?> definition) {
		this.definition = (EventHandlerDefinition<T>) definition;
	}

	@Override
    public String getTypeKey() {
        return "event.audit.eventHandler";
    }

	public String getHandlerType(){
		return this.definition.getEventHandlerTypeName();
	}
	
	public void addEventType(EventType eventType) {
	    if(addedEventTypes == null)
	        addedEventTypes = new ArrayList<EventType>(1);
	    this.addedEventTypes.add(eventType);
	}
	
	public List<EventType> getAddedEventTypes() {
	    return addedEventTypes;
	}
	
    public void validate(ProcessResult response) {
    	//Not using name so don't super validate
    	if (StringUtils.isBlank(xid))
            response.addContextualMessage("xid", "validate.required");
        else if (StringValidation.isLengthGreaterThan(xid, 50))
            response.addMessage("xid", new TranslatableMessage("validate.notLongerThan", 50));
        else if (!isXidUnique(xid, id))
            response.addContextualMessage("xid", "validate.xidUsed");
    }

    @SuppressWarnings("unchecked")
	@Override
    protected AbstractDao<T> getDao(){
    	return (AbstractDao<T>)EventHandlerDao.instance;
    }
    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeBoolean(disabled);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            disabled = in.readBoolean();
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        writer.writeEntry("xid", xid);
        writer.writeEntry("handlerType", this.definition.getEventHandlerTypeName());
        writer.writeEntry("eventTypes", EventHandlerDao.instance.getEventTypesForHandler(id));
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
    	//Don't read xid
    }
}
