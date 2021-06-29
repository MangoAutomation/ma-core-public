/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo.publish;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.PublisherEventType;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.AbstractActionVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.util.SerializationHelper;

/**
 * @author Matthew Lohbihler
 */
abstract public class PublisherVO<T extends PublishedPointVO> extends AbstractActionVO implements Serializable, JsonSerializable {
    public static final String XID_PREFIX = "PUB_";

    public interface PublishType{
        int ALL = 1;
        int CHANGES_ONLY = 2;
        int LOGGED_ONLY = 3;
        int NONE = 4;
    }

    public static final ExportCodes PUBLISH_TYPE_CODES = new ExportCodes();
    static{
        PUBLISH_TYPE_CODES.addElement(PublishType.ALL, "ALL", "publisherEdit.publishType.all");
        PUBLISH_TYPE_CODES.addElement(PublishType.CHANGES_ONLY, "CHANGES_ONLY", "publisherEdit.publishType.changesOnly");
        PUBLISH_TYPE_CODES.addElement(PublishType.LOGGED_ONLY, "LOGGED_ONLY", "publisherEdit.publishType.loggedOnly");
        PUBLISH_TYPE_CODES.addElement(PublishType.NONE, "NONE", "publisherEdit.publishType.none");
    }

    /**
     * Get the description for this publisher configuration
     * @return
     */
    abstract public TranslatableMessage getConfigDescription();

    abstract protected T createPublishedPointInstance();

    abstract public PublisherRT<T> createPublisherRT();

    public List<EventTypeVO> getEventTypes() {
        List<EventTypeVO> eventTypes = new ArrayList<>();
        eventTypes
        .add(new EventTypeVO(new PublisherEventType(this, PublisherRT.POINT_DISABLED_EVENT),
                new TranslatableMessage("event.pb.pointMissing"),
                getAlarmLevel(PublisherRT.POINT_DISABLED_EVENT, AlarmLevels.URGENT)));
        eventTypes
        .add(new EventTypeVO(new PublisherEventType(this, PublisherRT.QUEUE_SIZE_WARNING_EVENT),
                new TranslatableMessage("event.pb.queueSize"),
                getAlarmLevel(PublisherRT.QUEUE_SIZE_WARNING_EVENT, AlarmLevels.URGENT)));

        getEventTypesImpl(eventTypes);

        return eventTypes;
    }

    abstract protected void getEventTypesImpl(List<EventTypeVO> eventTypes);

    protected static void addDefaultEventCodes(ExportCodes codes) {
        codes.addElement(PublisherRT.POINT_DISABLED_EVENT, "POINT_DISABLED_EVENT");
        codes.addElement(PublisherRT.QUEUE_SIZE_WARNING_EVENT, "QUEUE_SIZE_WARNING_EVENT");
    }

    abstract public ExportCodes getEventCodes();

    private PublisherDefinition<? extends PublisherVO<?>> definition;

    private Map<Integer, AlarmLevels> alarmLevels = new HashMap<>();

    public TranslatableMessage getTypeDescription() {
        return new TranslatableMessage(getDefinition().getDescriptionKey());
    }

    protected List<T> points = new ArrayList<>();
    private int publishType = PublishType.ALL;
    @JsonProperty
    private int cacheWarningSize = 100;
    @JsonProperty
    private int cacheDiscardSize = 1000;
    @JsonProperty
    private boolean sendSnapshot;
    private int snapshotSendPeriodType = Common.TimePeriods.MINUTES;
    @JsonProperty
    private int snapshotSendPeriods = 5;
    @JsonProperty
    private boolean publishAttributeChanges;

    public final <PUB extends PublisherVO<? extends PublishedPointVO>> PublisherDefinition<PUB> getDefinition() {
        return (PublisherDefinition<PUB>) definition;
    }

    public <PUB extends PublisherVO<? extends PublishedPointVO>> void setDefinition(PublisherDefinition<PUB> definition) {
        this.definition = definition;
    }

    public void setAlarmLevel(int eventId, AlarmLevels level) {
        alarmLevels.put(eventId, level);
    }

    /**
     * Set an alarm level based on the sub-type of the publisher event type
     * which MUST (and already is) one of the codes in getEventCodes()
     * @param subType
     * @param level
     */
    public void setAlarmLevel(String subType, AlarmLevels level) throws ValidationException {
        ExportCodes codes = getEventCodes();
        int eventId = codes.getId(subType);
        if(eventId == -1) {
            ProcessResult result = new ProcessResult();
            result.addContextualMessage("alarmLevel", "emport.error.eventCode", subType, codes.getCodeList());
            throw new ValidationException(result);
        }
        alarmLevels.put(eventId, level);
    }

    public AlarmLevels getAlarmLevel(int eventId, AlarmLevels defaultLevel) {
        AlarmLevels level = alarmLevels.get(eventId);
        if (level == null)
            return defaultLevel;
        return level;
    }

    public List<T> getPoints() {
        return points;
    }

    public void setPoints(List<T> points) {
        this.points = points;
    }

    public int getPublishType() {
        return publishType;
    }

    public void setPublishType(int publishType) {
        this.publishType = publishType;
    }

    public int getCacheWarningSize() {
        return cacheWarningSize;
    }

    public void setCacheWarningSize(int cacheWarningSize) {
        this.cacheWarningSize = cacheWarningSize;
    }

    public int getCacheDiscardSize() {
        return cacheDiscardSize;
    }

    public void setCacheDiscardSize(int cacheDiscardSize) {
        this.cacheDiscardSize = cacheDiscardSize;
    }

    public boolean isSendSnapshot() {
        return sendSnapshot;
    }

    public void setSendSnapshot(boolean sendSnapshot) {
        this.sendSnapshot = sendSnapshot;
    }

    public int getSnapshotSendPeriodType() {
        return snapshotSendPeriodType;
    }

    public void setSnapshotSendPeriodType(int snapshotSendPeriodType) {
        this.snapshotSendPeriodType = snapshotSendPeriodType;
    }

    public int getSnapshotSendPeriods() {
        return snapshotSendPeriods;
    }

    public void setSnapshotSendPeriods(int snapshotSendPeriods) {
        this.snapshotSendPeriods = snapshotSendPeriods;
    }

    public boolean isPublishAttributeChanges() {
        return publishAttributeChanges;
    }
    public void setPublishAttributeChanges(boolean publish) {
        this.publishAttributeChanges = publish;
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 7;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeObject(alarmLevels);
        SerializationHelper.writeSafeUTF(out, name);
        out.writeBoolean(enabled);
        out.writeObject(points);
        out.writeInt(publishType);
        out.writeInt(cacheWarningSize);
        out.writeInt(cacheDiscardSize);
        out.writeBoolean(sendSnapshot);
        out.writeInt(snapshotSendPeriodType);
        out.writeInt(snapshotSendPeriods);
        out.writeBoolean(publishAttributeChanges);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            name = SerializationHelper.readSafeUTF(in);
            enabled = in.readBoolean();
            points = (List<T>) in.readObject();
            //Changes Only
            if(in.readBoolean())
                this.publishType = PublishType.CHANGES_ONLY;
            else
                this.publishType = PublishType.ALL;
            cacheWarningSize = in.readInt();
            cacheDiscardSize = cacheWarningSize * 3;
            sendSnapshot = in.readBoolean();
            snapshotSendPeriodType = in.readInt();
            snapshotSendPeriods = in.readInt();
            alarmLevels = new HashMap<Integer,AlarmLevels>();
            publishAttributeChanges = false;
        }
        else if (ver == 2) {
            name = SerializationHelper.readSafeUTF(in);
            enabled = in.readBoolean();
            points = (List<T>) in.readObject();
            //Changes Only
            if(in.readBoolean())
                this.publishType = PublishType.CHANGES_ONLY;
            else
                this.publishType = PublishType.ALL;
            cacheWarningSize = in.readInt();
            cacheDiscardSize = in.readInt();
            sendSnapshot = in.readBoolean();
            snapshotSendPeriodType = in.readInt();
            snapshotSendPeriods = in.readInt();
            alarmLevels = new HashMap<Integer,AlarmLevels>();
            publishAttributeChanges = false;
        }else if(ver == 3){
            HashMap<Integer, Integer> alarmLevelsInt = (HashMap<Integer, Integer>) in.readObject();
            for(Entry<Integer, Integer> item : alarmLevelsInt.entrySet())
                if(item.getValue() >= 2) //Add warning and important
                    item.setValue(item.getValue()+2);
            alarmLevels = AlarmLevels.convertMap(alarmLevelsInt);
            name = SerializationHelper.readSafeUTF(in);
            enabled = in.readBoolean();
            points = (List<T>) in.readObject();
            //Changes Only
            if(in.readBoolean())
                this.publishType = PublishType.CHANGES_ONLY;
            else
                this.publishType = PublishType.ALL;
            cacheWarningSize = in.readInt();
            cacheDiscardSize = in.readInt();
            sendSnapshot = in.readBoolean();
            snapshotSendPeriodType = in.readInt();
            snapshotSendPeriods = in.readInt();
            publishAttributeChanges = false;
        }else if(ver == 4){
            HashMap<Integer, Integer> alarmLevelsInt = (HashMap<Integer, Integer>) in.readObject();
            for(Entry<Integer, Integer> item : alarmLevelsInt.entrySet())
                if(item.getValue() >= 2) //Add warning and important
                    item.setValue(item.getValue()+2);
            alarmLevels = AlarmLevels.convertMap(alarmLevelsInt);
            name = SerializationHelper.readSafeUTF(in);
            enabled = in.readBoolean();
            points = (List<T>) in.readObject();
            publishType = in.readInt();
            cacheWarningSize = in.readInt();
            cacheDiscardSize = in.readInt();
            sendSnapshot = in.readBoolean();
            snapshotSendPeriodType = in.readInt();
            snapshotSendPeriods = in.readInt();
            publishAttributeChanges = false;
        }else if(ver == 5){
            HashMap<Integer, Integer> alarmLevelsInt = (HashMap<Integer, Integer>) in.readObject();
            alarmLevels = AlarmLevels.convertMap(alarmLevelsInt);
            name = SerializationHelper.readSafeUTF(in);
            enabled = in.readBoolean();
            points = (List<T>) in.readObject();
            publishType = in.readInt();
            cacheWarningSize = in.readInt();
            cacheDiscardSize = in.readInt();
            sendSnapshot = in.readBoolean();
            snapshotSendPeriodType = in.readInt();
            snapshotSendPeriods = in.readInt();
            publishAttributeChanges = false;
        }else if(ver == 6){
            HashMap<Integer, Integer> alarmLevelsInt = (HashMap<Integer, Integer>) in.readObject();
            alarmLevels = AlarmLevels.convertMap(alarmLevelsInt);
            name = SerializationHelper.readSafeUTF(in);
            enabled = in.readBoolean();
            points = (List<T>) in.readObject();
            publishType = in.readInt();
            cacheWarningSize = in.readInt();
            cacheDiscardSize = in.readInt();
            sendSnapshot = in.readBoolean();
            snapshotSendPeriodType = in.readInt();
            snapshotSendPeriods = in.readInt();
            publishAttributeChanges = in.readBoolean();
        }else if(ver == 7){
            alarmLevels = (HashMap<Integer, AlarmLevels>) in.readObject();
            name = SerializationHelper.readSafeUTF(in);
            enabled = in.readBoolean();
            points = (List<T>) in.readObject();
            publishType = in.readInt();
            cacheWarningSize = in.readInt();
            cacheDiscardSize = in.readInt();
            sendSnapshot = in.readBoolean();
            snapshotSendPeriodType = in.readInt();
            snapshotSendPeriods = in.readInt();
            publishAttributeChanges = in.readBoolean();
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("type", definition.getPublisherTypeName());
        writer.writeEntry("points", points);
        writer.writeEntry("snapshotSendPeriodType", Common.TIME_PERIOD_CODES.getCode(snapshotSendPeriodType));
        writer.writeEntry("publishType", PUBLISH_TYPE_CODES.getCode(publishType));
        ExportCodes eventCodes = getEventCodes();
        if (eventCodes != null && eventCodes.size() > 0) {
            Map<String, String> alarmCodeLevels = new HashMap<>();

            for (int i = 0; i < eventCodes.size(); i++) {
                int eventId = eventCodes.getId(i);
                AlarmLevels level = getAlarmLevel(eventId, AlarmLevels.URGENT);
                alarmCodeLevels.put(eventCodes.getCode(eventId), level.name());
            }

            writer.writeEntry("alarmLevels", alarmCodeLevels);
        }
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {

        //Not reading XID so can't do this: super.jsonRead(reader, jsonObject);
        if(jsonObject.containsKey("name"))
            name = getString(jsonObject, "name");
        if(jsonObject.containsKey("enabled"))
            enabled = getBoolean(jsonObject, "enabled");

        //Legacy conversion for publishType
        if(jsonObject.containsKey("publishType")) {
            String publishTypeCode = jsonObject.getString("publishType");
            int publishTypeId = PUBLISH_TYPE_CODES.getId(publishTypeCode);
            if(publishTypeId == -1)
                throw new TranslatableJsonException("emport.error.invalid", "publishType",
                        publishTypeCode, PUBLISH_TYPE_CODES.getCodeList());
            publishType = publishTypeId;
        }else if(jsonObject.containsKey("changesOnly")){
            boolean changesOnly = getBoolean(jsonObject, "changesOnly");
            if(changesOnly){
                this.publishType = PublishType.CHANGES_ONLY;
            }else{
                this.publishType = PublishType.ALL;
            }
        }

        //Could wrap the readInto with a try-catch in case one dataPointId entry is null,
        // however this would be a silent suppression of the issue, so we have elected not to.
        // infiniteautomation/ma-core-public#948
        JsonArray arr = jsonObject.getJsonArray("points");
        if (arr != null) {
            points.clear();
            for (JsonValue jv : arr) {
                T point = createPublishedPointInstance();
                reader.readInto(point, jv.toJsonObject());
                points.add(point);
            }
        }

        String text = jsonObject.getString("snapshotSendPeriodType");
        if (text != null) {
            snapshotSendPeriodType = Common.TIME_PERIOD_CODES.getId(text);
            if (snapshotSendPeriodType == -1)
                throw new TranslatableJsonException("emport.error.invalid", "snapshotSendPeriodType", text,
                        Common.TIME_PERIOD_CODES.getCodeList());
        }

        JsonObject alarmCodeLevels = jsonObject.getJsonObject("alarmLevels");
        if (alarmCodeLevels != null) {
            ExportCodes eventCodes = getEventCodes();
            if (eventCodes != null && eventCodes.size() > 0) {
                for (String code : alarmCodeLevels.keySet()) {
                    int eventId = eventCodes.getId(code);
                    if (!eventCodes.isValidId(eventId))
                        throw new TranslatableJsonException("emport.error.eventCode", code, eventCodes.getCodeList());

                    text = alarmCodeLevels.getString(code);
                    try {
                        setAlarmLevel(eventId, AlarmLevels.fromName(text));
                    } catch (IllegalArgumentException | NullPointerException e) {
                        throw new TranslatableJsonException("emport.error.alarmLevel", text, code,
                                Arrays.asList(AlarmLevels.values()));
                    }
                }
            }
        }
    }

    @Override
    public String getTypeKey() {
        return "event.audit.publisher";
    }
}
