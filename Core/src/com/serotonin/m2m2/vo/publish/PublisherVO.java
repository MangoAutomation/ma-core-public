/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.publish;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        List<EventTypeVO> eventTypes = new ArrayList<EventTypeVO>();
        eventTypes
                .add(new EventTypeVO(EventType.EventTypeNames.PUBLISHER, null, getId(),
                        PublisherRT.POINT_DISABLED_EVENT, new TranslatableMessage("event.pb.pointMissing"),
                        AlarmLevels.URGENT));
        eventTypes
                .add(new EventTypeVO(EventType.EventTypeNames.PUBLISHER, null, getId(),
                        PublisherRT.QUEUE_SIZE_WARNING_EVENT, new TranslatableMessage("event.pb.queueSize"),
                        AlarmLevels.URGENT));

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
    protected List<T> points = new ArrayList<T>();
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

        Set<Integer> set = new HashSet<Integer>();
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
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 2;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
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
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        writer.writeEntry("xid", xid);
        writer.writeEntry("type", definition.getPublisherTypeName());
        writer.writeEntry("points", points);
        writer.writeEntry("snapshotSendPeriodType", Common.TIME_PERIOD_CODES.getCode(snapshotSendPeriodType));
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
    }
}
