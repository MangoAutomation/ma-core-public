/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.web.taglib.Functions;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.TimerTask;

/**
 * Cache for events for each user, used to improve event access performance for logged in users at
 * the expense of memory use.
 *
 * The events cache is a map of time stamps to a list of events since multiple events can exist
 * at the same times stamp.
 *
 * Events in this list are:
 *
 * 1.  Inserted for any user that has recently accessed their recent events
 * 2.  Removed when they are acknowledged by a user
 * 3.  Updated when they RTN or are Deactivated
 *
 *
 * If a user has not accessed their cache in timeToLive ms then the entry is cleaned up.
 * Stale entries are discarded every timeInterval.
 *
 * @author Terry Packer
 *
 */
public class UserEventCache extends TimeoutClient{
    private final Log LOG = LogFactory.getLog(UserEventCache.class);

    private final static Comparator<Long> TIMESTAMP_DESC = new Comparator<Long>() {
        @Override
        public int compare(Long o1, Long o2) {
            return (int)(o2-o1);
        }

    };

    private long timeToLive;
    //List of events to Users in ActiveTs descending order, there can be > 1 event at any given time
    private ConcurrentMap<Long, List<MultiUserEvent>> cache;
    //Track users that have accessed the cache for a read
    private ConcurrentMap<Integer, Long> activeUsers;
    private TimerTask timerTask;

    //Task management
    private boolean running;
    private Thread jobThread;
    private Object terminationLock;

    /**
     *
     * @param timeToLive
     * @param timeInterval
     */
    public UserEventCache(long timeToLive, final long timeInterval) {
        this.timeToLive = timeToLive;
        this.cache = new ConcurrentSkipListMap<Long, List<MultiUserEvent>>(TIMESTAMP_DESC);
        this.activeUsers = new ConcurrentHashMap<>();
        this.timerTask = new TimeoutTask(new FixedRateTrigger(timeInterval, timeInterval), this);
        this.running = true;
    }

    protected boolean isActive(Integer userId) {
        if(activeUsers.get(userId) != null)
            return true;
        else
            return false;
    }

    protected Set<Integer> pruneInactive(Set<Integer> userIds){
        Set<Integer> active = new HashSet<>(userIds.size());
        for(Integer id : userIds)
            if(activeUsers.get(id) != null)
                active.add(id);
        return active;
    }


    /**
     * Add an event for users if they are active
     * @param userIds
     * @param value
     */
    public void addEvent(Set<Integer> userIds, EventInstance value) {
        Set<Integer> active = pruneInactive(userIds);
        if(active.size() == 0)
            return;
        cache.compute(value.getActiveTimestamp(), (k,v) -> {
            if(v == null) {
                v = new CopyOnWriteArrayList<>();
                v.add(new MultiUserEvent(value, active));
            }else {
                MultiUserEvent event = null;
                for(MultiUserEvent e : v) {
                    if(e.event.getId() == value.getId()) {
                        event = e;
                        break;
                    }
                }
                if(event == null) {
                    event = new MultiUserEvent(value, active);
                    v.add(event);
                }else {
                    event.users.addAll(active);
                }
            }
            return v;
        });
    }


    /**
     * Update an event with the new instance
     * @param userIds
     * @param value
     */
    public void updateEvent(Set<Integer> userIds, EventInstance value) {
        Set<Integer> active = pruneInactive(userIds);
        this.cache.computeIfPresent(value.getActiveTimestamp(), (k,v) -> {
            MultiUserEvent event = null;
            for(MultiUserEvent e : v) {
                if(e.event.getId() == value.getId()) {
                    event = e;
                    break;
                }
            }
            if(event != null)
                event.update(value, active);
            return v;
        });
    }

    /**
     * Remove an event from the cache
     * @param evt
     */
    public void removeEvent(EventInstance evt) {
        this.cache.computeIfPresent(evt.getActiveTimestamp(), (k,v)->{
            v.removeIf(mue -> mue.event.getId() == evt.getId());
            if(v.size() == 0)
                return null;
            else
                return v;
        });
    }

    /**
     * Get all events for a user, if no events exist in cache then an entry is created
     * and filled with all unsilenced events from the database.
     * @param userId
     * @return
     */
    public List<EventInstance> getAllEvents(Integer userId) {
        if(!isActive(userId)) {
            //Collect events and add to our list
            List<EventInstance> userEvents = EventDao.getInstance().getAllUnsilencedEvents(userId);
            List<EventInstance> view = new ArrayList<>(userEvents.size());
            userEvents.forEach(e -> {
                cache.compute(e.getActiveTimestamp(), (k,v) ->{
                    if(v == null) {
                        v = new CopyOnWriteArrayList<>();
                        v.add(new MultiUserEvent(e, userId));
                    }else {
                        MultiUserEvent event = null;
                        for(MultiUserEvent mue : v) {
                            if(mue.event.getId() == e.getId()) {
                                event = mue;
                                break;
                            }
                        }
                        if(event == null) {
                            event = new MultiUserEvent(e, userId);
                            v.add(event);
                        }else {
                            event.users.add(userId);
                        }
                    }
                    return v;
                });
                view.add(e);
            });
            this.activeUsers.compute(userId, (k,v) -> Common.timer.currentTimeMillis());
            return view;
        }else {
            //Get user's view
            List<EventInstance> view = new ArrayList<>();
            cache.forEach((k,v) -> {
                for(MultiUserEvent mue : v) {
                    if(mue.hasAccess(userId))
                        view.add(mue.event);
                }
            });
            this.activeUsers.compute(userId, (k,v) -> Common.timer.currentTimeMillis());
            return view;
        }
    }

    /**
     * Add a user to a cached event (when user un-silences an event)
     * @param userId
     * @param eventId
     */
    public void addUser(Integer userId, int eventId) {
        MultiUserEvent event = findEvent(eventId);
        if(event != null)
            event.users.add(userId);
    }

    /**
     * Remove a user from a cached event (when user silences an event),
     *   if no users remain for this event it will eventually be removed by
     *   the cleaner.
     * @param userId
     * @param eventId
     */
    public void removeUser(Integer userId, int eventId) {
        MultiUserEvent event = findEvent(eventId);
        if(event != null)
            event.users.remove(userId);
    }

    /**
     * Find an event with this id in our cache
     * @param eventId
     * @return
     */
    protected MultiUserEvent findEvent(int eventId) {
        MultiUserEvent event = null;
        //Find the event (There should only be one
        for(List<MultiUserEvent> events : cache.values()){
            for(MultiUserEvent e : events) {
                if(e.event.getId() == eventId) {
                    event = e;
                    break;
                }
            }
            if(event != null)
                break;
        }

        return event;
    }

    public Map<Long, List<MultiUserEvent>> getCache(){
        return cache;
    }

    public int getEventCount() {
        MutableInt count = new MutableInt();
        cache.forEach((k,v) -> {
            count.add(v.size());
        });
        return count.getValue();
    }

    /**
     *
     * @param time
     */
    public void purgeEventsBefore(long time){
        cache.forEach((k,v) -> {
            if(k < time)
                cache.remove(k);
        });
    }

    public void purgeEventsBefore(long time, AlarmLevels alarmLevel){
        cache.forEach((k,v) -> {
            v.removeIf(e -> (e.event.getActiveTimestamp() < time) && (e.event.getAlarmLevel() == alarmLevel));
            if(v.size() == 0)
                cache.remove(k);
        });
    }

    public void purgeEventsBefore(long time, String typeName){
        cache.forEach((k,v) -> {
            v.removeIf(e -> (e.event.getActiveTimestamp() < time) && (e.event.getEventType().getEventType().equals(typeName)));
            if(v.size() == 0)
                cache.remove(k);
        });
    }

    /**
     * Clear Events for all users
     */
    public void purgeAllEvents() {
        cache.clear();
    }

    // CLEANUP method
    public void cleanup() {
        if (!running)
            return;
        long now = Common.timer.currentTimeMillis();
        Set<Integer> removeUserIds = new HashSet<>();
        activeUsers.forEach((k, v) -> {
            if(now > (timeToLive + v)) {
                activeUsers.remove(k);
                removeUserIds.add(k);
            }
        });
        //Remove from the cache
        cache.forEach((k,v) -> {
            for(MultiUserEvent mue : v) {
                mue.users.removeAll(removeUserIds);
                if(mue.users.isEmpty())
                    v.remove(mue);
            }
            if(v.isEmpty())
                cache.remove(k);
        });

    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.util.timeout.TimeoutClient#scheduleTimeout(long)
     */
    @Override
    public void scheduleTimeout(long fireTime) {
        if (jobThread != null) {
            // There is another poll still running, so abort this one.
            LOG.warn("Cleanup at :"+ Functions.getFullSecondTime(fireTime)
            + " aborted because a previous Cleanup is still running");
            return;
        }
        try{
            jobThread = Thread.currentThread();
            cleanup();
        }catch(Exception e){
            LOG.error(e.getMessage(), e);
        }finally{
            jobThread = null;
        }
    }

    public void terminate(){
        if (timerTask != null)
            timerTask.cancel();
        this.running = false;
    }

    public void joinTermination(){
        if (jobThread == null)
            return;

        terminationLock = new Object();

        int tries = 30;
        while (true) {
            synchronized (terminationLock) {
                Thread localThread = jobThread;
                if (localThread == null)
                    break;

                try {
                    terminationLock.wait(10000);
                }
                catch (InterruptedException e) {
                    // no op
                }

                if (jobThread != null) {
                    if (tries-- > 0)
                        LOG.warn("Waiting for UserEventCache cleaner task to stop");
                }
            }
        }
        LOG.info("UserEventCache cleaner task terminated.");
    }
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getName()
     */
    @Override
    public String getThreadName() {
        return "User event cache cleaner";
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.util.timeout.TimeoutClient#getTaskId()
     */
    @Override
    public String getTaskId() {
        return "UserEventCacheCleaner";
    }

    public class MultiUserEvent {

        private EventInstance event;
        private Set<Integer> users;

        public MultiUserEvent(EventInstance event, Set<Integer> users) {
            this.event = event;
            this.users = ConcurrentHashMap.newKeySet();
            for(Integer user : users)
                this.users.add(user);
        }

        public MultiUserEvent(EventInstance event, Integer userId) {
            this.event = event;
            this.users = ConcurrentHashMap.newKeySet();
            this.users.add(userId);
        }

        public void update(EventInstance event, Set<Integer> users) {
            this.event = event;
            this.users.addAll(users);
        }

        public boolean hasAccess(Integer userId) {
            return this.users.contains(userId);
        }
        public void addUser(Integer userId) {
            this.users.add(userId);
        }
        public void removeUser(Integer userId) {
            this.users.remove(userId);
        }
        public void removeUsers(Set<Integer> userIds) {
            this.users.removeAll(userIds);
        }
    }
}
