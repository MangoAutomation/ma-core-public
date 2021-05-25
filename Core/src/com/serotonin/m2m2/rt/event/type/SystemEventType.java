/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonEntity;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventHandlerDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.SystemEventTypeDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.util.ExportNames;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

@JsonEntity
public class SystemEventType extends EventType {
    private static final Log LOG = LogFactory.getLog(SystemEventType.class);

    //
    //
    // Static stuff
    //
    public static final String SYSTEM_SETTINGS_PREFIX = "systemEventAlarmLevel.";

    public static final String TYPE_SYSTEM_STARTUP = "SYSTEM_STARTUP";
    public static final String TYPE_SYSTEM_SHUTDOWN = "SYSTEM_SHUTDOWN";
    public static final String TYPE_MAX_ALARM_LEVEL_CHANGED = "MAX_ALARM_LEVEL_CHANGED";
    public static final String TYPE_USER_LOGIN = "USER_LOGIN";
    public static final String TYPE_FAILED_USER_LOGIN = "FAILED_USER_LOGIN";
    public static final String TYPE_SET_POINT_HANDLER_FAILURE = "SET_POINT_HANDLER_FAILURE";
    public static final String TYPE_EMAIL_SEND_FAILURE = "EMAIL_SEND_FAILURE";
    public static final String TYPE_PROCESS_FAILURE = "PROCESS_FAILURE";
    public static final String TYPE_LICENSE_CHECK = "LICENSE_CHECK";
    public static final String TYPE_BACKUP_FAILURE = "BACKUP_FAILURE";
    public static final String TYPE_BACKUP_SUCCESS = "BACKUP_SUCCESS";
    public static final String TYPE_UPGRADE_CHECK = "UPGRADE_CHECK";
    public static final String TYPE_REJECTED_WORK_ITEM = "REJECTED_WORK_ITEM";
    public static final String TYPE_MISSING_MODULE_DEPENDENCY = "MISSING_MODULE_DEPENDENCY";
    public static final String TYPE_NEW_USER_REGISTERED = "TYPE_NEW_USER_REGISTERED";

    private static final ExportNames TYPE_NAMES = new ExportNames();
    private static final ConcurrentHashMap<String, EventTypeVO> EVENT_TYPES = new ConcurrentHashMap<>();

    public static void initialize() {
        for (SystemEventTypeDefinition def : ModuleRegistry.getDefinitions(SystemEventTypeDefinition.class))
            registerEventType(def.getTypeName(), def.getDescriptionKey());
    }

    private static void registerEventType(String subtype, String key) {
        TYPE_NAMES.addElement(subtype);
        AlarmLevels level = AlarmLevels.fromValue(SystemSettingsDao.instance.getIntValue(SYSTEM_SETTINGS_PREFIX + subtype));
        EVENT_TYPES.put(subtype, new EventTypeVO(new SystemEventType(subtype, 0, null), new TranslatableMessage(key), level));
    }

    static void updateAlarmLevel(String subtype, AlarmLevels alarmLevel) {
        EVENT_TYPES.computeIfPresent(subtype, (k, v) -> {
            return new EventTypeVO(new SystemEventType(k, 0, null), v.getDescription(), alarmLevel);
        });
    }

    public static EventTypeVO getEventType(String subtype) {
        return EVENT_TYPES.get(subtype);
    }

    public static List<EventTypeVO> getRegisteredEventTypes() {
        return new ArrayList<>(EVENT_TYPES.values());
    }

    public static void setEventTypeAlarmLevel(String subtype, AlarmLevels alarmLevel) {
        SystemSettingsDao.instance.setIntValue(SYSTEM_SETTINGS_PREFIX + subtype, alarmLevel.value());
    }

    public static void raiseEvent(SystemEventType type, long time, boolean rtn, TranslatableMessage message) {
        raiseEvent(type, time, rtn, message, null);
    }

    public static void raiseEvent(SystemEventType type, long time, boolean rtn, TranslatableMessage message, Map<String, Object> context) {
        EventTypeVO vo = EVENT_TYPES.get(type.getSystemEventType());
        if (vo == null) {
            LOG.warn("Unknown system event type: " + type.getSystemEventType());
            return;
        }
        Common.eventManager.raiseEvent(type, time, rtn, vo.getAlarmLevel(), message, context);
    }

    public static void returnToNormal(SystemEventType type, long time) {
        Common.eventManager.returnToNormal(type, time);
    }

    //
    //
    // Instance stuff
    //
    private String systemEventType;
    private int refId1;
    private DuplicateHandling duplicateHandling = DuplicateHandling.ALLOW;

    public SystemEventType() {
        // Required for reflection.
    }

    public SystemEventType(String systemEventType) {
        this.systemEventType = systemEventType;
    }

    public SystemEventType(String systemEventType, int refId1) {
        this(systemEventType);
        this.refId1 = refId1;
    }

    public SystemEventType(String systemEventType, int refId1, DuplicateHandling duplicateHandling) {
        this(systemEventType, refId1);
        this.duplicateHandling = duplicateHandling;
    }

    @Override
    public String getEventType() {
        return EventType.EventTypeNames.SYSTEM;
    }

    @Override
    public String getEventSubtype() {
        return systemEventType;
    }

    public String getSystemEventType() {
        return systemEventType;
    }

    @Override
    public boolean isSystemMessage() {
        return true;
    }

    @Override
    public String toString() {
        return "SystemEventType(subtype=" + systemEventType + ")";
    }

    @Override
    public DuplicateHandling getDuplicateHandling() {
        return duplicateHandling;
    }

    @Override
    public int getReferenceId1() {
        return refId1;
    }

    @Override
    public int getReferenceId2() {
        return 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + refId1;
        result = prime * result + ((systemEventType == null) ? 0 : systemEventType.hashCode());
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
        SystemEventType other = (SystemEventType) obj;
        if (refId1 != other.refId1)
            return false;
        if (systemEventType == null) {
            if (other.systemEventType != null)
                return false;
        }
        else if (!systemEventType.equals(other.systemEventType))
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
        systemEventType = getString(jsonObject, "systemType", TYPE_NAMES);

        // Check xid for system events supportsReferenceId1
        String xid = jsonObject.getString("XID");
        if (xid != null) {
            Integer id = getIdByXid(xid);
            if (id == null)
                throw new TranslatableJsonException("emport.error.eventType.invalid.reference", "XID", xid);
            refId1 = id;
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("systemType", systemEventType);

        // Set xid for system events supportsReferenceId1
        String xid = getXidById();
        if (xid != null) {
            writer.writeEntry("XID", xid);
        }
    }

    @Override
    public boolean hasPermission(PermissionHolder user, PermissionService service) {
        return service.hasAdminRole(user) || service.hasEventsSuperadminViewPermission(user);
    }

    @Override
    public MangoPermission getEventPermission(Map<String, Object> context, PermissionService service) {
        return MangoPermission.superadminOnly();
    }

    private String getXidById() {
        String xid = null;
        if (systemEventType.equals(TYPE_USER_LOGIN)) {
            xid = UserDao.getInstance().getXidById(refId1);
        }
        if (systemEventType.equals(TYPE_SET_POINT_HANDLER_FAILURE)) {
            xid = EventHandlerDao.getInstance().getXidById(refId1);
        }
        return xid;
    }

    private Integer getIdByXid(String xid) {
        Integer id = null;
        if (systemEventType.equals(TYPE_USER_LOGIN)) {
            id = UserDao.getInstance().getIdByXid(xid);
        }
        if (systemEventType.equals(TYPE_SET_POINT_HANDLER_FAILURE)) {
            id = EventHandlerDao.getInstance().getIdByXid(xid);
        }
        return id;
    }
}
