/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.event.type;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.AuditEventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.util.ExportNames;
import com.serotonin.m2m2.util.JsonSerializableUtility;
import com.serotonin.m2m2.vo.AbstractActionVO;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.vo.event.audit.AuditEventInstanceVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.timer.RejectedTaskReason;

public class AuditEventType extends EventType {
    //
    //
    // Static stuff
    //
    private static final Log LOG = LogFactory.getLog(AuditEventType.class);

    public static final String AUDIT_SETTINGS_PREFIX = "auditEventAlarmLevel.";

    public static final String TYPE_DATA_SOURCE = "DATA_SOURCE";
    public static final String TYPE_DATA_POINT = "DATA_POINT";
    public static final String TYPE_EVENT_HANDLER = "EVENT_HANDLER";
    public static final String TYPE_COMPOUND_EVENT_DETECTOR = "COMPOUND_EVENT_DETECTOR";
    public static final String TYPE_TEMPLATE = "TEMPLATE";
    public static final String TYPE_USER_COMMENT = "USER_COMMENT";
    public static final String TYPE_USER = "USER";
    public static final String TYPE_JSON_DATA = "JSON_DATA";
    public static final String TYPE_EVENT_DETECTOR = "EVENT_DETECTOR";
    public static final String TYPE_PUBLISHER = "EVENT_PUBLISHER";
    public static final String TYPE_MAILING_LIST = "MAILING_LIST";
    public static final String TYPE_ROLE = "ROLE";

    private static final ExportNames TYPE_NAMES = new ExportNames();
    private static final ConcurrentHashMap<String, EventTypeVO> EVENT_TYPES = new ConcurrentHashMap<>();

    public static void initialize() {
        registerEventType(TYPE_DATA_SOURCE, "event.audit.dataSource");
        registerEventType(TYPE_DATA_POINT, "event.audit.dataPoint");
        registerEventType(TYPE_EVENT_HANDLER, "event.audit.eventHandler");
        registerEventType(TYPE_TEMPLATE, "event.audit.template");
        registerEventType(TYPE_USER_COMMENT, "event.audit.userComment");
        registerEventType(TYPE_USER, "event.audit.user");
        registerEventType(TYPE_JSON_DATA, "event.audit.jsonData");
        registerEventType(TYPE_EVENT_DETECTOR, "event.audit.eventDetector");
        registerEventType(TYPE_PUBLISHER, "event.audit.publisher");
        registerEventType(TYPE_MAILING_LIST, "event.audit.mailingList");
        registerEventType(TYPE_ROLE, "event.audit.role");

        for (AuditEventTypeDefinition def : ModuleRegistry.getDefinitions(AuditEventTypeDefinition.class))
            registerEventType(def.getTypeName(), def.getDescriptionKey());
    }

    private static void registerEventType(String subtype, String key) {
        TYPE_NAMES.addElement(subtype);

        AlarmLevels level = AlarmLevels.fromValue(SystemSettingsDao.instance.getIntValue(AUDIT_SETTINGS_PREFIX + subtype));
        EVENT_TYPES.put(subtype, new EventTypeVO(new AuditEventType(subtype, 0, 0), new TranslatableMessage(key), level));
    }

    static void updateAlarmLevel(String subtype, AlarmLevels alarmLevel) {
        EVENT_TYPES.computeIfPresent(subtype, (k, v) -> {
            return new EventTypeVO(new AuditEventType(k, 0, 0), v.getDescription(), alarmLevel);
        });
    }

    public static EventTypeVO getEventType(String subtype) {
        return EVENT_TYPES.get(subtype);
    }

    public static List<EventTypeVO> getRegisteredEventTypes() {
        return new ArrayList<>(EVENT_TYPES.values());
    }

    public static void setEventTypeAlarmLevel(String subtype, AlarmLevels alarmLevel) {
        SystemSettingsDao.instance.setIntValue(AUDIT_SETTINGS_PREFIX + subtype, alarmLevel.value());
    }

    public static void raiseAddedEvent(String auditEventType, AbstractVO o) {
        Map<String, Object> context = new HashMap<String, Object>();
        JsonSerializableUtility scanner = new JsonSerializableUtility();
        try {
            context = scanner.findValues(o);
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | JsonException | IOException e) {
            LOG.error(e.getMessage(), e);
        }
        raiseEvent(AuditEventInstanceVO.CHANGE_TYPE_CREATE, auditEventType, o, "event.audit.extended.added", context);
    }

    public static void raiseChangedEvent(String auditEventType, AbstractVO from, AbstractVO to) {
        Map<String, Object> context = new HashMap<String, Object>();

        //Find the annotated properties
        JsonSerializableUtility scanner = new JsonSerializableUtility();
        try {
            context = scanner.findChanges(from,to);
            if (context.size() == 0)
                // If the object wasn't in fact changed, don't raise an event.
                return;
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | JsonException | IOException e) {
            LOG.error(e.getMessage(), e);
        }

        raiseEvent(AuditEventInstanceVO.CHANGE_TYPE_MODIFY, auditEventType, to, "event.audit.extended.changed", context);
    }

    public static void raiseToggleEvent(String auditEventType, AbstractActionVO toggled) {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(AbstractActionVO.ENABLED_KEY, toggled.isEnabled());
        raiseEvent(AuditEventInstanceVO.CHANGE_TYPE_MODIFY, auditEventType, toggled, "event.audit.extended.toggled", context);
    }

    public static void raiseDeletedEvent(String auditEventType, AbstractVO o) {
        Map<String, Object> context = new HashMap<String, Object>();
        JsonSerializableUtility scanner = new JsonSerializableUtility();
        try {
            context = scanner.findValues(o);
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | JsonException | IOException e) {
            LOG.error(e.getMessage(), e);
        }
        raiseEvent(AuditEventInstanceVO.CHANGE_TYPE_DELETE, auditEventType, o, "event.audit.extended.deleted", context);
    }

    private static void raiseEvent(int changeType, String auditEventType, AbstractVO to, String key, Map<String, Object> context) {
        Object username;
        User raisingUser = null;

        try{
            PermissionHolder user = Common.getUser();
            if (user instanceof User) {
                raisingUser = (User)user;
                username = raisingUser.getUsername() + " (" + raisingUser.getId() + ")";
            }else {
                username = user.getPermissionHolderName();
            }
        }catch(PermissionException e) {
            //No user in context
            username = new TranslatableMessage("common.unknown");
        }

        TranslatableMessage message = new TranslatableMessage(key, username, new TranslatableMessage(to.getTypeKey()),
                to.getName(), to.getXid());

        AuditEventType type = new AuditEventType(auditEventType, changeType, to.getId());
        type.setRaisingUser(raisingUser);

        Common.backgroundProcessing.addWorkItem(new AuditEventWorkItem(type, Common.timer.currentTimeMillis(), message, context));
    }

    static class AuditEventWorkItem implements WorkItem {

        private AuditEventType type;
        private long time;
        private TranslatableMessage message;
        private Map<String, Object> context;


        /**
         * @param type
         * @param time
         * @param message
         * @param context
         */
        public AuditEventWorkItem(AuditEventType type, long time, TranslatableMessage message,
                Map<String, Object> context) {
            this.type = type;
            this.time = time;
            this.message = message;
            this.context = context;
        }

        @Override
        public void execute() {
            Common.eventManager.raiseEvent(type, time, false, getEventType(type.getAuditEventType())
                    .getAlarmLevel(), message, context);
        }

        @Override
        public int getPriority() {
            return WorkItem.PRIORITY_LOW;
        }

        @Override
        public String getDescription() {
            return message.translate(Common.getTranslations());
        }

        @Override
        public String getTaskId() {
            // No Order required
            return null;
        }

        @Override
        public int getQueueSize() {
            return 0;
        }

        @Override
        public void rejected(RejectedTaskReason reason) {
            //No special handling, tracking/logging handled by WorkItemRunnable
        }
    }

    //
    //
    // Instance stuff
    //
    private String auditEventType;
    private int referenceId;
    private User raisingUser;
    private int changeType;
    private int auditEventId;

    public AuditEventType() {
        // Required for reflection.
    }

    public AuditEventType(String auditEventType, int changeType, int referenceId) {
        this.auditEventType = auditEventType;
        this.changeType = changeType;
        this.referenceId = referenceId;
    }

    public AuditEventType(String auditEventType, int changeType, int referenceId, User raisingUser, int auditEventId) {
        this.auditEventType = auditEventType;
        this.changeType = changeType;
        this.referenceId = referenceId;
        this.raisingUser = raisingUser;
        this.auditEventId = auditEventId;
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
    public DuplicateHandling getDuplicateHandling() {
        return DuplicateHandling.ALLOW;
    }

    @Override
    public int getReferenceId1() {
        return referenceId;
    }

    public void setReferenceId2(int auditEventId){
        this.auditEventId = auditEventId;
    }
    @Override
    public int getReferenceId2() {
        return auditEventId;
    }

    public int getChangeType(){
        return changeType;
    }

    public User getRaisingUser(){
        return this.raisingUser;
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

    @Override
    public boolean hasPermission(PermissionHolder user, PermissionService service) {
        return service.hasAdminRole(user);
    }
}
