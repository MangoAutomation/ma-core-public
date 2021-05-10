/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
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
import net.jazdw.rql.parser.ASTNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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
    public EventInstanceService(EventInstanceDao dao, PermissionService permissionService, DataPointDao dataPointDao,
                                EventsViewPermissionDefinition eventsViewPermission,
                                EventsSuperadminViewPermissionDefinition eventsSuperadminViewPermission) {
        super(dao, permissionService);
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
        if (this.permissionService.hasPermission(user, eventsSuperadminViewPermission.getPermission())) {
            return true;
        } else {
            return permissionService.hasPermission(user, vo.getReadPermission());
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
     * @return
     */
    public List<UserEventLevelSummary> getUnacknowledgedSummary() {
        PermissionHolder user = Common.getUser();
        this.permissionService.ensurePermission(user, eventsViewPermission.getPermission());

        Map<AlarmLevels, UserEventLevelSummary> summaries = new EnumMap<>(AlarmLevels.class);
        for (AlarmLevels level : AlarmLevels.values()) {
            if(level == AlarmLevels.IGNORE) {
                continue;
            }
            summaries.put(level, new UserEventLevelSummary(level));
        }

        ConditionSortLimit conditions = new ConditionSortLimit(events.ackTs.isNull(), null, null, null);
        dao.customizedQuery(conditions, user, (item) -> {
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
     * @return
     */
    public List<EventInstance> getAllActiveUserEvents() {
        PermissionHolder user = Common.getUser();
        this.permissionService.ensurePermission(user, eventsViewPermission.getPermission());
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

    /**
     * Count events for a set of tags and rql on events and data points
     */
    public int countDataPointEventCountsByRQL(ASTNode rql, Long from, Long to) {
        PermissionHolder user = Common.getUser();
        return this.dao.countDataPointEventCountsByRQL(rql, from, to, user);
    }

    /**
     * Query events for a set of tags
     *
     */
    public void queryDataPointEventCountsByRQL(ASTNode rql, Long from, Long to, Consumer<AlarmPointTagCount> callback) {
        PermissionHolder user = Common.getUser();
        this.dao.queryDataPointEventCountsByRQL(rql, from, to, user, callback);
    }

    public static class AlarmPointTagCount {
        private final String xid;
        private final String name;
        private final String deviceName;
        private final TranslatableMessage message;
        private final AlarmLevels alarmLevel;
        private final int count;
        private final Long latestActiveTs;
        private final Long latestRtnTs;
        private final Map<String,String> tags;
        /**
         * @param xid
         * @param name
         * @param deviceName
         * @param alarmLevel
         * @param count
         * @param latestActiveTs
         * @param latestRtnTs
         * @param tags
         */
        public AlarmPointTagCount(String xid, String name, String deviceName,
                TranslatableMessage message, AlarmLevels alarmLevel,
                int count, Long latestActiveTs, Long latestRtnTs, Map<String,String> tags) {
            super();
            this.xid = xid;
            this.name = name;
            this.deviceName = deviceName;
            this.message = message;
            this.alarmLevel = alarmLevel;
            this.count = count;
            this.latestActiveTs = latestActiveTs;
            this.latestRtnTs = latestRtnTs;
            this.tags = tags;
        }
        public String getXid() {
            return xid;
        }
        public String getName() {
            return name;
        }
        public String getDeviceName() {
            return deviceName;
        }
        public TranslatableMessage getMessage() {
            return message;
        }
        public AlarmLevels getAlarmLevel() {
            return alarmLevel;
        }
        public int getCount() {
            return count;
        }
        public Long getLatestActiveTs() { return latestActiveTs; }
        public Long getLatestRtnTs() { return latestRtnTs; }

        public Map<String,String> getTags() {
            return tags;
        }
    }

    @Override
    public ProcessResult validate(EventInstanceVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        permissionService.validatePermission(result, "readPermission", user, vo.getReadPermission());

        return result;
    }
    @Override
    public ProcessResult validate(EventInstanceVO existing, EventInstanceVO vo,
            PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        permissionService.validatePermission(result, "readPermission", user, vo.getReadPermission());

        return result;
    }

    /**
     * @param vo
     * @param user
     * @return
     */
    private ProcessResult commonValidation(EventInstanceVO vo, PermissionHolder user) {
        ProcessResult result = new ProcessResult();
        return result;
    }


}
