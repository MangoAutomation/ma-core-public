/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import net.jazdw.rql.parser.ASTNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.query.ConditionSortLimitWithTagKeys;
import com.infiniteautomation.mango.db.tables.Events;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.TranslatableRuntimeException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.EventInstanceDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.definitions.permissions.EventsSuperadminViewPermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.EventsViewPermissionDefinition;
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
public class EventInstanceService extends AbstractVOService<EventInstanceVO, EventInstanceDao> {

    private final DataPointDao dataPointDao;
    private final EventsViewPermissionDefinition eventsViewPermission;
    private final EventsSuperadminViewPermissionDefinition eventsSuperadminViewPermission;
    /**
     * Lock used to protect access to acknowledging many events at once
     */
    private final Lock ackManyLock = new ReentrantLock();
    private final Events events = Events.EVENTS;

    @Autowired
    public EventInstanceService(EventInstanceDao dao,
                                ServiceDependencies dependencies,
                                DataPointDao dataPointDao,
                                @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") EventsViewPermissionDefinition eventsViewPermission,
                                @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") EventsSuperadminViewPermissionDefinition eventsSuperadminViewPermission) {
        super(dao, dependencies);
        this.dataPointDao = dataPointDao;
        this.eventsViewPermission = eventsViewPermission;
        this.eventsSuperadminViewPermission = eventsSuperadminViewPermission;
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, EventInstanceVO vo) {
        return permissionService.hasAdminRole(user);
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, EventInstanceVO vo) {
        return vo.getEventType().hasPermission(user,permissionService);
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, EventInstanceVO vo) {
        return this.permissionService.hasPermission(user, eventsSuperadminViewPermission.getPermission()) || permissionService.hasPermission(user, vo.getReadPermission());
    }

    /**
     * Get the active summary of events for a user
     */
    public List<UserEventLevelSummary> getActiveSummary() throws PermissionException {
        PermissionHolder user = Common.getUser();
        this.permissionService.ensurePermission(user, eventsViewPermission.getPermission());

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
     */
    public List<UserEventLevelSummary> getUnacknowledgedSummary() {
        PermissionHolder user = Common.getUser();
        this.permissionService.ensurePermission(user, eventsViewPermission.getPermission());

        Map<AlarmLevels, UserEventLevelSummary> summaries = new EnumMap<>(AlarmLevels.class);
        for (AlarmLevels level : AlarmLevels.values()) {
            if(level == AlarmLevels.IGNORE) {
                continue;
            }
            int count = dao.countUnacknowledgedAlarms(level, user);
            EventInstanceVO latest = dao.getLatestUnacknowledgedAlarm(level, user);
            summaries.put(level, new UserEventLevelSummary(level, count, latest));
        }

        return new ArrayList<>(summaries.values());
    }

    /**
     * Get a summary of data point events for a list of data points
     */
    public Collection<DataPointEventLevelSummary> getDataPointEventSummaries(String[] dataPointXids) throws NotFoundException, PermissionException {
        PermissionHolder user = Common.getUser();
        this.permissionService.ensurePermission(user, eventsViewPermission.getPermission());

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
     */
    public List<EventInstance> getAllActiveUserEvents() {
        PermissionHolder user = Common.getUser();
        this.permissionService.ensurePermission(user, eventsViewPermission.getPermission());
        return Common.eventManager.getAllActiveUserEvents(user);
    }

    /**
     */
    public EventInstanceVO acknowledgeEventById(Integer id, User user, TranslatableMessage message) throws NotFoundException, PermissionException {
        EventInstanceVO vo = get(id);
        ensureEditPermission(user, vo);
        Common.eventManager.acknowledgeEventById(id, System.currentTimeMillis(), user, message);
        return vo;
    }

    public int acknowledgeMany(ConditionSortLimit conditions, TranslatableMessage message) {
        // only users can ack events as it stores user id in events table
        User user = Objects.requireNonNull(Common.getUser().getUser());
        AtomicInteger total = new AtomicInteger();
        long ackTimestamp = Common.timer.currentTimeMillis();

        try {
            if (!ackManyLock.tryLock(10, TimeUnit.SECONDS)) {
                throw new TranslatableRuntimeException(new TranslatableMessage("events.acknowledgeManyFailed"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        try {
            customizedQuery(conditions, (EventInstanceVO vo) -> {
                if (hasEditPermission(user, vo)) {
                    EventInstance event = Common.eventManager.acknowledgeEventById(vo.getId(), ackTimestamp, user, message);
                    if (event != null && event.isAcknowledged()) {
                        total.incrementAndGet();
                    }
                }
            });
        } finally {
            ackManyLock.unlock();
        }
        return total.get();
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
        customizedQuery(conditions.withNullLimitOffset(), (EventInstanceVO item) -> {
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
            while (current != null && (time < current.from.getTime() || time >= current.to.getTime())) {
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

        private static final long serialVersionUID = 1L;

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

    public ConditionSortLimitWithTagKeys createEventCountsConditions(ASTNode rql) {
        return dao.createEventCountsConditions(rql);
    }

    /**
     * Count events for a set of tags and rql on events and data points
     */
    public int countDataPointEventCountsByRQL(ConditionSortLimitWithTagKeys conditions, Long from, Long to) {
        return this.dao.countDataPointEventCountsByRQL(conditions, from, to, Common.getUser());
    }

    /**
     * Query events for a set of tags
     *
     */
    public void queryDataPointEventCountsByRQL(ConditionSortLimitWithTagKeys conditions, Long from, Long to, Consumer<AlarmPointTagCount> callback) {
        this.dao.queryDataPointEventCountsByRQL(conditions, from, to, Common.getUser(), callback);
    }

    public static class AlarmPointTagCount {
        private String xid;
        private String name;
        private String deviceName;
        private TranslatableMessage message;
        private AlarmLevels alarmLevel;
        private int count;
        private Long latestActiveTs;
        private Long latestRtnTs;
        private Map<String,String> tags;

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

        public String getDeviceName() {
            return deviceName;
        }

        public void setDeviceName(String deviceName) {
            this.deviceName = deviceName;
        }

        public TranslatableMessage getMessage() {
            return message;
        }

        public void setMessage(TranslatableMessage message) {
            this.message = message;
        }

        public AlarmLevels getAlarmLevel() {
            return alarmLevel;
        }

        public void setAlarmLevel(AlarmLevels alarmLevel) {
            this.alarmLevel = alarmLevel;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public Long getLatestActiveTs() {
            return latestActiveTs;
        }

        public void setLatestActiveTs(Long latestActiveTs) {
            this.latestActiveTs = latestActiveTs;
        }

        public Long getLatestRtnTs() {
            return latestRtnTs;
        }

        public void setLatestRtnTs(Long latestRtnTs) {
            this.latestRtnTs = latestRtnTs;
        }

        public Map<String, String> getTags() {
            return tags;
        }

        public void setTags(Map<String, String> tags) {
            this.tags = tags;
        }
    }

    @Override
    public ProcessResult validate(EventInstanceVO vo) {
        ProcessResult result = new ProcessResult();
        permissionService.validatePermission(result, "readPermission", Common.getUser(), vo.getReadPermission());
        return result;
    }
    @Override
    public ProcessResult validate(EventInstanceVO existing, EventInstanceVO vo) {
        ProcessResult result = new ProcessResult();
        permissionService.validatePermission(result, "readPermission", Common.getUser(), existing.getReadPermission(), vo.getReadPermission());
        return result;
    }
}
