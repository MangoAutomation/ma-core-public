/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.publish;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.PublisherDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.publish.PublisherRT;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.util.SerializationHelper;
import com.serotonin.validation.StringValidation;

/**
 * @author Matthew Lohbihler
 */
abstract public class PublisherVO<T extends PublishedPointVO> implements Serializable, JsonSerializable {
    public static final String XID_PREFIX = "PUB_";

    abstract public TranslatableMessage getConfigDescription();

    abstract protected T createPublishedPointInstance();

    abstract public PublisherRT<T> createPublisherRT();

    public List<EventTypeVO> getEventTypes() {
        List<EventTypeVO> eventTypes = new ArrayList<>();
        eventTypes
                .add(new EventTypeVO(EventType.EventTypeNames.PUBLISHER, null, getId(),
                        PublisherRT.POINT_DISABLED_EVENT, new TranslatableMessage("event.pb.pointMissing"),
                        getAlarmLevel(PublisherRT.POINT_DISABLED_EVENT, AlarmLevels.URGENT)));
        eventTypes
                .add(new EventTypeVO(EventType.EventTypeNames.PUBLISHER, null, getId(),
                        PublisherRT.QUEUE_SIZE_WARNING_EVENT, new TranslatableMessage("event.pb.queueSize"),
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

    private PublisherDefinition definition;
    
    private Map<Integer, Integer> alarmLevels = new HashMap<>();

    public TranslatableMessage getTypeDescription() {
        return new TranslatableMessage(getDefinition().getDescriptionKey());
    }

    public boolean isNew() {
        return id == Common.NEW_ID;
    }

    private int id = Common.NEW_ID;
    private String xid;
    @JsonProperty
    private String name;
    @JsonProperty
    private boolean enabled;
    protected List<T> points = new ArrayList<>();
    @JsonProperty
    private boolean changesOnly;
    @JsonProperty
    private int cacheWarningSize = 100;
    @JsonProperty
    private int cacheDiscardSize = 1000;
    @JsonProperty
    private boolean sendSnapshot;
    private int snapshotSendPeriodType = Common.TimePeriods.MINUTES;
    @JsonProperty
    private int snapshotSendPeriods = 5;

    public final PublisherDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(PublisherDefinition definition) {
        this.definition = definition;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setAlarmLevel(int eventId, int level) {
        alarmLevels.put(eventId, level);
    }

    public int getAlarmLevel(int eventId, int defaultLevel) {
        Integer level = alarmLevels.get(eventId);
        if (level == null)
            return defaultLevel;
        return level;
    }
    
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<T> getPoints() {
        return points;
    }

    public void setPoints(List<T> points) {
        this.points = points;
    }

    public boolean isChangesOnly() {
        return changesOnly;
    }

    public void setChangesOnly(boolean changesOnly) {
        this.changesOnly = changesOnly;
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

    public void validate(ProcessResult response) {
        if (StringUtils.isBlank(name))
            response.addContextualMessage("name", "validate.required");
        if (StringValidation.isLengthGreaterThan(name, 40))
            response.addContextualMessage("name", "validate.nameTooLong");

        if (StringUtils.isBlank(xid))
            response.addContextualMessage("xid", "validate.required");
        else if (!new PublisherDao().isXidUnique(xid, id))
            response.addContextualMessage("xid", "validate.xidUsed");
        else if (StringValidation.isLengthGreaterThan(xid, 50))
            response.addContextualMessage("xid", "validate.notLongerThan", 50);

        if (sendSnapshot) {
            if (snapshotSendPeriods <= 0)
                response.addContextualMessage("snapshotSendPeriods", "validate.greaterThanZero");
        }

        if (cacheWarningSize < 1)
            response.addContextualMessage("cacheWarningSize", "validate.greaterThanZero");

        if (cacheDiscardSize <= cacheWarningSize)
            response.addContextualMessage("cacheDiscardSize", "validate.publisher.cacheDiscardSize");

        Set<Integer> set = new HashSet<>();
        for (T point : points) {
            int pointId = point.getDataPointId();
            if (set.contains(pointId)) {
                DataPointVO vo = new DataPointDao().getDataPoint(pointId);
                response.addGenericMessage("validate.publisher.duplicatePoint", vo.getExtendedName(), vo.getXid());
            }
            else
                set.add(pointId);
        }
    }

    //
    //
    // Editing customization
    //
    /*
     * Allows the data source to provide custom context data to its own editing page. Can be used for things like lists
     * of comm ports and such. See DataSourceEditController.
     */
    public void addEditContext(Map<String, Object> model) {
        // No op. Override as required.
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 3;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeObject(alarmLevels);
        SerializationHelper.writeSafeUTF(out, name);
        out.writeBoolean(enabled);
        out.writeObject(points);
        out.writeBoolean(changesOnly);
        out.writeInt(cacheWarningSize);
        out.writeInt(cacheDiscardSize);
        out.writeBoolean(sendSnapshot);
        out.writeInt(snapshotSendPeriodType);
        out.writeInt(snapshotSendPeriods);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            name = SerializationHelper.readSafeUTF(in);
            enabled = in.readBoolean();
            points = (List<T>) in.readObject();
            changesOnly = in.readBoolean();
            cacheWarningSize = in.readInt();
            cacheDiscardSize = cacheWarningSize * 3;
            sendSnapshot = in.readBoolean();
            snapshotSendPeriodType = in.readInt();
            snapshotSendPeriods = in.readInt();
            alarmLevels = new HashMap<Integer,Integer>();
        }
        else if (ver == 2) {
            name = SerializationHelper.readSafeUTF(in);
            enabled = in.readBoolean();
            points = (List<T>) in.readObject();
            changesOnly = in.readBoolean();
            cacheWarningSize = in.readInt();
            cacheDiscardSize = in.readInt();
            sendSnapshot = in.readBoolean();
            snapshotSendPeriodType = in.readInt();
            snapshotSendPeriods = in.readInt();
            alarmLevels = new HashMap<Integer,Integer>();
        }else if(ver == 3){
        	alarmLevels = (HashMap<Integer, Integer>) in.readObject();
            name = SerializationHelper.readSafeUTF(in);
            enabled = in.readBoolean();
            points = (List<T>) in.readObject();
            changesOnly = in.readBoolean();
            cacheWarningSize = in.readInt();
            cacheDiscardSize = cacheWarningSize * 3;
            sendSnapshot = in.readBoolean();
            snapshotSendPeriodType = in.readInt();
            snapshotSendPeriods = in.readInt();
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        writer.writeEntry("xid", xid);
        writer.writeEntry("type", definition.getPublisherTypeName());
        writer.writeEntry("points", points);
        writer.writeEntry("snapshotSendPeriodType", Common.TIME_PERIOD_CODES.getCode(snapshotSendPeriodType));
        ExportCodes eventCodes = getEventCodes();
        if (eventCodes != null && eventCodes.size() > 0) {
            Map<String, String> alarmCodeLevels = new HashMap<>();

            for (int i = 0; i < eventCodes.size(); i++) {
                int eventId = eventCodes.getId(i);
                int level = getAlarmLevel(eventId, AlarmLevels.URGENT);
                alarmCodeLevels.put(eventCodes.getCode(eventId), AlarmLevels.CODES.getCode(level));
            }

            writer.writeEntry("alarmLevels", alarmCodeLevels);
        }
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
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
                    int level = AlarmLevels.CODES.getId(text);
                    if (!AlarmLevels.CODES.isValidId(level))
                        throw new TranslatableJsonException("emport.error.alarmLevel", text, code,
                                AlarmLevels.CODES.getCodeList());

                    setAlarmLevel(eventId, level);
                }
            }
        }
    }
}
