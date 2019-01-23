/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.dataSource;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.DataSourceEventType;
import com.serotonin.m2m2.rt.event.type.DuplicateHandling;
import com.serotonin.m2m2.vo.event.EventTypeVO;

/**
 * Base class for Polling type data sources
 * 
 * @author Terry Packer
 *
 */
public abstract class PollingDataSourceVO<T extends PollingDataSourceVO<T>> extends DataSourceVO<T>{
    
    protected final static String POLL_ABORTED = "POLL_ABORTED";
    
    @JsonProperty 
    boolean quantize = false; //Start polls quantized to the start of the update period
    protected int updatePeriodType = Common.TimePeriods.MINUTES;
    @JsonProperty
    protected int updatePeriods = 5;
    
    @Override
    public void validate(ProcessResult response) {
        super.validate(response);
        if (!Common.TIME_PERIOD_CODES.isValidId(updatePeriodType))
            response.addContextualMessage("updatePeriodType", "validate.invalidValue");
        if (updatePeriods <= 0)
            response.addContextualMessage("updatePeriods", "validate.greaterThanZero");
    }

    public boolean isQuantize() {
        return quantize;
    }

    public void setQuantize(boolean quantize) {
        this.quantize = quantize;
    }

    public int getUpdatePeriods() {
        return updatePeriods;
    }

    public void setUpdatePeriods(int updatePeriods) {
        this.updatePeriods = updatePeriods;
    }

    public int getUpdatePeriodType() {
        return updatePeriodType;
    }

    public void setUpdatePeriodType(int updatePeriodType) {
        this.updatePeriodType = updatePeriodType;
    }
    
    /**
     * Allow Polling Data Sources to use an event per Data Source
     * if it exists.
     *
     * This should be overridden in that case.
     *
     * @return
     */
    public abstract int getPollAbortedExceptionEventId();
    
    @Override
    protected void addEventTypes(List<EventTypeVO> ets) {
         ets.add(createPollAbortedEventType(getPollAbortedExceptionEventId()));
    }
    
    /**
     * Useful for polling data sources
     *
     * Events are fired by the Polling Data Source RT
     *
     * @return
     */
    protected EventTypeVO createPollAbortedEventType(int eventId) {
        AlarmLevels alarmLevel = getAlarmLevel(eventId, AlarmLevels.URGENT);
        return new EventTypeVO(
                new DataSourceEventType(getId(), eventId, POLL_ABORTED, alarmLevel, DuplicateHandling.IGNORE),
                new TranslatableMessage("event.ds.pollAborted"),
                alarmLevel);
    }
    
    /*
     * Serialization 
     */
    private static final long serialVersionUID = 1L;
    private static final int version = 1;
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeInt(updatePeriodType);
        out.writeInt(updatePeriods);
        out.writeBoolean(quantize);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            updatePeriodType = in.readInt();
            updatePeriods = in.readInt();
            quantize = in.readBoolean();
        }
    }
    
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writeUpdatePeriodType(writer, updatePeriodType);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);
        Integer value = readUpdatePeriodType(jsonObject);
        if (value != null)
            updatePeriodType = value;
    }
   
}
