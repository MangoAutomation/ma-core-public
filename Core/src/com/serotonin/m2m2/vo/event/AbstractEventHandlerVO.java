/*
    Copyright (C) 2016 Infinite Automation Systems Inc. All rights reserved.
    @author Terry Packer
 */
package com.serotonin.m2m2.vo.event;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.util.LazyField;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventHandlerDefinition;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.AbstractVO;

public abstract class AbstractEventHandlerVO extends AbstractVO {
    public static final String XID_PREFIX = "EH_";

    @JsonProperty
    private boolean disabled;

    @JsonProperty
    private LazyField<MangoPermission> readPermission = new LazyField<>(new MangoPermission());
    @JsonProperty
    private LazyField<MangoPermission> editPermission = new LazyField<>(new MangoPermission());

    private EventHandlerDefinition<? extends AbstractEventHandlerVO> definition;

    List<EventType> eventTypes = null;

    public TranslatableMessage getMessage() {
        if (!StringUtils.isBlank(name))
            return new TranslatableMessage("common.default", name);
        return getTypeMessage();
    }

    private TranslatableMessage getTypeMessage() {
        return new TranslatableMessage(this.definition.getDescriptionKey());
    }

    /**
     * Deprecated as we should just use the name. Leaving here as I believe these are probably accessed on the legacy page via DWR.
     */
    @Deprecated
    public String getAlias() {
        return name;
    }

    /**
     * Deprecated as we should just use the name. Leaving here as I believe these are probably accessed on the legacy page via DWR.
     * @param alias
     */
    public void setAlias(String alias) {
        setName(alias);
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public <T extends AbstractEventHandlerVO> EventHandlerDefinition<T> getDefinition() {
        return (EventHandlerDefinition<T>) definition;
    }

    public <T extends AbstractEventHandlerVO> void setDefinition(EventHandlerDefinition<T> definition) {
        this.definition = definition;
    }

    @Override
    public String getTypeKey() {
        return "event.audit.eventHandler";
    }

    public String getHandlerType(){
        return this.definition.getEventHandlerTypeName();
    }

    /**
     * @return the eventTypes
     */
    public List<EventType> getEventTypes() {
        return eventTypes;
    }

    /**
     * @param eventTypes the eventTypes to set
     */
    public void setEventTypes(List<EventType> eventTypes) {
        this.eventTypes = eventTypes;
    }

    public MangoPermission getReadPermission() {
        return readPermission.get();
    }

    public void setReadPermission(MangoPermission readPermission) {
        this.readPermission.set(readPermission);
    }

    public void supplyReadPermission(Supplier<MangoPermission> readPermission) {
        this.readPermission = new LazyField<MangoPermission>(readPermission);
    }

    public MangoPermission getEditPermission() {
        return editPermission.get();
    }

    public void setEditPermission(MangoPermission editPermission) {
        this.editPermission.set(editPermission);
    }

    public void supplyEditPermission(Supplier<MangoPermission> editPermission) {
        this.editPermission = new LazyField<MangoPermission>(editPermission);
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
        super.jsonWrite(writer);
        writer.writeEntry("handlerType", this.definition.getEventHandlerTypeName());
        writer.writeEntry("eventTypes", this.eventTypes);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        if(jsonObject.containsKey("alias")) {
            name = jsonObject.getString("alias");
            xid = jsonObject.getString("xid");
        }else {
            super.jsonRead(reader, jsonObject);
        }
    }
}
