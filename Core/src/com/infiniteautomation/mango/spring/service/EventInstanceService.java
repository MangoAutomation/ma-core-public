/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jooq.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.spring.db.EventInstanceTableDefinition;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventInstanceDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.DataPointEventLevelSummary;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.UserEventLevelSummary;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.event.EventInstanceVO;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
@Service
public class EventInstanceService extends AbstractVOService<EventInstanceVO, EventInstanceTableDefinition, EventInstanceDao> {

    private final DataPointService dataPointService;

    @Autowired
    public EventInstanceService(EventInstanceDao dao, PermissionService permissionService, DataPointService dataPointService) {
        super(dao, permissionService);
        this.dataPointService = dataPointService;
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, EventInstanceVO vo) {
        return user.hasAdminRole();
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, EventInstanceVO vo) {
        return permissionService.hasEventTypePermission(user, vo.getEventType());
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, EventInstanceVO vo) {
        if(user.hasAdminRole()) {
            return true;
        }else {
            return permissionService.hasEventTypePermission(user, vo.getEventType());
        }
    }

    /**
     * Get the active summary of events for a user
     * @return
     * @throws NotFoundException
     * @throws PermissionException
     */
    public List<UserEventLevelSummary> getActiveSummary() throws PermissionException {
        Map<AlarmLevels, UserEventLevelSummary> summaries = new EnumMap<>(AlarmLevels.class);
        for (AlarmLevels level : AlarmLevels.values()) {
            if(level == AlarmLevels.IGNORE) {
                continue;
            }
            summaries.put(level, new UserEventLevelSummary(level));
        }

        for (EventInstance event : this.getAllActiveUserEvents()) {
            UserEventLevelSummary summary = summaries.get(event.getAlarmLevel());
            summary.increment(event);
        }

        return new ArrayList<>(summaries.values());
    }

    /**
     * @return
     */
    public List<UserEventLevelSummary> getUnacknowledgedSummary() {
        Map<AlarmLevels, UserEventLevelSummary> summaries = new EnumMap<>(AlarmLevels.class);
        for (AlarmLevels level : AlarmLevels.values()) {
            if(level == AlarmLevels.IGNORE) {
                continue;
            }
            summaries.put(level, new UserEventLevelSummary(level));
        }

        Field<Long> ackTs = this.dao.getTable().getAlias("ackTs");
        this.customizedQuery(ackTs.isNull(), null, null, null, null, (event, i) -> {
            UserEventLevelSummary summary = summaries.get(event.getAlarmLevel());
            summary.increment(event);
        });

        return new ArrayList<>(summaries.values());
    }

    /**
     * Get a summary of data point events for a list of data points
     * @param dataPointXids
     * @return
     * @throws NotFoundException
     * @throws PermissionException
     */
    public Collection<DataPointEventLevelSummary> getDataPointEventSummaries(String[] dataPointXids) throws NotFoundException, PermissionException {
        Map<Integer, DataPointEventLevelSummary> map = new LinkedHashMap<>();
        for(String xid : dataPointXids) {
            Integer point = dataPointService.getDao().getIdByXid(xid);
            if(point != null) {
                map.put(point, new DataPointEventLevelSummary(xid));
            }
        }

        for(EventInstance event : this.getAllActiveUserEvents()) {
            if(EventType.EventTypeNames.DATA_POINT.equals(event.getEventType().getEventType())) {
                DataPointEventLevelSummary model = map.get(event.getEventType().getReferenceId1());
                if(model != null) {
                    model.update(event);
                }
            }
        }
        return map.values();
    }

    /**
     * @return
     */
    public List<EventInstance> getAllActiveUserEvents() {
        PermissionHolder user = Common.getUser();
        this.permissionService.ensureValidPermissionHolder(user);
        return Common.eventManager.getAllActiveUserEvents(user);
    }

    /**
     * @param id
     * @param user
     * @param message
     */
    public EventInstanceVO acknowledgeEventById(Integer id, User user, TranslatableMessage message) throws NotFoundException, PermissionException {
        EventInstanceVO vo = get(id);
        ensureEditPermission(user, vo);
        Common.eventManager.acknowledgeEventById(id, System.currentTimeMillis(), user, message);
        return vo;
    }

}
