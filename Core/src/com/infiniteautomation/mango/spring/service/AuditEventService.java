/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.infiniteautomation.mango.spring.events.audit.AuditEvent;
import com.infiniteautomation.mango.spring.events.audit.ChangeAuditEvent;
import com.infiniteautomation.mango.spring.events.audit.CreateAuditEvent;
import com.infiniteautomation.mango.spring.events.audit.DeleteAuditEvent;
import com.infiniteautomation.mango.spring.events.audit.SystemSettingChangeAuditEvent;
import com.infiniteautomation.mango.spring.events.audit.SystemSettingDeleteAuditEvent;
import com.infiniteautomation.mango.spring.events.audit.ToggleAuditEvent;
import com.serotonin.json.JsonException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.AuditEventDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.util.JsonSerializableUtility;
import com.serotonin.m2m2.vo.AbstractActionVO;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.event.audit.AuditEventInstanceVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.timer.RejectedTaskReason;

/**
 *
 * @author Terry Packer
 */
@Service
public class AuditEventService extends AbstractBasicVOService<AuditEventInstanceVO, AuditEventDao> {

    private final Logger log = LoggerFactory.getLogger(AuditEventService.class);

    @Autowired
    public AuditEventService(AuditEventDao dao, PermissionService permissionService) {
        super(dao, permissionService);
    }

    @Override
    public ProcessResult validate(AuditEventInstanceVO vo, PermissionHolder user) {
        return new ProcessResult();
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, AuditEventInstanceVO vo) {
        return false;
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, AuditEventInstanceVO vo) {
        return permissionService.hasAdminRole(user);
    }

    @EventListener
    protected void raiseAuditEvent(AuditEvent event) {
        Common.backgroundProcessing.addWorkItem(new AuditEventWorkItem(event, this));
    }

    /**
     * VO created
     * @param event
     */
    protected void raiseCreatedEvent(CreateAuditEvent event) {
        Assert.notNull(event.getAuditEventType(), "auditEventType cannot be null");
        Map<String, Object> context = new HashMap<String, Object>();
        JsonSerializableUtility scanner = new JsonSerializableUtility();
        try {
            context = scanner.findValues(event.getVo());
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | JsonException | IOException e) {
            log.error(e.getMessage(), e);
        }
        raiseVoAuditEvent(AuditEventInstanceVO.CHANGE_TYPE_CREATE, event.getAuditEventType(), event.getRaisingHolder(), event.getVo(), "event.audit.extended.added", context);
    }

    /**
     * VO changed
     * @param event
     */
    protected void raiseChangedEvent(ChangeAuditEvent event) {
        Assert.notNull(event.getAuditEventType(), "auditEventType cannot be null");
        Map<String, Object> context = new HashMap<String, Object>();

        //Find the annotated properties
        JsonSerializableUtility scanner = new JsonSerializableUtility();
        try {
            context = scanner.findChanges(event.getFrom(), event.getVo());
            if (context.size() == 0)
                // If the object wasn't in fact changed, don't raise an event.
                return;
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | JsonException | IOException e) {
            log.error(e.getMessage(), e);
        }

        raiseVoAuditEvent(AuditEventInstanceVO.CHANGE_TYPE_MODIFY, event.getAuditEventType(), event.getRaisingHolder(), event.getVo(), "event.audit.extended.changed", context);
    }

    /**
     * An action VO was toggled
     * @param event
     */
    protected void raiseToggledEvent(ToggleAuditEvent event) {
        Assert.notNull(event.getAuditEventType(), "auditEventType cannot be null");
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(AbstractActionVO.ENABLED_KEY, event.getVo().isEnabled());
        raiseVoAuditEvent(AuditEventInstanceVO.CHANGE_TYPE_MODIFY, event.getAuditEventType(), event.getRaisingHolder(), event.getVo(), "event.audit.extended.toggled", context);
    }

    /**
     * VO deleted
     * @param event
     */
    protected void raiseDeletedEvent(DeleteAuditEvent event) {
        Assert.notNull(event.getAuditEventType(), "auditEventType cannot be null");
        Map<String, Object> context = new HashMap<String, Object>();
        raiseVoAuditEvent(AuditEventInstanceVO.CHANGE_TYPE_DELETE, event.getAuditEventType(), event.getRaisingHolder(), event.getVo(), "event.audit.extended.deleted", context);
    }

    /**
     * System setting changed
     * @param event
     */
    protected void raiseSystemSettingChangedEvent(SystemSettingChangeAuditEvent event) {
        Assert.notNull(event.getAuditEventType(), "auditEventType cannot be null");
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(event.getKey(), event.getToValue());
        raiseSystemSettingAuditEvent(AuditEventInstanceVO.CHANGE_TYPE_MODIFY,
                event.getAuditEventType(), event.getRaisingHolder(),
                event.getKey(), "event.audit.systemSetting.changed", context);
    }

    /**
     * System setting deleted (set back to default)
     * @param event
     */
    protected void raiseSystemSettingDeletedEvent(SystemSettingDeleteAuditEvent event) {
        Assert.notNull(event.getAuditEventType(), "auditEventType cannot be null");
        Map<String, Object> context = new HashMap<String, Object>();
        raiseSystemSettingAuditEvent(AuditEventInstanceVO.CHANGE_TYPE_DELETE,
                event.getAuditEventType(), event.getRaisingHolder(),
                event.getKey(), "event.audit.systemSetting.deleted", context);
    }

    private void raiseSystemSettingAuditEvent(int changeType, String auditEventType, PermissionHolder permissionHolder,
            String systemSettingKey, String key, Map<String, Object> context) {
        User raisingUser = permissionHolder.getUser();
        Object username = permissionHolder.getPermissionHolderName();
        if (raisingUser != null) {
            username = raisingUser.getUsername() + " (" + raisingUser.getId() + ")";
        }

        TranslatableMessage message = new TranslatableMessage(key, username, systemSettingKey);

        AuditEventType type = new AuditEventType(auditEventType, changeType, Common.NEW_ID);
        type.setRaisingUser(raisingUser);

        Common.eventManager.raiseEvent(type, Common.timer.currentTimeMillis(), false,
                AuditEventType.getEventType(type.getAuditEventType()).getAlarmLevel(),
                message, context);
    }


    /**
     * Common raise event logic
     *
     * @param changeType
     * @param auditEventType
     * @param permissionHolder
     * @param to
     * @param key
     * @param context
     */
    private void raiseVoAuditEvent(int changeType, String auditEventType, PermissionHolder permissionHolder, AbstractVO to, String key, Map<String, Object> context) {

        User raisingUser = permissionHolder.getUser();
        Object username = permissionHolder.getPermissionHolderName();
        if (raisingUser != null) {
            username = raisingUser.getUsername() + " (" + raisingUser.getId() + ")";
        }

        TranslatableMessage message = new TranslatableMessage(key, username, new TranslatableMessage(to.getTypeKey()),
                to.getName(), to.getXid());

        AuditEventType type = new AuditEventType(auditEventType, changeType, to.getId());
        type.setRaisingUser(raisingUser);

        Common.eventManager.raiseEvent(type, Common.timer.currentTimeMillis(), false,
                AuditEventType.getEventType(type.getAuditEventType()).getAlarmLevel(),
                message, context);
    }

    static class AuditEventWorkItem implements WorkItem {

        private final AuditEventService service;
        private final AuditEvent auditEvent;

        public AuditEventWorkItem(AuditEvent event, AuditEventService service) {
            this.service = service;
            this.auditEvent = event;
        }

        @Override
        public void execute() {
            if(auditEvent instanceof CreateAuditEvent) {
                service.raiseCreatedEvent((CreateAuditEvent)auditEvent);
            }else if(auditEvent instanceof ChangeAuditEvent) {
                service.raiseChangedEvent((ChangeAuditEvent)auditEvent);
            }else if(auditEvent instanceof ToggleAuditEvent) {
                service.raiseToggledEvent((ToggleAuditEvent)auditEvent);
            }else if(auditEvent instanceof DeleteAuditEvent) {
                service.raiseDeletedEvent((DeleteAuditEvent)auditEvent);
            }else if(auditEvent instanceof SystemSettingChangeAuditEvent) {
                service.raiseSystemSettingChangedEvent((SystemSettingChangeAuditEvent)auditEvent);
            }else if(auditEvent instanceof SystemSettingDeleteAuditEvent) {
                service.raiseSystemSettingDeletedEvent((SystemSettingDeleteAuditEvent)auditEvent);
            }
        }

        @Override
        public int getPriority() {
            return WorkItem.PRIORITY_LOW;
        }

        @Override
        public String getDescription() {
            return auditEvent.getAuditEventType();
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
}
