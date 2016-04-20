/*
    Copyright (C) 2016 Infinite Automation Systems Inc. All rights reserved.
    @author Terry Packer
 */
package com.serotonin.m2m2.vo.event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventHandlerDefinition;
import com.serotonin.m2m2.rt.event.handlers.EventHandlerRT;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.util.ChangeComparable;

public abstract class AbstractEventHandlerVO implements Serializable, ChangeComparable<AbstractEventHandlerVO>, JsonSerializable {
    public static final String XID_PREFIX = "EH_";

    // Common fields
    private int id = Common.NEW_ID;
    private String xid;
    @JsonProperty
    private String alias;
    @JsonProperty
    private boolean disabled;
    
    private EventHandlerDefinition definition;

    /**
     * Create the runtime handler
     * @return
     */
    public abstract EventHandlerRT<?> createRuntime();

    public TranslatableMessage getMessage() {
        if (!StringUtils.isBlank(alias))
            return new TranslatableMessage("common.default", alias);
        return getTypeMessage();
    }

    private TranslatableMessage getTypeMessage() {
        return new TranslatableMessage(this.definition.getDescriptionKey());
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
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

    public EventHandlerDefinition getDefinition() {
		return definition;
	}

	public void setDefinition(EventHandlerDefinition definition) {
		this.definition = definition;
	}

	@Override
    public String getTypeKey() {
        return "event.audit.eventHandler";
    }

	//For DWR Use
	public String getHandlerType(){
		return this.definition.getEventHandlerTypeName();
	}
	
    public void validate(ProcessResult response) {
    }

    @Override
    public void addProperties(List<TranslatableMessage> list) {
        AuditEventType.addPropertyMessage(list, "common.xid", xid);
        AuditEventType.addPropertyMessage(list, "eventHandlers.alias", alias);
        AuditEventType.addPropertyMessage(list, "eventHandlers.type", getTypeMessage());
        AuditEventType.addPropertyMessage(list, "common.disabled", disabled);
    }

    @Override
    public void addPropertyChanges(List<TranslatableMessage> list, AbstractEventHandlerVO from) {
        AuditEventType.maybeAddPropertyChangeMessage(list, "common.xid", from.xid, xid);
        AuditEventType.maybeAddPropertyChangeMessage(list, "eventHandlers.alias", from.alias, alias);
        AuditEventType.maybeAddPropertyChangeMessage(list, "common.disabled", from.disabled, disabled);
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
        writer.writeEntry("eventType", new EventDao().getEventHandlerType(id));
        writer.writeEntry("xid", xid);
        writer.writeEntry("handlerType", this.definition.getEventHandlerTypeName());
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
    }
}
