/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.AuditEventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.util.ChangeComparable;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.util.ExportNames;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.event.EventTypeVO;

public class AuditEventType extends EventType {
    //
    //
    // Static stuff
    //
    private static final String AUDIT_SETTINGS_PREFIX = "auditEventAlarmLevel.";

    public static final String TYPE_DATA_SOURCE = "DATA_SOURCE";
    public static final String TYPE_DATA_POINT = "DATA_POINT";
    public static final String TYPE_POINT_EVENT_DETECTOR = "POINT_EVENT_DETECTOR";
    public static final String TYPE_EVENT_HANDLER = "EVENT_HANDLER";
    public static final String TYPE_COMPOUND_EVENT_DETECTOR = "COMPOUND_EVENT_DETECTOR";
    public static final String TYPE_TEMPLATE = "TEMPLATE";
    public static final String TYPE_USER_COMMENT = "USER_COMMENT";
    public static final String TYPE_USER = "USER";
    
    private static final ExportNames TYPE_NAMES = new ExportNames();
    public static final List<EventTypeVO> EVENT_TYPES = new ArrayList<EventTypeVO>();

    public static void initialize() {
        addEventType(TYPE_DATA_SOURCE, "event.audit.dataSource");
        addEventType(TYPE_DATA_POINT, "event.audit.dataPoint");
        addEventType(TYPE_POINT_EVENT_DETECTOR, "event.audit.pointEventDetector");
        addEventType(TYPE_EVENT_HANDLER, "event.audit.eventHandler");
        addEventType(TYPE_TEMPLATE, "event.audit.template");
        addEventType(TYPE_USER_COMMENT, "event.audit.userComment");
        addEventType(TYPE_USER, "event.audit.user");

        for (AuditEventTypeDefinition def : ModuleRegistry.getDefinitions(AuditEventTypeDefinition.class))
            addEventType(def.getTypeName(), def.getDescriptionKey());
    }

    private static void addEventType(String subtype, String key) {
        TYPE_NAMES.addElement(subtype);
        EVENT_TYPES.add(new EventTypeVO(EventType.EventTypeNames.AUDIT, subtype, 0, 0, new TranslatableMessage(key),
                SystemSettingsDao.getIntValue(AUDIT_SETTINGS_PREFIX + subtype, AlarmLevels.INFORMATION)));
    }

    public static EventTypeVO getEventType(String subtype) {
        for (EventTypeVO et : EVENT_TYPES) {
            if (et.getSubtype().equals(subtype))
                return et;
        }
        return null;
    }

    public static void setEventTypeAlarmLevel(String subtype, int alarmLevel) {
        EventTypeVO et = getEventType(subtype);
        et.setAlarmLevel(alarmLevel);

        SystemSettingsDao dao = new SystemSettingsDao();
        dao.setIntValue(AUDIT_SETTINGS_PREFIX + subtype, alarmLevel);
    }

    public static void raiseAddedEvent(String auditEventType, ChangeComparable<?> o) {
        List<TranslatableMessage> list = new ArrayList<TranslatableMessage>();
        o.addProperties(list);
        raiseEvent(auditEventType, o, "event.audit.added", list.toArray());
    }

    public static <T> void raiseChangedEvent(String auditEventType, T from, ChangeComparable<T> to) {
        List<TranslatableMessage> changes = new ArrayList<TranslatableMessage>();
        to.addPropertyChanges(changes, from);
        if (changes.size() == 0)
            // If the object wasn't in fact changed, don't raise an event.
            return;
        raiseEvent(auditEventType, to, "event.audit.changed", changes.toArray());
    }

    public static void raiseDeletedEvent(String auditEventType, ChangeComparable<?> o) {
        List<TranslatableMessage> list = new ArrayList<TranslatableMessage>();
        o.addProperties(list);
        raiseEvent(auditEventType, o, "event.audit.deleted", list.toArray());
    }

    private static void raiseEvent(String auditEventType, ChangeComparable<?> o, String key, Object[] props) {
        User user = Common.getUser();
        Object username;
        if (user != null)
            username = user.getUsername() + " (" + user.getId() + ")";
        else {
            String descKey = Common.getBackgroundProcessDescription();
            if (descKey == null)
                username = new TranslatableMessage("common.unknown");
            else
                username = new TranslatableMessage(descKey);
        }

        TranslatableMessage message = new TranslatableMessage(key, username, new TranslatableMessage(o.getTypeKey()),
                o.getId(), new TranslatableMessage("event.audit.propertyList." + props.length, props));

        AuditEventType type = new AuditEventType(auditEventType, o.getId());
        type.setRaisingUser(user);

        Common.eventManager.raiseEvent(type, System.currentTimeMillis(), false, getEventType(type.getAuditEventType())
                .getAlarmLevel(), message, null);
    }

    //
    //
    // Utility methods for other classes
    //
    public static void addPropertyMessage(List<TranslatableMessage> list, String propertyNameKey, Object propertyValue) {
        list.add(new TranslatableMessage("event.audit.property", new TranslatableMessage(propertyNameKey),
                propertyValue));
    }

    public static void addPropertyMessage(List<TranslatableMessage> list, String propertyNameKey, boolean propertyValue) {
        list.add(new TranslatableMessage("event.audit.property", new TranslatableMessage(propertyNameKey),
                getBooleanMessage(propertyValue)));
    }

    public static void addPeriodMessage(List<TranslatableMessage> list, String propertyNameKey, int periodType,
            int period) {
        list.add(new TranslatableMessage("event.audit.property", new TranslatableMessage(propertyNameKey), Common
                .getPeriodDescription(periodType, period)));
    }

    public static void addExportCodeMessage(List<TranslatableMessage> list, String propertyNameKey, ExportCodes codes,
            int id) {
        list.add(new TranslatableMessage("event.audit.property", new TranslatableMessage(propertyNameKey),
                getExportCodeMessage(codes, id)));
    }

    public static void addDataTypeMessage(List<TranslatableMessage> list, String propertyNameKey, int dataTypeId) {
        list.add(new TranslatableMessage("event.audit.property", new TranslatableMessage(propertyNameKey), DataTypes
                .getDataTypeMessage(dataTypeId)));
    }

    public static void maybeAddPropertyChangeMessage(List<TranslatableMessage> list, String propertyNameKey,
            int fromValue, int toValue) {
        if (fromValue != toValue)
            addPropertyChangeMessage(list, propertyNameKey, fromValue, toValue);
    }

    public static void maybeAddPropertyChangeMessage(List<TranslatableMessage> list, String propertyNameKey,
            Object fromValue, Object toValue) {
        if (!ObjectUtils.equals(fromValue, toValue))
            addPropertyChangeMessage(list, propertyNameKey, fromValue, toValue);
    }

    public static void maybeAddPropertyChangeMessage(List<TranslatableMessage> list, String propertyNameKey,
            boolean fromValue, boolean toValue) {
        if (fromValue != toValue)
            addPropertyChangeMessage(list, propertyNameKey, getBooleanMessage(fromValue), getBooleanMessage(toValue));
    }

    public static void maybeAddAlarmLevelChangeMessage(List<TranslatableMessage> list, String propertyNameKey,
            int fromAlarmLevel, int toAlarmLevel) {
        if (fromAlarmLevel != toAlarmLevel)
            addPropertyChangeMessage(list, propertyNameKey, AlarmLevels.getAlarmLevelMessage(fromAlarmLevel),
                    AlarmLevels.getAlarmLevelMessage(toAlarmLevel));
    }

    public static void maybeAddPeriodChangeMessage(List<TranslatableMessage> list, String propertyNameKey,
            int fromPeriodType, int fromPeriod, int toPeriodType, int toPeriod) {
        if (fromPeriodType != toPeriodType || fromPeriod != toPeriod)
            addPropertyChangeMessage(list, propertyNameKey, Common.getPeriodDescription(fromPeriodType, fromPeriod),
                    Common.getPeriodDescription(toPeriodType, toPeriod));
    }

    public static void maybeAddExportCodeChangeMessage(List<TranslatableMessage> list, String propertyNameKey,
            ExportCodes exportCodes, int fromId, int toId) {
        if (fromId != toId)
            addPropertyChangeMessage(list, propertyNameKey, getExportCodeMessage(exportCodes, fromId),
                    getExportCodeMessage(exportCodes, toId));
    }

    public static void maybeAddDataTypeChangeMessage(List<TranslatableMessage> list, String propertyNameKey,
            int fromDataTypeId, int toDataTypeId) {
        if (fromDataTypeId != toDataTypeId)
            addPropertyChangeMessage(list, propertyNameKey, DataTypes.getDataTypeMessage(fromDataTypeId),
                    DataTypes.getDataTypeMessage(toDataTypeId));
    }

    private static TranslatableMessage getBooleanMessage(boolean value) {
        if (value)
            return new TranslatableMessage("common.true");
        return new TranslatableMessage("common.false");
    }

    private static TranslatableMessage getExportCodeMessage(ExportCodes exportCodes, int id) {
        String key = exportCodes.getKey(id);
        if (key == null)
            return new TranslatableMessage("common.unknown");
        return new TranslatableMessage(key);
    }

    public static void addPropertyChangeMessage(List<TranslatableMessage> list, String propertyNameKey,
            Object fromValue, Object toValue) {
        list.add(new TranslatableMessage("event.audit.changedProperty", new TranslatableMessage(propertyNameKey),
                fromValue, toValue));
    }

    //
    //
    // Instance stuff
    //
    private String auditEventType;
    private int referenceId;
    private User raisingUser;

    public AuditEventType() {
        // Required for reflection.
    }

    public AuditEventType(String auditEventType, int referenceId) {
        this.auditEventType = auditEventType;
        this.referenceId = referenceId;
    }

    @Override
    public String getEventType() {
        return EventType.EventTypeNames.AUDIT;
    }

    @Override
    public String getEventSubtype() {
        return auditEventType;
    }

    public String getAuditEventType() {
        return auditEventType;
    }

    @Override
    public String toString() {
        return "AuditEventType(auditType=" + auditEventType + ", referenceId=" + referenceId + ")";
    }

    @Override
    public int getDuplicateHandling() {
        return DuplicateHandling.ALLOW;
    }

    @Override
    public int getReferenceId1() {
        return referenceId;
    }

    @Override
    public int getReferenceId2() {
        return 0;
    }

    public void setRaisingUser(User raisingUser) {
        this.raisingUser = raisingUser;
    }

    @Override
    public boolean excludeUser(User user) {
        if (raisingUser != null && !raisingUser.isReceiveOwnAuditEvents())
            return user.equals(raisingUser);
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((auditEventType == null) ? 0 : auditEventType.hashCode());
        result = prime * result + referenceId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AuditEventType other = (AuditEventType) obj;
        if (auditEventType == null) {
            if (other.auditEventType != null)
                return false;
        }
        else if (!auditEventType.equals(other.auditEventType))
            return false;
        if (referenceId != other.referenceId)
            return false;
        return true;
    }

    //
    //
    // Serialization
    //
    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);
        auditEventType = getString(jsonObject, "auditType", TYPE_NAMES);
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("auditType", auditEventType);
    }
}
