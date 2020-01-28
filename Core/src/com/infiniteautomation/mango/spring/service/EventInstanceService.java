/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     *
     * @param user
     * @return
     * @throws NotFoundException
     * @throws PermissionException
     */
    public List<UserEventLevelSummary> getActiveSummary(User user) throws PermissionException {
        List<UserEventLevelSummary> list = new ArrayList<UserEventLevelSummary>();

        //This query is slow the first time as it must fill the UserEventCache
        List<EventInstance> events = Common.eventManager.getAllActiveUserEvents(user);

        UserEventLevelSummary lifeSafety = new UserEventLevelSummary(AlarmLevels.LIFE_SAFETY);
        list.add(lifeSafety);

        UserEventLevelSummary critical = new UserEventLevelSummary(AlarmLevels.CRITICAL);
        list.add(critical);

        UserEventLevelSummary urgent = new UserEventLevelSummary(AlarmLevels.URGENT);
        list.add(urgent);

        UserEventLevelSummary warning = new UserEventLevelSummary(AlarmLevels.WARNING);
        list.add(warning);

        UserEventLevelSummary important = new UserEventLevelSummary(AlarmLevels.IMPORTANT);
        list.add(important);

        UserEventLevelSummary information = new UserEventLevelSummary(AlarmLevels.INFORMATION);
        list.add(information);

        UserEventLevelSummary none = new UserEventLevelSummary(AlarmLevels.NONE);
        list.add(none);

        UserEventLevelSummary doNotLog = new UserEventLevelSummary(AlarmLevels.DO_NOT_LOG);
        list.add(doNotLog);

        for (EventInstance event : events) {
            switch (event.getAlarmLevel()) {
                case LIFE_SAFETY:
                    lifeSafety.incrementUnsilencedCount();
                    lifeSafety.setLatest(event);
                    break;
                case CRITICAL:
                    critical.incrementUnsilencedCount();
                    critical.setLatest(event);
                    break;
                case URGENT:
                    urgent.incrementUnsilencedCount();
                    urgent.setLatest(event);
                    break;
                case WARNING:
                    warning.incrementUnsilencedCount();
                    warning.setLatest(event);
                    break;
                case IMPORTANT:
                    important.incrementUnsilencedCount();
                    important.setLatest(event);
                    break;
                case INFORMATION:
                    information.incrementUnsilencedCount();
                    information.setLatest(event);
                    break;
                case NONE:
                    none.incrementUnsilencedCount();
                    none.setLatest(event);
                    break;
                case DO_NOT_LOG:
                    doNotLog.incrementUnsilencedCount();
                    doNotLog.setLatest(event);
                    break;
                case IGNORE:
                    break;
                default:
                    break;
            }
        }
        return list;
    }

    /**
     * Get a summary of data point events for a list of data points
     * @param dataPointXids
     * @param user
     * @return
     * @throws NotFoundException
     * @throws PermissionException
     */
    public Collection<DataPointEventLevelSummary> getDataPointEventSummaries(String[] dataPointXids, User user) throws NotFoundException, PermissionException {
        return this.permissionService.runAs(user, () -> {
            Map<Integer, DataPointEventLevelSummary> map = new HashMap<>();
            for(String xid : dataPointXids) {
                Integer point = dataPointService.getDao().getIdByXid(xid);
                if(point != null) {
                    map.put(point, new DataPointEventLevelSummary(xid));
                }
            }

            List<EventInstance> events = Common.eventManager.getAllActiveUserEvents(user);
            for(EventInstance event : events) {
                if(EventType.EventTypeNames.DATA_POINT.equals(event.getEventType().getEventType())) {
                    DataPointEventLevelSummary model = map.get(event.getEventType().getReferenceId1());
                    if(model != null) {
                        model.update(event);
                    }
                }
            }
            return map.values();
        });
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
