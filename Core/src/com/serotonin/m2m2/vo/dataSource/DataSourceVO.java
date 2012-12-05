/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.vo.dataSource;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.DataSourceDefinition;
import com.serotonin.m2m2.rt.dataSource.DataSourceRT;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.util.ChangeComparable;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.vo.DataPointVO.PurgeTypes;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.validation.StringValidation;

abstract public class DataSourceVO<T extends DataSourceVO<?>> implements Serializable, Cloneable, JsonSerializable,
        ChangeComparable<T> {
    public static final String XID_PREFIX = "DS_";

    abstract public TranslatableMessage getConnectionDescription();

    abstract public PointLocatorVO createPointLocator();

    abstract public DataSourceRT createDataSourceRT();

    abstract public ExportCodes getEventCodes();

    final public List<EventTypeVO> getEventTypes() {
        List<EventTypeVO> eventTypes = new ArrayList<EventTypeVO>();
        addEventTypes(eventTypes);
        return eventTypes;
    }

    abstract protected void addEventTypes(List<EventTypeVO> eventTypes);

    public boolean isNew() {
        return id == Common.NEW_ID;
    }

    private DataSourceDefinition definition;

    private int id = Common.NEW_ID;
    private String xid;
    @JsonProperty
    private String name;
    @JsonProperty
    private boolean enabled;
    private Map<Integer, Integer> alarmLevels = new HashMap<Integer, Integer>();

    @JsonProperty
    private boolean purgeOverride = true;
    private int purgeType = PurgeTypes.YEARS;
    @JsonProperty
    private int purgePeriod = 1;

    public final DataSourceDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(DataSourceDefinition definition) {
        this.definition = definition;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public boolean isPurgeOverride() {
        return purgeOverride;
    }

    public void setPurgeOverride(boolean purgeOverride) {
        this.purgeOverride = purgeOverride;
    }

    public int getPurgeType() {
        return purgeType;
    }

    public void setPurgeType(int purgeType) {
        this.purgeType = purgeType;
    }

    public int getPurgePeriod() {
        return purgePeriod;
    }

    public void setPurgePeriod(int purgePeriod) {
        this.purgePeriod = purgePeriod;
    }

    public EventTypeVO getEventType(int eventId) {
        for (EventTypeVO vo : getEventTypes()) {
            if (vo.getTypeRef2() == eventId)
                return vo;
        }
        return null;
    }

    protected EventTypeVO createEventType(int eventId, TranslatableMessage message) {
        return createEventType(eventId, message, EventType.DuplicateHandling.IGNORE, AlarmLevels.URGENT);
    }

    protected EventTypeVO createEventType(int eventId, TranslatableMessage message, int duplicateHandling,
            int defaultAlarmLevel) {
        return new EventTypeVO(EventType.EventTypeNames.DATA_SOURCE, null, getId(), eventId, message, getAlarmLevel(
                eventId, defaultAlarmLevel), duplicateHandling);
    }

    public TranslatableMessage getTypeDescription() {
        return new TranslatableMessage(getDefinition().getDescriptionKey());
    }

    public void validate(ProcessResult response) {
        if (StringUtils.isBlank(xid))
            response.addContextualMessage("xid", "validate.required");
        else if (!new DataSourceDao().isXidUnique(xid, id))
            response.addContextualMessage("xid", "validate.xidUsed");
        else if (StringValidation.isLengthGreaterThan(xid, 50))
            response.addContextualMessage("xid", "validate.notLongerThan", 50);

        if (StringUtils.isBlank(name))
            response.addContextualMessage("dataSourceName", "validate.nameRequired");
        if (StringValidation.isLengthGreaterThan(name, 40))
            response.addContextualMessage("dataSourceName", "validate.nameTooLong");

        if (purgeOverride) {
            if (purgeType != PurgeTypes.DAYS && purgeType != PurgeTypes.MONTHS && purgeType != PurgeTypes.WEEKS
                    && purgeType != PurgeTypes.YEARS)
                response.addContextualMessage("purgeType", "validate.invalidValue");
            if (purgePeriod <= 0)
                response.addContextualMessage("purgePeriod", "validate.greaterThanZero");
        }
    }

    protected String getMessage(Translations translations, String key, Object... args) {
        return new TranslatableMessage(key, args).translate(translations);
    }

    public DataSourceVO<?> copy() {
        try {
            return (DataSourceVO<?>) super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    @Override
    public String getTypeKey() {
        return "event.audit.dataSource";
    }

    @Override
    public final void addProperties(List<TranslatableMessage> list) {
        AuditEventType.addPropertyMessage(list, "dsEdit.head.name", name);
        AuditEventType.addPropertyMessage(list, "common.xid", xid);
        AuditEventType.addPropertyMessage(list, "common.enabled", enabled);
        AuditEventType.addPropertyMessage(list, "dsEdit.logging.purgeOverride", purgeOverride);
        AuditEventType.addPeriodMessage(list, "dsEdit.logging.purge", purgeType, purgePeriod);

        addPropertiesImpl(list);
    }

    @Override
    public final void addPropertyChanges(List<TranslatableMessage> list, T from) {
        AuditEventType.maybeAddPropertyChangeMessage(list, "dsEdit.head.name", from.getName(), name);
        AuditEventType.maybeAddPropertyChangeMessage(list, "common.xid", from.getXid(), xid);
        AuditEventType.maybeAddPropertyChangeMessage(list, "common.enabled", from.isEnabled(), enabled);
        AuditEventType.maybeAddPropertyChangeMessage(list, "dsEdit.logging.purgeOverride", from.isPurgeOverride(),
                purgeOverride);
        AuditEventType.maybeAddPeriodChangeMessage(list, "dsEdit.logging.purge", from.getPurgeType(), from.getPurgePeriod(),
                purgeType, purgePeriod);

        addPropertyChangesImpl(list, from);
    }

    abstract protected void addPropertiesImpl(List<TranslatableMessage> list);

    abstract protected void addPropertyChangesImpl(List<TranslatableMessage> list, T from);

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 2;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeBoolean(enabled);
        out.writeObject(alarmLevels);
        out.writeBoolean(purgeOverride);
        out.writeInt(purgeType);
        out.writeInt(purgePeriod);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            enabled = in.readBoolean();
            alarmLevels = (HashMap<Integer, Integer>) in.readObject();
            purgeOverride = false;
            purgeType = PurgeTypes.YEARS;
            purgePeriod = 1;
        }
        else if (ver == 2) {
            enabled = in.readBoolean();
            alarmLevels = (HashMap<Integer, Integer>) in.readObject();
            purgeOverride = in.readBoolean();
            purgeType = in.readInt();
            purgePeriod = in.readInt();
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        writer.writeEntry("xid", xid);
        writer.writeEntry("type", definition.getDataSourceTypeName());

        ExportCodes eventCodes = getEventCodes();
        if (eventCodes != null && eventCodes.size() > 0) {
            Map<String, String> alarmCodeLevels = new HashMap<String, String>();

            for (int i = 0; i < eventCodes.size(); i++) {
                int eventId = eventCodes.getId(i);
                int level = getAlarmLevel(eventId, AlarmLevels.URGENT);
                alarmCodeLevels.put(eventCodes.getCode(eventId), AlarmLevels.CODES.getCode(level));
            }

            writer.writeEntry("alarmLevels", alarmCodeLevels);
        }

        writer.writeEntry("purgeType", Common.TIME_PERIOD_CODES.getCode(purgeType));
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        // Can't change the type.

        JsonObject alarmCodeLevels = jsonObject.getJsonObject("alarmLevels");
        if (alarmCodeLevels != null) {
            ExportCodes eventCodes = getEventCodes();
            if (eventCodes != null && eventCodes.size() > 0) {
                for (String code : alarmCodeLevels.keySet()) {
                    int eventId = eventCodes.getId(code);
                    if (!eventCodes.isValidId(eventId))
                        throw new TranslatableJsonException("emport.error.eventCode", code, eventCodes.getCodeList());

                    String text = alarmCodeLevels.getString(code);
                    int level = AlarmLevels.CODES.getId(text);
                    if (!AlarmLevels.CODES.isValidId(level))
                        throw new TranslatableJsonException("emport.error.alarmLevel", text, code,
                                AlarmLevels.CODES.getCodeList());

                    setAlarmLevel(eventId, level);
                }
            }
        }

        String text = jsonObject.getString("purgeType");
        if (text != null) {
            purgeType = Common.TIME_PERIOD_CODES.getId(text);
            if (purgeType == -1)
                throw new TranslatableJsonException("emport.error.invalid", "purgeType", text,
                        Common.TIME_PERIOD_CODES.getCodeList());
        }
    }

    protected void writeUpdatePeriodType(ObjectWriter writer, int updatePeriodType) throws IOException, JsonException {
        writer.writeEntry("updatePeriodType", Common.TIME_PERIOD_CODES.getCode(updatePeriodType));
    }

    protected Integer readUpdatePeriodType(JsonObject json) throws JsonException {
        String text = json.getString("updatePeriodType");
        if (text == null)
            return null;

        int value = Common.TIME_PERIOD_CODES.getId(text);
        if (value == -1)
            throw new TranslatableJsonException("emport.error.invalid", "updatePeriodType", text,
                    Common.TIME_PERIOD_CODES.getCodeList());

        return value;
    }
}
