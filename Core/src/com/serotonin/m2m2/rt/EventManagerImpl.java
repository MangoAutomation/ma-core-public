/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.infiniteautomation.mango.spring.service.EventHandlerService;
import com.infiniteautomation.mango.spring.service.MailingListService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.AuditEventDao;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventManagerListenerDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.ReturnCause;
import com.serotonin.m2m2.rt.event.UserEventListener;
import com.serotonin.m2m2.rt.event.UserEventMulticaster;
import com.serotonin.m2m2.rt.event.handlers.EmailHandlerRT;
import com.serotonin.m2m2.rt.event.handlers.EventHandlerRT;
import com.serotonin.m2m2.rt.event.type.DuplicateHandling;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.rt.maint.work.WorkItem;
import com.serotonin.m2m2.util.ExceptionListWrapper;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.mailingList.RecipientListEntryType;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.timer.RejectedTaskReason;
import com.serotonin.util.ILifecycleState;

/**
 * @author Matthew Lohbihler
 */
public class EventManagerImpl implements EventManager {
    private final Log log = LogFactory.getLog(EventManagerImpl.class);
    private static final int RECENT_EVENT_PERIOD = 1000 * 60 * 10; // 10
    // minutes.

    private final List<EventManagerListenerDefinition> listeners = new CopyOnWriteArrayList<>();
    private final ReadWriteLock activeEventsLock = new ReentrantReadWriteLock();
    private final List<EventInstance> activeEvents = new ArrayList<>();
    private final ReadWriteLock recentEventsLock = new ReentrantReadWriteLock();
    private final List<EventInstance> recentEvents = new ArrayList<>();
    private EventDao eventDao;
    private UserDao userDao;
    private long lastAlarmTimestamp = 0;
    private int highestActiveAlarmLevel = 0;
    private UserEventListener userEventMulticaster = null;
    private MailingListService mailingListService;
    private AuditEventDao auditEventDao;
    private EventHandlerService eventHandlerService;
    private PermissionService permissionService;

    /**
     * State machine allowed order:
     * PRE_INITIALIZE
     * INITIALIZE
     * RUNNING
     * TERMINATE
     * POST_TERMINATE
     * TERMINATED
     *
     */
    private ILifecycleState state = ILifecycleState.PRE_INITIALIZE;

    public EventManagerImpl() {

    }

    /**
     * Check the state of the EventManager
     *  useful if you are a task that may run before/after the RUNNING state
     * @return
     */
    @Override
    public ILifecycleState getLifecycleState(){
        return state;
    }

    //
    //
    // Basic event management.
    //
    /**
     * Raise Event
     * @param type
     * @param time
     * @param rtnApplicable - does this event return to normal?
     * @param alarmLevel
     * @param message
     * @param context
     */
    @Override
    public void raiseEvent(EventType type, long time, boolean rtnApplicable,
            AlarmLevels alarmLevel, TranslatableMessage message,
            Map<String, Object> context) {
        if (state != ILifecycleState.RUNNING)
            return;

        long nowTimestamp = Common.timer.currentTimeMillis();
        if (time > nowTimestamp) {
            log.warn("Raising event in the future! type=" + type +
                    ", message='" + message.translate(Common.getTranslations()) +
                    "' now=" + new Date(nowTimestamp) +
                    " event=" + new Date(time) +
                    " deltaMs=" + (time - nowTimestamp)
            );
        }

        if (alarmLevel == AlarmLevels.IGNORE)
            return;

        // Check if there is an event for this type already active.
        EventInstance dup = get(type);
        if (dup != null) {
            // Check the duplicate handling.
            boolean discard = canDiscard(type, message);
            if (discard)
                return;

            // Otherwise we just continue...
        } else if (!rtnApplicable) {
            // Check if we've already seen this type recently.
            boolean recent = isRecent(type, message);
            if (recent)
                return;
        }

        EventInstance evt = new EventInstance(type, time, rtnApplicable,
                alarmLevel, message, context);
        evt.setReadPermission(type.getEventPermission(context, permissionService));

        // Determine if the event should be suppressed (automatically acknowledged and handlers dont run)
        TranslatableMessage autoAckMessage = null;
        for (EventManagerListenerDefinition l : listeners) {
            try {
                autoAckMessage = l.autoAckEventWithMessage(evt);
            } catch (Exception e) {
                log.warn("Error in event manager listener, continuing", e);
            }
            if (autoAckMessage != null)
                break;
        }

        for (EventManagerListenerDefinition l : listeners) {
            try {
                evt = l.modifyEvent(evt);
            } catch (Exception e) {
                log.warn("Error in event manager listener, continuing", e);
            }
            if (evt == null) {
                return;
            }
        }

        loadHandlers(evt);

        // Get id from database by inserting event immediately.
        //Check to see if we are Not Logging these
        if (alarmLevel != AlarmLevels.DO_NOT_LOG) {
            eventDao.saveEvent(evt);
        }

        // Create user alarm records for all applicable users
        List<Integer> eventUserIds = new ArrayList<>();
        // set of email addresses which have been configured to receive events over a certain level
        Set<String> emailUsers = new HashSet<>();

        List<Integer> userIdsToNotify = new ArrayList<>();
        UserEventListener multicaster = userEventMulticaster;

        for (User user : userDao.getActiveUsers()) {
            // Do not create an event for this user if the event type says the
            // user should be skipped.
            if (type.excludeUser(user))
                continue;

            if (type.hasPermission(user, permissionService)) {
                eventUserIds.add(user.getId());
                // add email addresses for users which have been configured to receive events over a certain level
                if (user.getReceiveAlarmEmails().value() > AlarmLevels.IGNORE.value() && alarmLevel.value() >= user.getReceiveAlarmEmails().value() && !StringUtils.isEmpty(user.getEmail()))
                    emailUsers.add(user.getEmail());

                //Notify All User Event Listeners of the new event
                if ((alarmLevel != AlarmLevels.DO_NOT_LOG) && (!evt.getEventType().getEventType().equals(EventType.EventTypeNames.AUDIT))) {
                    userIdsToNotify.add(user.getId());
                }
            }
        }

        if (multicaster != null)
            Common.backgroundProcessing.addWorkItem(new EventNotifyWorkItem(userIdsToNotify, multicaster, evt, true, false, false, false));

        // add email addresses for mailing lists which have been configured to receive events over a certain level
        emailUsers.addAll(mailingListService.getAlarmAddresses(alarmLevel, time,
                RecipientListEntryType.MAILING_LIST,
                RecipientListEntryType.ADDRESS,
                RecipientListEntryType.USER));

        //No Audit or Do Not Log events are User Events
        if ((eventUserIds.size() > 0) && (alarmLevel != AlarmLevels.DO_NOT_LOG) && (!evt.getEventType().getEventType().equals(EventType.EventTypeNames.AUDIT))) {
            if (autoAckMessage == null)
                lastAlarmTimestamp = Common.timer.currentTimeMillis();
        }

        if (evt.isRtnApplicable()) {
            activeEventsLock.writeLock().lock();
            try {
                activeEvents.add(evt);
            } finally {
                activeEventsLock.writeLock().unlock();
            }
        } else if (evt.getEventType().isRateLimited()) {
            recentEventsLock.writeLock().lock();
            try {
                recentEvents.add(evt);
            } finally {
                recentEventsLock.writeLock().unlock();
            }
        }

        if ((autoAckMessage != null) && (alarmLevel != AlarmLevels.DO_NOT_LOG) && (!evt.getEventType().getEventType().equals(EventType.EventTypeNames.AUDIT)))
            this.acknowledgeEvent(evt, time, null, autoAckMessage);
        else {
            if (evt.isRtnApplicable()) {
                if (alarmLevel.value() > highestActiveAlarmLevel) {
                    int oldValue = highestActiveAlarmLevel;
                    highestActiveAlarmLevel = alarmLevel.value();
                    SystemEventType.raiseEvent(
                            new SystemEventType(SystemEventType.TYPE_MAX_ALARM_LEVEL_CHANGED),
                            time,
                            false,
                            getAlarmLevelChangeMessage("event.alarmMaxIncreased", oldValue));
                }
            }

            // Call raiseEvent handlers.
            handleRaiseEvent(evt, emailUsers);

            if (log.isTraceEnabled())
                log.trace("Event raised: type=" + type + ", message="
                        + message.translate(Common.getTranslations()));
        }
    }

    private boolean canDiscard(EventType type, TranslatableMessage message) {
        // Check the duplicate handling.
        DuplicateHandling dh = type.getDuplicateHandling();
        if (dh == DuplicateHandling.DO_NOT_ALLOW) {
            // Create a log error...
            log.error("An event was raised for a type that is already active: type="
                    + type + ", message=" + message.getKey());
            // ... but ultimately just ignore the thing.
            return true;
        }

        if (dh == DuplicateHandling.IGNORE)
            // Safely return.
            return true;

        if (dh == DuplicateHandling.IGNORE_SAME_MESSAGE) {
            // Ignore only if the message is the same. There may be events of
            // this type with different messages,
            // so look through them all for a match.
            for (EventInstance e : getAll(type)) {
                if (e.getMessage().equals(message))
                    return true;
            }
        }

        return false;
    }

    private boolean isRecent(EventType type, TranslatableMessage message) {
        long cutoff = Common.timer.currentTimeMillis() - RECENT_EVENT_PERIOD;

        recentEventsLock.writeLock().lock();
        try{
            for (int i = recentEvents.size() - 1; i >= 0; i--) {
                EventInstance evt = recentEvents.get(i);
                // This method also purges the list, so we need to check if the
                // event instance has expired or not.
                if (cutoff > evt.getActiveTimestamp())
                    recentEvents.remove(i);
                else if (evt.getEventType().equals(type)
                        && evt.getMessage().equals(message))
                    return true;
            }
        }finally{
            recentEventsLock.writeLock().unlock();
        }

        return false;
    }

    @Override
    public void returnToNormal(EventType type, long time) {
        returnToNormal(type, time, ReturnCause.RETURN_TO_NORMAL);
    }

    @Override
    public void returnToNormal(EventType type, long time, ReturnCause cause) {
        EventInstance evt = remove(type);
        if(evt == null)
            return;

        long nowTimestamp = Common.timer.currentTimeMillis();
        if(time > nowTimestamp) {
            log.warn("Returning event to normal in future! type=" + type +
                    ", message='" + evt.getMessage().translate(Common.getTranslations()) +
                    "' now=" + new Date(nowTimestamp) +
                    " event=" + new Date(time) +
                    " deltaMs=" + (time - nowTimestamp)
                    );
        }

        List<User> activeUsers = userDao.getActiveUsers();
        UserEventListener multicaster = userEventMulticaster;

        // Loop in case of multiples
        while (evt != null) {
            evt.returnToNormal(time, cause);

            List<Integer> userIdsToNotify = new ArrayList<>();
            for (User user : activeUsers) {
                // Do not create an event for this user if the event type says the
                // user should be skipped.
                if (type.excludeUser(user))
                    continue;

                if(evt.getAlarmLevel() != AlarmLevels.DO_NOT_LOG){
                    if (type.hasPermission(user, permissionService)) {
                        userIdsToNotify.add(user.getId());
                    }
                }
            }

            if(multicaster != null && Common.backgroundProcessing != null)
                Common.backgroundProcessing.addWorkItem(new EventNotifyWorkItem(userIdsToNotify, multicaster, evt, false, true, false, false));

            resetHighestAlarmLevel(time);
            if(evt.getAlarmLevel() != AlarmLevels.DO_NOT_LOG)
                eventDao.saveEvent(evt);

            // Call inactiveEvent handlers.
            handleInactiveEvent(evt);

            // Check for another
            evt = remove(type);
        }

        if (log.isTraceEnabled())
            log.trace("Event returned to normal: type=" + type);
    }

    /**
     * Deactivate a group of similar events, these events should have been removed from the active events list already.
     *
     * @param evts
     * @param time
     * @param inactiveCause
     */
    protected void deactivateEvents(List<EventInstance> evts, long time, ReturnCause inactiveCause) {
        List<User> activeUsers = userDao.getActiveUsers();

        List<Integer> eventIds = new ArrayList<>();
        UserEventListener multicaster = userEventMulticaster;

        for(EventInstance evt : evts){
            if(evt.isActive())
                eventIds.add(evt.getId());

            evt.returnToNormal(time, inactiveCause);

            List<Integer> userIdsToNotify = new ArrayList<>();
            for (User user : activeUsers) {
                // Do not create an event for this user if the event type says the
                // user should be skipped.
                if (evt.getEventType().excludeUser(user))
                    continue;

                if (evt.getEventType().hasPermission(user, permissionService)) {
                    userIdsToNotify.add(user.getId());
                }
            }

            if(multicaster != null)
                Common.backgroundProcessing.addWorkItem(new EventNotifyWorkItem(userIdsToNotify, multicaster, evt, false, false, true, false));

            // Call inactiveEvent handlers.
            handleInactiveEvent(evt);
        }
        if(eventIds.size() > 0){
            resetHighestAlarmLevel(time);
            eventDao.returnEventsToNormal(eventIds, time, inactiveCause);
        }
    }

    /**
     * Added to allow Acknowledge Events to be fired
     * @param evt
     * @param time
     * @param ackUser the user that acknowledged the event, or null if acknowledged by system (e.g. EventManagerListenerDefinition)
     * @param alternateAckSource
     */
    private boolean acknowledgeEvent(EventInstance evt, long time, User ackUser, TranslatableMessage alternateAckSource) {
        boolean acked;
        if(ackUser != null)
            acked = eventDao.ackEvent(evt.getId(), time, ackUser.getId(), alternateAckSource);
        else
            acked = eventDao.ackEvent(evt.getId(), time, null, alternateAckSource);

        // event was already acknowledged or doesn't exist
        if (!acked) {
            return false;
        }

        //Fill in the info if someone on the other end wants it
        if(ackUser != null) {
            evt.setAcknowledgedByUserId(ackUser.getId());
            evt.setAcknowledgedByUsername(ackUser.getUsername());
        }
        evt.setAcknowledgedTimestamp(time);
        evt.setAlternateAckSource(alternateAckSource);

        // invoke event handlers
        for (EventHandlerRT<?> handler : evt.getHandlers()) {
            handler.eventAcknowledged(evt);
        }

        List<Integer> userIdsToNotify = new ArrayList<>();
        UserEventListener multicaster = userEventMulticaster;

        for (User user : userDao.getActiveUsers()) {
            // Do not create an event for this user if the event type says the
            // user should be skipped.
            if (evt.getEventType().excludeUser(user))
                continue;

            if (evt.getEventType().hasPermission(user, permissionService)) {
                //Notify All User Event Listeners of the new event
                userIdsToNotify.add(user.getId());
            }
        }

        if(multicaster != null)
            Common.backgroundProcessing.addWorkItem(new EventNotifyWorkItem(userIdsToNotify, multicaster, evt, false, false, false, true));
        return true;
    }

    /**
     * Acknowledges an event given an event ID.
     *
     * The returned EventInstance is a copy from the database, never the cached instance. If the returned instance
     * has a different time, userId or alternateAckSource to what was provided then the event must have been already acknowledged.
     *
     * @param eventId
     * @param time
     * @param user
     * @param alternateAckSource
     * @return the EventInstance for the ID if found, null otherwise
     */
    @Override
    public EventInstance acknowledgeEventById(int eventId, long time, User user, TranslatableMessage alternateAckSource) {
        EventInstance dbEvent;
        EventInstance cachedEvent = getById(eventId);

        if (cachedEvent != null) {
            acknowledgeEvent(cachedEvent, time, user, alternateAckSource);

            // we don't want to return the cached event, user might change it
            // return a copy from the database
            dbEvent = eventDao.get(eventId);
        } else {
            dbEvent = eventDao.get(eventId);

            // only ack the event if it exists and is not already acknowledged
            if (dbEvent != null && !dbEvent.isAcknowledged()) {
                loadHandlers(dbEvent);

                boolean acked = acknowledgeEvent(dbEvent, time, user, alternateAckSource);

                // unlikely case that someone else ackd the event at the same time
                if (!acked) {
                    // fetch the updated event from the db again
                    dbEvent = eventDao.get(eventId);
                }
            }
        }

        return dbEvent;
    }

    @Override
    public long getLastAlarmTimestamp() {
        return lastAlarmTimestamp;
    }

    /**
     * Purge All Events We have
     * @return
     */
    @Override
    public int purgeAllEvents(){

        activeEventsLock.writeLock().lock();
        try{
            activeEvents.clear();
        }finally{
            activeEventsLock.writeLock().unlock();
        }

        recentEventsLock.writeLock().lock();
        try{
            recentEvents.clear();
        }finally{
            recentEventsLock.writeLock().unlock();
        }

        int auditEventCount = auditEventDao.purgeAllEvents();
        return auditEventCount + eventDao.purgeAllEvents();
    }

    /**
     * Purge events prior to time
     * @param time
     * @return
     */
    @Override
    public int purgeEventsBefore(final long time){

        activeEventsLock.writeLock().lock();
        try{
            activeEvents.removeIf(e -> e.getActiveTimestamp() < time);
        }finally{
            activeEventsLock.writeLock().unlock();
        }

        recentEventsLock.writeLock().lock();
        try{
            recentEvents.removeIf(e -> e.getActiveTimestamp() < time);
        }finally{
            recentEventsLock.writeLock().unlock();
        }

        int auditCount = auditEventDao.purgeEventsBefore(time);
        return auditCount + eventDao.purgeEventsBefore(time);
    }

    /**
     * Purge Events before time with a given type
     * @param time
     * @param typeName
     * @return
     */
    @Override
    public int purgeEventsBefore(final long time, final String typeName){

        activeEventsLock.writeLock().lock();
        try{
            activeEvents.removeIf(e -> (e.getActiveTimestamp() < time) && (e.getEventType().getEventType().equals(typeName)));
        }finally{
            activeEventsLock.writeLock().unlock();
        }

        recentEventsLock.writeLock().lock();
        try{
            recentEvents.removeIf(e -> (e.getActiveTimestamp() < time) && (e.getEventType().getEventType().equals(typeName)));
        }finally{
            recentEventsLock.writeLock().unlock();
        }

        if(EventType.EventTypeNames.AUDIT.equals(typeName)) {
            return auditEventDao.purgeEventsBefore(time);
        }else {
            return eventDao.purgeEventsBefore(time, typeName);
        }
    }

    /**
     * Purge Events before time with a given type
     * @param time
     * @param alarmLevel
     * @return
     */
    @Override
    public int purgeEventsBefore(final long time, final AlarmLevels alarmLevel){

        activeEventsLock.writeLock().lock();
        try{
            activeEvents.removeIf(e -> (e.getActiveTimestamp() < time) && (e.getAlarmLevel() == alarmLevel));
        }finally{
            activeEventsLock.writeLock().unlock();
        }

        recentEventsLock.writeLock().lock();
        try{
            recentEvents.removeIf(e -> (e.getActiveTimestamp() < time) && (e.getAlarmLevel() == alarmLevel));
        }finally{
            recentEventsLock.writeLock().unlock();
        }

        int auditEventCount = auditEventDao.purgeEventsBefore(time, alarmLevel);
        return auditEventCount + eventDao.purgeEventsBefore(time, alarmLevel);
    }

    //
    //
    // Canceling events.
    //
    @Override
    public void cancelEventsForDataPoint(int dataPointId) {

        List<EventInstance> dataPointEvents = new ArrayList<>();
        activeEventsLock.writeLock().lock();
        try{
            ListIterator<EventInstance> it = activeEvents.listIterator();
            while(it.hasNext()){
                EventInstance e = it.next();
                if (e.getEventType().getDataPointId() == dataPointId){
                    it.remove();
                    dataPointEvents.add(e);
                }
            }
        }finally{
            activeEventsLock.writeLock().unlock();
        }

        deactivateEvents(dataPointEvents, Common.timer.currentTimeMillis(), ReturnCause.SOURCE_DISABLED);

        recentEventsLock.writeLock().lock();
        try{
            recentEvents.removeIf(e -> e.getEventType().getDataPointId() == dataPointId);
        }finally{
            recentEventsLock.writeLock().unlock();
        }
    }

    @Override
    public void cancelEventsForDataPoints(Set<Integer> pointIds) {
        List<EventInstance> dataPointEvents = new ArrayList<>();
        activeEventsLock.writeLock().lock();
        try{
            ListIterator<EventInstance> it = activeEvents.listIterator();
            while(it.hasNext()){
                EventInstance e = it.next();
                if(pointIds.contains(e.getEventType().getDataPointId())) {
                    it.remove();
                    dataPointEvents.add(e);
                }
            }
        }finally{
            activeEventsLock.writeLock().unlock();
        }

        deactivateEvents(dataPointEvents, Common.timer.currentTimeMillis(), ReturnCause.SOURCE_DISABLED);

        recentEventsLock.writeLock().lock();
        try{
            recentEvents.removeIf(e -> pointIds.contains(e.getEventType().getDataPointId()));
        }finally{
            recentEventsLock.writeLock().unlock();
        }
    }

    /**
     * Cancel active events for a Data Source
     * @param dataSourceId
     */
    @Override
    public void cancelEventsForDataSource(int dataSourceId) {

        List<EventInstance> dataSourceEvents = new ArrayList<>();
        activeEventsLock.writeLock().lock();
        try{
            ListIterator<EventInstance> it = activeEvents.listIterator();
            while(it.hasNext()){
                EventInstance e = it.next();
                if(e.getEventType().getDataSourceId() == dataSourceId){
                    it.remove();
                    dataSourceEvents.add(e);
                }
            }
        }finally{
            activeEventsLock.writeLock().unlock();
        }

        deactivateEvents(dataSourceEvents, Common.timer.currentTimeMillis(), ReturnCause.SOURCE_DISABLED);

        recentEventsLock.writeLock().lock();
        try{
            recentEvents.removeIf(e -> e.getEventType().getDataSourceId() == dataSourceId);
        }finally{
            recentEventsLock.writeLock().unlock();
        }
    }

    /**
     * Cancel all events for a publisher
     * @param publisherId
     */
    @Override
    public void cancelEventsForPublisher(int publisherId) {

        List<EventInstance> publisherEvents = new ArrayList<>();
        activeEventsLock.writeLock().lock();
        try{
            ListIterator<EventInstance> it = activeEvents.listIterator();
            while(it.hasNext()){
                EventInstance e = it.next();
                if(e.getEventType().getPublisherId() == publisherId){
                    it.remove();
                    publisherEvents.add(e);
                }
            }
        }finally{
            activeEventsLock.writeLock().unlock();
        }

        deactivateEvents(publisherEvents, Common.timer.currentTimeMillis(), ReturnCause.SOURCE_DISABLED);

        recentEventsLock.writeLock().lock();
        try{
            recentEvents.removeIf(e -> e.getEventType().getPublisherId() == publisherId);
        }finally{
            recentEventsLock.writeLock().unlock();
        }
    }

    private void resetHighestAlarmLevel(long time) {

        int max = 0;
        activeEventsLock.readLock().lock();
        try{
            for (EventInstance e : activeEvents) {
                if (e.getAlarmLevel().value() > max)
                    max = e.getAlarmLevel().value();
            }
        }finally{
            activeEventsLock.readLock().unlock();
        }



        if (max > highestActiveAlarmLevel) {
            int oldValue = highestActiveAlarmLevel;
            highestActiveAlarmLevel = max;
            SystemEventType.raiseEvent(
                    new SystemEventType(
                            SystemEventType.TYPE_MAX_ALARM_LEVEL_CHANGED),
                    time,
                    false,
                    getAlarmLevelChangeMessage("event.alarmMaxIncreased",
                            oldValue));
        } else if (max < highestActiveAlarmLevel) {
            int oldValue = highestActiveAlarmLevel;
            highestActiveAlarmLevel = max;
            SystemEventType.raiseEvent(
                    new SystemEventType(
                            SystemEventType.TYPE_MAX_ALARM_LEVEL_CHANGED),
                    time,
                    false,
                    getAlarmLevelChangeMessage("event.alarmMaxDecreased",
                            oldValue));
        }
    }

    private TranslatableMessage getAlarmLevelChangeMessage(String key,
            int oldValue) {
        return new TranslatableMessage(key,
                AlarmLevels.fromValue(oldValue).getDescription(),
                AlarmLevels.fromValue(highestActiveAlarmLevel).getDescription());
    }

    //
    //
    // Lifecycle interface
    //
    @Override
    public void initialize(boolean safe) {
        if((state != ILifecycleState.PRE_INITIALIZE))
            return;
        state = ILifecycleState.INITIALIZING;

        permissionService = Common.getBean(PermissionService.class);
        eventDao = Common.getBean(EventDao.class);
        userDao = Common.getBean(UserDao.class);
        mailingListService = Common.getBean(MailingListService.class);
        auditEventDao = Common.getBean(AuditEventDao.class);
        eventHandlerService = Common.getBean(EventHandlerService.class);

        // Get all active events from the database.
        activeEventsLock.writeLock().lock();
        try{
            activeEvents.addAll(eventDao.getActiveEvents());
        }finally{
            activeEventsLock.writeLock().unlock();
        }

        lastAlarmTimestamp = Common.timer.currentTimeMillis();
        resetHighestAlarmLevel(lastAlarmTimestamp);
        state = ILifecycleState.RUNNING;
    }

    @Override
    public void terminate() {
        if (state != ILifecycleState.RUNNING)
            return;
        state = ILifecycleState.TERMINATING;
    }

    @Override
    public void joinTermination() {
        if(state != ILifecycleState.TERMINATING)
            return;
        state = ILifecycleState.TERMINATED;
    }

    //
    //
    // Listeners
    //
    @Override
    public void addListener(EventManagerListenerDefinition l) {
        listeners.add(l);
    }

    @Override
    public void removeListener(EventManagerListenerDefinition l) {
        listeners.remove(l);
    }
    @Override
    public synchronized void addUserEventListener(UserEventListener l) {
        userEventMulticaster = UserEventMulticaster.add(userEventMulticaster, l);
    }
    @Override
    public synchronized void removeUserEventListener(UserEventListener l) {
        userEventMulticaster = UserEventMulticaster.remove(userEventMulticaster, l);
    }

    //
    // User view of active events
    //
    @Override
    public List<EventInstance> getAllActiveUserEvents(PermissionHolder user) {
        List<EventInstance> userEvents;
        activeEventsLock.readLock().lock();
        try{
            userEvents = new ArrayList<>(activeEvents);
        }finally{
            activeEventsLock.readLock().unlock();
        }

        //Prune for user
        userEvents.removeIf(eventInstance -> !eventInstance.getEventType().hasPermission(user, permissionService));
        return userEvents;
    }


    //
    //
    // Convenience
    //

    /**
     * Gets an event from the activeEvents list/cache by its id
     */
    private EventInstance getById(int id) {
        EventInstance e;

        activeEventsLock.readLock().lock();
        try{
            for (EventInstance activeEvent : activeEvents) {
                e = activeEvent;
                if (e.getId() == id)
                    return e;
            }
        }finally{
            activeEventsLock.readLock().unlock();
        }

        return null;
    }

    /**
     * Returns the first event instance with the given type, or null is there is
     * none.
     */
    private EventInstance get(EventType type) {
        activeEventsLock.readLock().lock();
        try{
            for (EventInstance e : activeEvents) {
                if (e.getEventType().equals(type))
                    return e;
            }
        }finally{
            activeEventsLock.readLock().unlock();
        }
        return null;
    }

    private List<EventInstance> getAll(EventType type) {
        activeEventsLock.readLock().lock();
        try{
            return activeEvents.stream()
                    .filter(e -> e.getEventType().equals(type))
                    .collect(Collectors.toList());
        }finally{
            activeEventsLock.readLock().unlock();
        }
    }

    /**
     * To access all active events quickly
     * @return
     */
    @Override
    public List<EventInstance> getAllActive() {
        List<EventInstance> result = new ArrayList<>();
        activeEventsLock.readLock().lock();
        try{
            result.addAll(activeEvents);
        }finally{
            activeEventsLock.readLock().unlock();
        }
        return result;
    }

    /**
     * Finds and removes the first event instance with the given type. Returns
     * null if there is none.
     *
     * @param type
     * @return
     */
    private EventInstance remove(EventType type) {
        activeEventsLock.writeLock().lock();
        try{
            ListIterator<EventInstance> it = activeEvents.listIterator();
            while(it.hasNext()){
                EventInstance e = it.next();
                if (e.getEventType().equals(type)) {
                    it.remove();
                    return e;
                }
            }
        }finally{
            activeEventsLock.writeLock().unlock();
        }

        return null;
    }

    private void loadHandlers(EventInstance event) {
        List<AbstractEventHandlerVO> vos = eventHandlerService.enabledHandlersForType(event.getEventType());
        if (!vos.isEmpty()) {
            List<EventHandlerRT<?>> rts = vos.stream()
                    .map(vo -> {
                        try {
                            return vo.getDefinition().createRuntime(vo);
                        } catch (Exception e) {
                            log.error("Error creating event handler runtime", e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            event.setHandlers(Collections.unmodifiableList(rts));
        }
    }

    /**
     * Used to call eventRaised() on all EventHandlerRT instances, also sends emails to all users and mailing lists
     * which have been configured to receive events of this level.
     *
     * @param evt
     * @param defaultAddresses A set of email addresses that will be notified of all events over a certain level
     * which is configured on each user or on a mailing list
     */
    private void handleRaiseEvent(EventInstance evt, Set<String> defaultAddresses) {
        for (EventHandlerRT<?> h : evt.getHandlers()) {
            h.eventRaised(evt);

            // If this is an email handler, remove any addresses to which it
            // was sent from the default addresses
            // so that the default users do not receive multiple
            // notifications.
            if (h instanceof EmailHandlerRT) {
                EmailHandlerRT eh = (EmailHandlerRT)h;
                if(eh.getActiveRecipients() != null) {
                    for (String addr : eh.getActiveRecipients())
                        defaultAddresses.remove(addr);
                }
            }
        }

        //Only run if there are default recipients
        if (defaultAddresses.size() > 0) {
            EmailHandlerRT.sendActiveEmail(evt, defaultAddresses);
        }
    }

    private void handleInactiveEvent(EventInstance evt) {
        for (EventHandlerRT<?> h : evt.getHandlers()) {
            h.eventInactive(evt);
        }
    }

    class EventNotifyWorkItem implements WorkItem {
        private static final String prefix = "EVENT_EVENT_NOTIFY-";
        private static final String RAISE = "_RAISE";
        private static final String RTN = "_RTN";
        private static final String DEACTIVATED = "_DEACTIVATED";
        private static final String ACK = "_ACK";
        private static final String UNK = "_UNK";

        private final List<Integer> userIds;
        private final UserEventListener listener;
        private final EventInstance event;
        private final boolean raised;
        private final boolean returnToNormal;
        private final boolean deactivated;
        private final boolean acknowledged;

        EventNotifyWorkItem(List<Integer> userIds, UserEventListener listener, EventInstance event, boolean raised,
                boolean returnToNormal, boolean deactivated, boolean acknowledged) {
            this.userIds = userIds;
            this.listener = listener;
            this.event = event;
            this.raised = raised;
            this.returnToNormal = returnToNormal;
            this.deactivated = deactivated;
            this.acknowledged = acknowledged;

        }

        @Override
        public void execute() {
            event.setIdsToNotify(userIds);
            try {
                if(listener instanceof UserEventMulticaster) {
                    //Multi-cast
                    if(raised)
                        listener.raised(event);
                    else if(returnToNormal)
                        listener.returnToNormal(event);
                    else if(deactivated)
                        listener.deactivated(event);
                    else if(acknowledged)
                        listener.acknowledged(event);
                }else {
                    //Single listener, check ids first
                    if(event.getIdsToNotify().contains(listener.getUserId())) {
                        if(raised)
                            listener.raised(event);
                        else if(returnToNormal)
                            listener.returnToNormal(event);
                        else if(deactivated)
                            listener.deactivated(event);
                        else if(acknowledged)
                            listener.acknowledged(event);
                    }
                }

            } catch(ExceptionListWrapper e) {
                log.error("Exceptions in user event notify work item.");
                for(Exception e2 : e.getExceptions())
                    log.error("User event listener exception: " + e2.getMessage(), e2);
            }catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        @Override
        public int getPriority() {
            return WorkItem.PRIORITY_LOW;
        }

        /* (non-Javadoc)
         * @see com.serotonin.m2m2.rt.maint.work.WorkItem#getDescription()
         */
        @Override
        public String getDescription() {
            String type = "";
            if(raised)
                type = "raised";
            else if(deactivated)
                type = "deactivated";
            else if(returnToNormal)
                type = "return to normal";
            else if(acknowledged)
                type = "acknowledged";
            return "Event " + type + " Notification for event: " + event.getId();
        }

        /* (non-Javadoc)
         * @see com.serotonin.m2m2.rt.maint.work.WorkItem#getTaskId()
         */
        @Override
        public String getTaskId() {
            if(raised)
                return prefix + event.getId() + RAISE;
            else if(returnToNormal)
                return prefix + event.getId() +  RTN;
            else if(deactivated)
                return prefix + event.getId() + DEACTIVATED;
            else if(acknowledged)
                return prefix + event.getId() + ACK;
            else
                return prefix + event.getId() + UNK;
        }

        /* (non-Javadoc)
         * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getQueueSize()
         */
        @Override
        public int getQueueSize() {
            return Common.defaultTaskQueueSize;
        }

        /* (non-Javadoc)
         * @see com.serotonin.m2m2.rt.maint.work.WorkItem#rejected(com.serotonin.timer.RejectedTaskReason)
         */
        @Override
        public void rejected(RejectedTaskReason reason) {
            //No special handling, tracking/logging handled by WorkItemRunnable
        }

    }
}
