/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.event.detector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.util.LazyField;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.detectors.AbstractEventDetectorRT;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;

/**
 * @author Terry Packer
 *
 */
public abstract class AbstractEventDetectorVO extends AbstractVO {

    private static final long serialVersionUID = 1L;

    public static final String XID_PREFIX = "ED_";

    @JsonProperty
    private LazyField<MangoPermission> editPermission = new LazyField<>(new MangoPermission());
    @JsonProperty
    private LazyField<MangoPermission> readPermission = new LazyField<>(new MangoPermission());
    @JsonProperty
    private JsonNode data;

    /* Source of the detector */
    protected int sourceId;

    protected AlarmLevels alarmLevel = AlarmLevels.NONE;

    /**
     * Handlers that will be added to this detector upon save.
     * Used for JSON Imports
     */
    private List<AbstractEventHandlerVO> addedEventHandlers = null;

    /**
     * All event handlers that map to this detector.
     * When non-null this will replace all mappings for this detector <--> handlers
     */
    private LazyField<List<String>> eventHandlerXids = new LazyField<>();

    /**
     * Our defintion
     */
    protected EventDetectorDefinition<? extends AbstractEventDetectorVO> definition;

    /**
     * What event type do we generate
     * @return
     */
    public abstract EventTypeVO getEventType();


    /**
     * Create the runtime
     * @return
     */
    public abstract AbstractEventDetectorRT<? extends AbstractEventDetectorVO> createRuntime();

    /**
     * Is our event Rtn Applicable?
     * @return
     */
    public abstract boolean isRtnApplicable();

    /**
     * Return the configuration description for this handlers
     * @return
     */
    protected abstract TranslatableMessage getConfigurationDescription();

    public AlarmLevels getAlarmLevel() {
        return alarmLevel;
    }

    public void setAlarmLevel(AlarmLevels alarmLevel) {
        this.alarmLevel = alarmLevel;
    }

    /**
     * Our type name defintion
     * @return
     */
    public String getDetectorType(){
        return this.definition.getEventDetectorTypeName();
    }

    /**
     * Our source type name
     * @return
     */
    public String getDetectorSourceType(){
        return this.definition.getSourceTypeName();
    }

    public TranslatableMessage getDescription() {
        if (!StringUtils.isBlank(name))
            return new TranslatableMessage("common.default", name);
        return getConfigurationDescription();
    }

    @Override
    public String getTypeKey(){
        return "event.audit.pointEventDetector";
    }

    /**
     * Deprecated as we should just use the name. Leaving here as I believe these are probably accessed on the legacy page via DWR.
     * @param alias
     */
    @Deprecated
    public String getAlias() {
        return name;
    }

    /**
     * Deprecated as we should just use the name. Leaving here as I believe these are probably accessed on the legacy page via DWR.
     * @param alias
     */
    @Deprecated
    public void setAlias(String alias) {
        this.name = alias;
    }

    public MangoPermission getEditPermission() {
        return editPermission.get();
    }

    public void setEditPermission(MangoPermission editPermission) {
        this.editPermission.set(editPermission);
    }

    public void supplyEditPermission(Supplier<MangoPermission> editPermission) {
        this.editPermission = new LazyField<>(editPermission);
    }

    public MangoPermission getReadPermission() {
        return readPermission.get();
    }

    public void setReadPermission(MangoPermission readPermission) {
        this.readPermission.set(readPermission);
    }

    public void supplyReadPermission(Supplier<MangoPermission> readPermission) {
        this.readPermission = new LazyField<>(readPermission);
    }

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }

    public int getSourceId(){
        return this.sourceId;
    }
    public void setSourceId(int id){
        sourceId = id;
    }
    public void addAddedEventHandler(AbstractEventHandlerVO eventHandler) {
        if(addedEventHandlers == null)
            addedEventHandlers = new ArrayList<>();
        addedEventHandlers.add(eventHandler);
    }
    public List<AbstractEventHandlerVO> getAddedEventHandlers() {
        return addedEventHandlers;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractEventDetectorVO> EventDetectorDefinition<T> getDefinition() {
        return (EventDetectorDefinition<T>) definition;
    }

    public <T extends AbstractEventDetectorVO> void setDefinition(EventDetectorDefinition<T> definition) {
        this.definition = definition;
    }

    /**
     * @return the eventHandlerXids
     */
    public List<String> getEventHandlerXids() {
        return eventHandlerXids.get();
    }

    /**
     * @param eventHandlerXids the eventHandlerXids to set
     */
    public void setEventHandlerXids(List<String> eventHandlerXids) {
        this.eventHandlerXids.set(eventHandlerXids);
    }

    public void supplyEventHandlerXids(Supplier<List<String>> eventHandlerXids) {
        this.eventHandlerXids = new LazyField<>(eventHandlerXids);
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        writer.writeEntry("type", this.definition.getEventDetectorTypeName());
        writer.writeEntry("sourceType", this.definition.getSourceTypeName());
        writer.writeEntry("xid", xid);
        writer.writeEntry("name", name);
        writer.writeEntry("alarmLevel", alarmLevel.name());

        /* Event handler references are not exported here because there would be a circular dependency
         *  with the eventTypes array in the handler, and since there are other event types that was deemed
         *  the more versatile. One can create handler mappings through this array, but you cannot have both
         *  items refer to one another in the JSON if both are new, so this is not exported.
         */
        //writer.writeEntry("handlers", EventHandlerDao.getInstance().getEventHandlerXids(getEventType()));
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        String text = jsonObject.getString("name");
        if(text != null)
            name = text;
        else {
            text = jsonObject.getString("alias");
            if(text != null)
                name = text;
        }

        text = jsonObject.getString("alarmLevel");
        if (text != null) {
            try {
                alarmLevel = AlarmLevels.fromName(text);
            } catch (IllegalArgumentException e) {
                throw new TranslatableJsonException("emport.error.ped.invalid", "alarmLevel", text,
                        Arrays.asList(AlarmLevels.values()));
            }
        }
    }
}
