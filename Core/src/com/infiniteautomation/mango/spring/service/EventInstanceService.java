/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jooq.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.spring.db.EventInstanceTableDefinition;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
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

    private final DataPointDao dataPointDao;

    @Autowired
    public EventInstanceService(EventInstanceDao dao, PermissionService permissionService, DataPointDao dataPointDao) {
        super(dao, permissionService);
        this.dataPointDao = dataPointDao;
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, EventInstanceVO vo) {
        return permissionService.hasAdminRole(user);
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, EventInstanceVO vo) {
        return permissionService.hasEventTypePermission(user, vo.getEventType());
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, EventInstanceVO vo) {
        if(permissionService.hasAdminRole(user)) {
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
        PermissionHolder user = Common.getUser();
        this.permissionService.ensureEventsVewPermission(user);

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
        PermissionHolder user = Common.getUser();
        this.permissionService.ensureEventsVewPermission(user);

        Map<AlarmLevels, UserEventLevelSummary> summaries = new EnumMap<>(AlarmLevels.class);
        for (AlarmLevels level : AlarmLevels.values()) {
            if(level == AlarmLevels.IGNORE) {
                continue;
            }
            summaries.put(level, new UserEventLevelSummary(level));
        }

        Field<Long> ackTs = this.dao.getTable().getAlias("ackTs");
        ConditionSortLimit conditions = new ConditionSortLimit(ackTs.isNull(), null, null, null);
        dao.customizedQuery(conditions, user, (item, index) -> {
            dao.loadRelationalData(item);
            UserEventLevelSummary summary = summaries.get(item.getAlarmLevel());
            summary.increment(item);
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
        PermissionHolder user = Common.getUser();
        this.permissionService.ensureEventsVewPermission(user);

        Map<Integer, DataPointEventLevelSummary> map = new LinkedHashMap<>();
        for(String xid : dataPointXids) {
            Integer point = dataPointDao.getIdByXid(xid);
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
        this.permissionService.ensureEventsVewPermission(user);

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

    public List<PeriodCounts> countQuery(ConditionSortLimit conditions, List<Date> periodBoundaries) {
        Assert.notNull(conditions, "Conditions can't be null");
        Assert.notNull(periodBoundaries, "periodBoundaries can't be null");
        Assert.isTrue(periodBoundaries.size() >= 2, "periodBoundaries must have at least 2 elements");

        Collections.sort(periodBoundaries);

        List<PeriodCounts> list = new ArrayList<>(periodBoundaries.size() - 1);
        for (int i = 0; i < periodBoundaries.size() - 1; i++) {
            Date from = periodBoundaries.get(i);
            Date to = periodBoundaries.get(i + 1);
            list.add(new PeriodCounts(from, to));
        }

        PermissionHolder user = Common.getUser();
        CurrentPeriod current = new CurrentPeriod(list);
        customizedQuery(conditions.withNullLimitOffset(), (EventInstanceVO item, int index) -> {
            if (hasReadPermission(user, item)) {
                PeriodCounts period = current.getPeriod(new Date(item.getActiveTimestamp()));
                if (period != null) {
                    if (item.isRtnApplicable() && item.getRtnTimestamp() == null) {
                        period.active.increment(item.getAlarmLevel());
                    }
                    if (item.getAcknowledgedTimestamp() == null) {
                        period.unacknowledged.increment(item.getAlarmLevel());
                    }
                    period.total.increment(item.getAlarmLevel());
                }
            }
        });
        return list;
    }

    private static class CurrentPeriod {
        final Iterator<PeriodCounts> periods;
        PeriodCounts current = null;

        CurrentPeriod(List<PeriodCounts> periods) {
            this.periods = periods.iterator();
            if (this.periods.hasNext()) {
                current = this.periods.next();
            }
        }

        PeriodCounts getPeriod(Date instant) {
            long time = instant.getTime();
            while (current != null && time >= current.from.getTime() && time < current.to.getTime()) {
                current = periods.hasNext() ? periods.next() : null;
            }
            return current;
        }
    }

    public static class PeriodCounts {
        private final Date from;
        private final Date to;
        private final AlarmCounts active = new AlarmCounts();
        private final AlarmCounts unacknowledged = new AlarmCounts();
        private final AlarmCounts total = new AlarmCounts();

        private PeriodCounts(Date from, Date to) {
            this.from = from;
            this.to = to;
        }

        public Date getFrom() {
            return from;
        }

        public Date getTo() {
            return to;
        }

        public Map<AlarmLevels, Integer> getActive() {
            return active;
        }

        public Map<AlarmLevels, Integer> getTotal() {
            return total;
        }

        public Map<AlarmLevels, Integer> getUnacknowledged() {
            return unacknowledged;
        }
    }

    public static class AlarmCounts extends HashMap<AlarmLevels, Integer> {
        public AlarmCounts() {
            for (AlarmLevels level : AlarmLevels.values()) {
                if (level != AlarmLevels.DO_NOT_LOG && level != AlarmLevels.IGNORE) {
                    put(level, 0);
                }
            }
        }

        public void increment(AlarmLevels level) {
            this.computeIfPresent(level, (l, count) -> ++count);
        }
    }

}
