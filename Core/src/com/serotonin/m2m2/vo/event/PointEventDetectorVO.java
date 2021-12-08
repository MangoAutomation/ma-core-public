/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.detectors.PointEventDetectorRT;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.view.ImplDefinition;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Terry Packer
 *
 */
@Deprecated
public class PointEventDetectorVO extends SimpleEventDetectorVO<PointEventDetectorVO>{
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public static final String XID_PREFIX = "PED_";

    public static final int TYPE_ANALOG_HIGH_LIMIT = 1;
    public static final int TYPE_ANALOG_LOW_LIMIT = 2;
    public static final int TYPE_BINARY_STATE = 3;
    public static final int TYPE_MULTISTATE_STATE = 4;
    public static final int TYPE_POINT_CHANGE = 5;
    public static final int TYPE_STATE_CHANGE_COUNT = 6;
    public static final int TYPE_NO_CHANGE = 7;
    public static final int TYPE_NO_UPDATE = 8;
    public static final int TYPE_ALPHANUMERIC_STATE = 9;
    public static final int TYPE_POSITIVE_CUSUM = 10;
    public static final int TYPE_NEGATIVE_CUSUM = 11;
    public static final int TYPE_ALPHANUMERIC_REGEX_STATE = 12;
    public static final int TYPE_ANALOG_RANGE = 13;
    public static final int TYPE_ANALOG_CHANGE = 14;
    public static final int TYPE_SMOOTHNESS = 15;

    private static List<ImplDefinition> definitions;

    private static void initializeDefinitions() {
        if (definitions == null) {
            List<ImplDefinition> d = new ArrayList<>();
            d.add(new ImplDefinition(TYPE_ANALOG_HIGH_LIMIT, null, "pointEdit.detectors.highLimit",
                    EnumSet.of(DataTypes.NUMERIC)));
            d.add(new ImplDefinition(TYPE_ANALOG_LOW_LIMIT, null, "pointEdit.detectors.lowLimit",
                    EnumSet.of(DataTypes.NUMERIC)));
            d.add(new ImplDefinition(TYPE_POINT_CHANGE, null, "pointEdit.detectors.change", EnumSet.of(
                    DataTypes.BINARY, DataTypes.MULTISTATE, DataTypes.NUMERIC, DataTypes.ALPHANUMERIC)));
            d.add(new ImplDefinition(TYPE_BINARY_STATE, null, "pointEdit.detectors.state",
                    EnumSet.of(DataTypes.BINARY)));
            d.add(new ImplDefinition(TYPE_MULTISTATE_STATE, null, "pointEdit.detectors.state",
                    EnumSet.of(DataTypes.MULTISTATE)));
            d.add(new ImplDefinition(TYPE_ALPHANUMERIC_STATE, null, "pointEdit.detectors.state",
                    EnumSet.of(DataTypes.ALPHANUMERIC)));
            d.add(new ImplDefinition(TYPE_ALPHANUMERIC_REGEX_STATE, null, "pointEdit.detectors.regexState",
                    EnumSet.of(DataTypes.ALPHANUMERIC)));
            d.add(new ImplDefinition(TYPE_STATE_CHANGE_COUNT, null, "pointEdit.detectors.changeCount", EnumSet.of(
                    DataTypes.BINARY, DataTypes.MULTISTATE, DataTypes.ALPHANUMERIC)));
            d.add(new ImplDefinition(TYPE_NO_CHANGE, null, "pointEdit.detectors.noChange", EnumSet.of(
                    DataTypes.BINARY, DataTypes.MULTISTATE, DataTypes.NUMERIC, DataTypes.ALPHANUMERIC)));
            d.add(new ImplDefinition(TYPE_NO_UPDATE, null, "pointEdit.detectors.noUpdate",
                    EnumSet.of(DataTypes.BINARY, DataTypes.MULTISTATE, DataTypes.NUMERIC, DataTypes.ALPHANUMERIC,
                            DataTypes.IMAGE)));
            d.add(new ImplDefinition(TYPE_POSITIVE_CUSUM, null, "pointEdit.detectors.posCusum",
                    EnumSet.of(DataTypes.NUMERIC)));
            d.add(new ImplDefinition(TYPE_NEGATIVE_CUSUM, null, "pointEdit.detectors.negCusum",
                    EnumSet.of(DataTypes.NUMERIC)));
            d.add(new ImplDefinition(TYPE_ANALOG_RANGE, null, "pointEdit.detectors.range",
                    EnumSet.of(DataTypes.NUMERIC)));
            //            d.add(new ImplDefinition(TYPE_ANALOG_CHANGE, null, "pointEdit.detectors.analogChange",
            //                    EnumSet.of( DataTypes.NUMERIC )));
            d.add(new ImplDefinition(TYPE_SMOOTHNESS, null, "pointEdit.detectors.smoothness",
                    EnumSet.of(DataTypes.NUMERIC)));
            definitions = d;
        }
    }

    public static List<ImplDefinition> getImplementations(DataTypes dataType) {
        initializeDefinitions();
        List<ImplDefinition> impls = new ArrayList<>();
        for (ImplDefinition def : definitions) {
            if (def.supports(dataType))
                impls.add(def);
        }
        return impls;
    }

    private int id;
    private String xid;
    @JsonProperty
    private String alias;
    private DataPointVO dataPoint;
    private int detectorType;
    private int alarmLevel;
    private double limit;
    private int duration;
    private int durationType = Common.TimePeriods.SECONDS;
    private boolean binaryState;
    private int multistateState;
    private int changeCount = 2;
    private String alphanumericState;
    private double weight;

    public EventTypeVO getEventType() {
        return new EventTypeVO(new DataPointEventType(dataPoint.getId(), id),
                getDescription(),
                AlarmLevels.fromValue(alarmLevel));
    }

    public ImplDefinition getDef() {
        initializeDefinitions();

        for (ImplDefinition def : definitions) {
            if (def.getId() == detectorType)
                return def;
        }
        return null;
    }

    public PointEventDetectorRT<?> createRuntime() {
        throw new ShouldNeverHappenException("Deprecated.");
    }

    public boolean isRtnApplicable() {
        return detectorType != TYPE_POINT_CHANGE;
    }

    public TranslatableMessage getDescription() {
        if (!StringUtils.isBlank(alias))
            return new TranslatableMessage("common.default", alias);
        return getConfigurationDescription();
    }

    private TranslatableMessage getConfigurationDescription() {
        TranslatableMessage message;
        TranslatableMessage durationDesc = getDurationDescription();
        if (detectorType == TYPE_ANALOG_HIGH_LIMIT) {

            if (binaryState) {
                //Check if Not above
                if (durationDesc == null)
                    message = new TranslatableMessage("event.detectorVo.highLimitNotHigher", dataPoint
                            .getTextRenderer().getText(limit, TextRenderer.HINT_SPECIFIC));
                else
                    message = new TranslatableMessage("event.detectorVo.highLimitNotHigherPeriod", dataPoint
                            .getTextRenderer().getText(limit, TextRenderer.HINT_SPECIFIC), durationDesc);
            }
            else {
                //Must be above
                if (durationDesc == null)
                    message = new TranslatableMessage("event.detectorVo.highLimit", dataPoint.getTextRenderer()
                            .getText(limit, TextRenderer.HINT_SPECIFIC));
                else
                    message = new TranslatableMessage("event.detectorVo.highLimitPeriod", dataPoint.getTextRenderer()
                            .getText(limit, TextRenderer.HINT_SPECIFIC), durationDesc);
            }
        }
        else if (detectorType == TYPE_ANALOG_LOW_LIMIT) {
            if (binaryState) {
                //Not below
                if (durationDesc == null)
                    message = new TranslatableMessage("event.detectorVo.lowLimitNotLower", dataPoint.getTextRenderer()
                            .getText(limit, TextRenderer.HINT_SPECIFIC));
                else
                    message = new TranslatableMessage("event.detectorVo.lowLimitNotLowerPeriod", dataPoint
                            .getTextRenderer().getText(limit, TextRenderer.HINT_SPECIFIC), durationDesc);
            }
            else {
                //Must be below
                if (durationDesc == null)
                    message = new TranslatableMessage("event.detectorVo.lowLimit", dataPoint.getTextRenderer().getText(
                            limit, TextRenderer.HINT_SPECIFIC));
                else
                    message = new TranslatableMessage("event.detectorVo.lowLimitPeriod", dataPoint.getTextRenderer()
                            .getText(limit, TextRenderer.HINT_SPECIFIC), durationDesc);
            }
        }
        else if (detectorType == TYPE_BINARY_STATE) {
            if (durationDesc == null)
                message = new TranslatableMessage("event.detectorVo.state", dataPoint.getTextRenderer().getText(
                        binaryState, TextRenderer.HINT_SPECIFIC));
            else
                message = new TranslatableMessage("event.detectorVo.statePeriod", dataPoint.getTextRenderer().getText(
                        binaryState, TextRenderer.HINT_SPECIFIC), durationDesc);
        }
        else if (detectorType == TYPE_MULTISTATE_STATE) {
            if (durationDesc == null)
                message = new TranslatableMessage("event.detectorVo.state", dataPoint.getTextRenderer().getText(
                        multistateState, TextRenderer.HINT_SPECIFIC));
            else
                message = new TranslatableMessage("event.detectorVo.statePeriod", dataPoint.getTextRenderer().getText(
                        multistateState, TextRenderer.HINT_SPECIFIC), durationDesc);
        }
        else if (detectorType == TYPE_POINT_CHANGE)
            message = new TranslatableMessage("event.detectorVo.change");
        else if (detectorType == TYPE_STATE_CHANGE_COUNT)
            message = new TranslatableMessage("event.detectorVo.changeCount", changeCount, durationDesc);
        else if (detectorType == TYPE_NO_CHANGE)
            message = new TranslatableMessage("event.detectorVo.noChange", durationDesc);
        else if (detectorType == TYPE_NO_UPDATE)
            message = new TranslatableMessage("event.detectorVo.noUpdate", durationDesc);
        else if (detectorType == TYPE_ALPHANUMERIC_STATE) {
            if (durationDesc == null)
                message = new TranslatableMessage("event.detectorVo.state", dataPoint.getTextRenderer().getText(
                        alphanumericState, TextRenderer.HINT_SPECIFIC));
            else
                message = new TranslatableMessage("event.detectorVo.statePeriod", dataPoint.getTextRenderer().getText(
                        alphanumericState, TextRenderer.HINT_SPECIFIC), durationDesc);
        }
        else if (detectorType == TYPE_ALPHANUMERIC_REGEX_STATE)
            message = new TranslatableMessage("pointEdit.detectors.regexState", durationDesc);
        else if (detectorType == TYPE_POSITIVE_CUSUM) {
            if (durationDesc == null)
                message = new TranslatableMessage("event.detectorVo.posCusum", dataPoint.getTextRenderer().getText(
                        limit, TextRenderer.HINT_SPECIFIC));
            else
                message = new TranslatableMessage("event.detectorVo.posCusumPeriod", dataPoint.getTextRenderer()
                        .getText(limit, TextRenderer.HINT_SPECIFIC), durationDesc);
        }
        else if (detectorType == TYPE_NEGATIVE_CUSUM) {
            if (durationDesc == null)
                message = new TranslatableMessage("event.detectorVo.negCusum", dataPoint.getTextRenderer().getText(
                        limit, TextRenderer.HINT_SPECIFIC));
            else
                message = new TranslatableMessage("event.detectorVo.negCusumPeriod", dataPoint.getTextRenderer()
                        .getText(limit, TextRenderer.HINT_SPECIFIC), durationDesc);
        }
        else if (detectorType == TYPE_ANALOG_RANGE) {

            //For within range
            if (binaryState) {
                if (durationDesc == null)
                    message = new TranslatableMessage("event.detectorVo.range", dataPoint.getTextRenderer().getText(
                            weight, TextRenderer.HINT_SPECIFIC), dataPoint.getTextRenderer().getText(limit,
                                    TextRenderer.HINT_SPECIFIC));
                else
                    message = new TranslatableMessage("event.detectorVo.rangePeriod", dataPoint.getTextRenderer()
                            .getText(weight, TextRenderer.HINT_SPECIFIC), dataPoint.getTextRenderer().getText(limit,
                                    TextRenderer.HINT_SPECIFIC), durationDesc);
            }
            else {
                //Outside of range
                if (durationDesc == null)
                    message = new TranslatableMessage("event.detectorVo.rangeOutside", dataPoint.getTextRenderer()
                            .getText(weight, TextRenderer.HINT_SPECIFIC), dataPoint.getTextRenderer().getText(limit,
                                    TextRenderer.HINT_SPECIFIC));
                else
                    message = new TranslatableMessage("event.detectorVo.rangeOutsidePeriod", dataPoint
                            .getTextRenderer().getText(weight, TextRenderer.HINT_SPECIFIC), dataPoint.getTextRenderer()
                            .getText(limit, TextRenderer.HINT_SPECIFIC), durationDesc);
            }
        }
        else if (detectorType == TYPE_SMOOTHNESS) {
            if (durationDesc == null)
                message = new TranslatableMessage("event.detectorVo.smoothness", dataPoint.getTextRenderer().getText(
                        limit, TextRenderer.HINT_SPECIFIC));
            else
                message = new TranslatableMessage("event.detectorVo.smoothnessPeriod", dataPoint.getTextRenderer()
                        .getText(limit, TextRenderer.HINT_SPECIFIC), durationDesc);
        }

        else
            throw new ShouldNeverHappenException("Unknown detector type: " + detectorType);

        return message;
    }

    public TranslatableMessage getDurationDescription() {
        if (duration == 0)
            return null;
        return Common.getPeriodDescription(durationType, duration);
    }

    @Override
    public PointEventDetectorVO copy() {
        try {
            return (PointEventDetectorVO) super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    //@Override
    @Override
    public String getTypeKey() {
        return "event.audit.pointEventDetector";
    }

    public DataPointVO njbGetDataPoint() {
        return dataPoint;
    }

    public void njbSetDataPoint(DataPointVO dataPoint) {
        this.dataPoint = dataPoint;
    }

    public int getAlarmLevel() {
        return alarmLevel;
    }

    public void setAlarmLevel(int alarmLevel) {
        this.alarmLevel = alarmLevel;
    }

    //@Override
    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getXid() {
        return xid;
    }

    @Override
    public void setXid(String xid) {
        this.xid = xid;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isBinaryState() {
        return binaryState;
    }

    public void setBinaryState(boolean binaryState) {
        this.binaryState = binaryState;
    }

    public int getChangeCount() {
        return changeCount;
    }

    public void setChangeCount(int changeCount) {
        this.changeCount = changeCount;
    }

    public int getDetectorType() {
        return detectorType;
    }

    public void setDetectorType(int detectorType) {
        this.detectorType = detectorType;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getDurationType() {
        return durationType;
    }

    public void setDurationType(int durationType) {
        this.durationType = durationType;
    }

    public double getLimit() {
        return limit;
    }

    public void setLimit(double limit) {
        this.limit = limit;
    }

    public int getMultistateState() {
        return multistateState;
    }

    public void setMultistateState(int multistateState) {
        this.multistateState = multistateState;
    }

    public String getAlphanumericState() {
        return alphanumericState;
    }

    public void setAlphanumericState(String alphanumericState) {
        this.alphanumericState = alphanumericState;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    private static final ExportCodes TYPE_CODES = new ExportCodes();
    static {
        TYPE_CODES.addElement(TYPE_ANALOG_HIGH_LIMIT, "HIGH_LIMIT");
        TYPE_CODES.addElement(TYPE_ANALOG_LOW_LIMIT, "LOW_LIMIT");
        TYPE_CODES.addElement(TYPE_BINARY_STATE, "BINARY_STATE");
        TYPE_CODES.addElement(TYPE_MULTISTATE_STATE, "MULTISTATE_STATE");
        TYPE_CODES.addElement(TYPE_POINT_CHANGE, "POINT_CHANGE");
        TYPE_CODES.addElement(TYPE_STATE_CHANGE_COUNT, "STATE_CHANGE_COUNT");
        TYPE_CODES.addElement(TYPE_NO_CHANGE, "NO_CHANGE");
        TYPE_CODES.addElement(TYPE_NO_UPDATE, "NO_UPDATE");
        TYPE_CODES.addElement(TYPE_ALPHANUMERIC_STATE, "ALPHANUMERIC_STATE");
        TYPE_CODES.addElement(TYPE_ALPHANUMERIC_REGEX_STATE, "ALPHANUMERIC_REGEX_STATE");
        TYPE_CODES.addElement(TYPE_POSITIVE_CUSUM, "POSITIVE_CUSUM");
        TYPE_CODES.addElement(TYPE_NEGATIVE_CUSUM, "NEGATIVE_CUSUM");
        TYPE_CODES.addElement(TYPE_ANALOG_RANGE, "RANGE");
        TYPE_CODES.addElement(TYPE_SMOOTHNESS, "SMOOTHNESS");
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        writer.writeEntry("xid", xid);
        writer.writeEntry("type", TYPE_CODES.getCode(detectorType));
        writer.writeEntry("alarmLevel", AlarmLevels.fromValue(alarmLevel).name());

        switch (detectorType) {
            case TYPE_ANALOG_HIGH_LIMIT:
                writer.writeEntry("limit", limit);
                addDuration(writer);
                writer.writeEntry("notHigher", binaryState);
                if (this.multistateState == 1) //Using reset limit
                    writer.writeEntry("resetLimit", weight);
                break;
            case TYPE_ANALOG_LOW_LIMIT:
                writer.writeEntry("limit", limit);
                addDuration(writer);
                writer.writeEntry("notLower", binaryState);
                if (this.multistateState == 1) //Using Reset Limit
                    writer.writeEntry("resetLimit", weight);
                break;
            case TYPE_BINARY_STATE:
                writer.writeEntry("state", binaryState);
                addDuration(writer);
                break;
            case TYPE_MULTISTATE_STATE:
                writer.writeEntry("state", multistateState);
                addDuration(writer);
                break;
            case TYPE_POINT_CHANGE:
                break;
            case TYPE_STATE_CHANGE_COUNT:
                writer.writeEntry("changeCount", changeCount);
                addDuration(writer);
                break;
            case TYPE_NO_CHANGE:
                addDuration(writer);
                break;
            case TYPE_NO_UPDATE:
                addDuration(writer);
                break;
            case TYPE_ALPHANUMERIC_STATE:
                writer.writeEntry("state", alphanumericState);
                addDuration(writer);
                break;
            case TYPE_ALPHANUMERIC_REGEX_STATE:
                writer.writeEntry("state", alphanumericState);
                addDuration(writer);
                break;
            case TYPE_POSITIVE_CUSUM:
                writer.writeEntry("limit", limit);
                writer.writeEntry("weight", weight);
                addDuration(writer);
                break;
            case TYPE_NEGATIVE_CUSUM:
                writer.writeEntry("limit", limit);
                writer.writeEntry("weight", weight);
                addDuration(writer);
                break;
            case TYPE_ANALOG_RANGE:
                writer.writeEntry("low", weight);
                writer.writeEntry("high", limit);
                writer.writeEntry("withinRange", binaryState);
                addDuration(writer);
                break;
            case TYPE_SMOOTHNESS:
                writer.writeEntry("limit", limit);
                writer.writeEntry("boxcar", changeCount);
                addDuration(writer);
                break;
        }
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        String text = jsonObject.getString("type");
        if (text == null)
            throw new TranslatableJsonException("emport.error.ped.missing", "type", TYPE_CODES.getCodeList());

        detectorType = TYPE_CODES.getId(text);
        if (!TYPE_CODES.isValidId(detectorType))
            throw new TranslatableJsonException("emport.error.ped.invalid", "type", text, TYPE_CODES.getCodeList());

        text = jsonObject.getString("alarmLevel");
        if (text != null) {
            try {
                alarmLevel = AlarmLevels.fromName(text).value();
            } catch(IllegalArgumentException | NullPointerException e) {
                throw new TranslatableJsonException("emport.error.ped.invalid", "alarmLevel", text,
                        Arrays.asList(AlarmLevels.values()));
            }
        }

        switch (detectorType) {
            case TYPE_ANALOG_HIGH_LIMIT:
                limit = getDouble(jsonObject, "limit");
                updateDuration(jsonObject);
                if (jsonObject.containsKey("notHigher"))
                    binaryState = getBoolean(jsonObject, "notHigher");
                else
                    binaryState = false;
                if (jsonObject.containsKey("resetLimit")) {
                    multistateState = 1;
                    weight = getDouble(jsonObject, "resetLimit");
                }
                break;
            case TYPE_ANALOG_LOW_LIMIT:
                limit = getDouble(jsonObject, "limit");
                updateDuration(jsonObject);
                if (jsonObject.containsKey("notLower"))
                    binaryState = getBoolean(jsonObject, "notLower");
                else
                    binaryState = false;
                if (jsonObject.containsKey("resetLimit")) {
                    multistateState = 1;
                    weight = getDouble(jsonObject, "resetLimit");
                }
                break;
            case TYPE_BINARY_STATE:
                binaryState = getBoolean(jsonObject, "state");
                updateDuration(jsonObject);
                break;
            case TYPE_MULTISTATE_STATE:
                multistateState = getInt(jsonObject, "state");
                updateDuration(jsonObject);
                break;
            case TYPE_POINT_CHANGE:
                break;
            case TYPE_STATE_CHANGE_COUNT:
                changeCount = getInt(jsonObject, "changeCount");
                updateDuration(jsonObject);
                break;
            case TYPE_NO_CHANGE:
                updateDuration(jsonObject);
                break;
            case TYPE_NO_UPDATE:
                updateDuration(jsonObject);
                break;
            case TYPE_ALPHANUMERIC_STATE:
                alphanumericState = getString(jsonObject, "state");
                updateDuration(jsonObject);
                break;
            case TYPE_ALPHANUMERIC_REGEX_STATE:
                alphanumericState = getString(jsonObject, "state");
                updateDuration(jsonObject);
                break;
            case TYPE_POSITIVE_CUSUM:
                limit = getDouble(jsonObject, "limit");
                weight = getDouble(jsonObject, "weight");
                updateDuration(jsonObject);
                break;
            case TYPE_NEGATIVE_CUSUM:
                limit = getDouble(jsonObject, "limit");
                weight = getDouble(jsonObject, "weight");
                updateDuration(jsonObject);
                break;
            case TYPE_ANALOG_RANGE:
                weight = getDouble(jsonObject, "low");
                limit = getDouble(jsonObject, "high");
                binaryState = getBoolean(jsonObject, "withinRange");
                break;
            case TYPE_SMOOTHNESS:
                limit = getDouble(jsonObject, "limit");
                changeCount = getInt(jsonObject, "boxcar");
                updateDuration(jsonObject);
                break;
        }
    }

    private void updateDuration(JsonObject json) throws JsonException {
        String text = json.getString("durationType");
        if (text == null)
            throw new TranslatableJsonException("emport.error.ped.missing", "durationType",
                    Common.TIME_PERIOD_CODES.getCodeList());

        durationType = Common.TIME_PERIOD_CODES.getId(text);
        if (!Common.TIME_PERIOD_CODES.isValidId(durationType))
            throw new TranslatableJsonException("emport.error.ped.invalid", "durationType", text,
                    Common.TIME_PERIOD_CODES.getCodeList());

        duration = getInt(json, "duration");
    }



    private void addDuration(ObjectWriter writer) throws JsonException, IOException {
        writer.writeEntry("durationType", Common.TIME_PERIOD_CODES.getCode(durationType));
        writer.writeEntry("duration", duration);
    }

    @Override
    public String getEventDetectorKey() {
        return SimpleEventDetectorVO.POINT_EVENT_DETECTOR_PREFIX + id;
    }
}
