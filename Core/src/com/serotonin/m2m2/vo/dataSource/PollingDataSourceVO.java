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
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.DataSourceEventType;
import com.serotonin.m2m2.rt.event.type.DuplicateHandling;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.util.SerializationHelper;

/**
 * Base class for Polling type data sources
 *
 * @author Terry Packer
 *
 */
public abstract class PollingDataSourceVO extends DataSourceVO {

    protected final static String POLL_ABORTED = "POLL_ABORTED";

    @JsonProperty
    protected boolean quantize; //Start polls quantized to the start of the update period
    protected int updatePeriodType = Common.TimePeriods.MINUTES;
    protected int updatePeriods = 5;
    @JsonProperty
    protected boolean useCron = false;
    protected String cronPattern;

    public boolean isQuantize() {
        return quantize;
    }

    public void setQuantize(Boolean quantize) {
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

    public boolean isUseCron() {
        return useCron;
    }

    public void setUseCron(boolean useCron) {
        this.useCron = useCron;
    }

    public String getCronPattern() {
        return cronPattern;
    }

    public void setCronPattern(String cronPattern) {
        this.cronPattern = cronPattern;
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
    private EventTypeVO createPollAbortedEventType(int eventId) {
        AlarmLevels alarmLevel = getAlarmLevel(eventId, AlarmLevels.URGENT);
        return new EventTypeVO(
                new DataSourceEventType(getId(), eventId, alarmLevel, DuplicateHandling.IGNORE),
                new TranslatableMessage("event.ds.pollAborted"),
                alarmLevel);
    }

    /*
     * Serialization
     */
    private static final long serialVersionUID = 1L;
    private static final int version = 2;
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeInt(updatePeriodType);
        out.writeInt(updatePeriods);
        out.writeBoolean(useCron);
        SerializationHelper.writeSafeUTF(out, cronPattern);
        out.writeBoolean(quantize);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            updatePeriodType = in.readInt();
            updatePeriods = in.readInt();
            quantize = in.readBoolean();
            useCron = false;
        }else if(ver == 2) {
            updatePeriodType = in.readInt();
            updatePeriods = in.readInt();
            useCron = in.readBoolean();
            cronPattern = SerializationHelper.readSafeUTF(in);
            quantize = in.readBoolean();
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        if(useCron)
            writer.writeEntry("cronPattern", cronPattern);
        else {
            writer.writeEntry("updatePeriods", updatePeriods);
            writeUpdatePeriodType(writer, updatePeriodType);
        }
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);

        //The annotated field useCron will have already been imported
        if(jsonObject.containsKey("updatePeriodType")) {
            Integer value = readUpdatePeriodType(jsonObject);
            if (value != null)
                updatePeriodType = value;
        }

        if(jsonObject.containsKey("updatePeriods"))
            updatePeriods = getInt(jsonObject, "updatePeriods");

        if(jsonObject.containsKey("cronPattern"))
            cronPattern = jsonObject.getString("cronPattern");
    }

}
