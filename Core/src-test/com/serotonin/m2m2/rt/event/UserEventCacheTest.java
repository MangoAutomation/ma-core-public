/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 *
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * A benchmarking test to validate changes to the UserEventCache
 *
 * TODO Mango 4.0 Test multiple inserters
 * TODO Mango 4.0 Test ack times
 * TODO Mango 4.0 Test rnt times
 * TODO Mango 4.0 Test removeUser
 *
 * @author Terry Packer
 *
 */
public class UserEventCacheTest extends MangoTestBase {

    // Runtime
    AtomicInteger runningThreads = new AtomicInteger(0);
    Object monitor = new Object();
    UserEventCache cache;

    @Test
    public void testMultipleEventsAtSameTime() {
        List<User> users = createUsers(1, PermissionHolder.SUPERADMIN_ROLE.get());
        Set<Integer> userIds = new HashSet<>();
        for(User user : users)
            userIds.add(user.getId());

        int cleanerPeriod = 10000;
        int timeToLive = cleanerPeriod * 4;
        int eventCount = 100;
        this.cache = new UserEventCache(timeToLive, cleanerPeriod);

        //Preload cache for users
        for(User user : users)
            cache.getAllEvents(user.getId());

        EventType eventTypeOne = new DataPointEventType(1, 1);
        EventType eventTypeTwo = new DataPointEventType(1, 2);
        TranslatableMessage message = new TranslatableMessage("common.default", "not a real alarm");
        Map<String,Object> context = new HashMap<>();
        List<EventInstance> allEventOnes = new ArrayList<EventInstance>();
        List<EventInstance> allEventTwos = new ArrayList<EventInstance>();

        for (int i = 0; i < eventCount; i++) {
            timer.fastForwardTo(timer.currentTimeMillis() + 1);
            EventInstance e = new EventInstance(eventTypeOne, Common.timer.currentTimeMillis(),
                    true, AlarmLevels.CRITICAL, message, context);
            // Important
            EventDao.getInstance().saveEvent(e);
            EventDao.getInstance().insertUserEvents(e.getId(), new ArrayList<>(userIds), true);
            allEventOnes.add(e);
            cache.addEvent(userIds, e);

            EventInstance e2 = new EventInstance(eventTypeTwo, Common.timer.currentTimeMillis(),
                    true, AlarmLevels.CRITICAL, message, context);
            // Important
            EventDao.getInstance().saveEvent(e2);
            EventDao.getInstance().insertUserEvents(e2.getId(), new ArrayList<>(userIds), true);
            allEventTwos.add(e2);
            cache.addEvent(userIds, e2);
        }

        //Confirm that the cache size is still eventCount
        assertEquals(eventCount, cache.getCache().size());

        //Test Remove 10, 20, 30, 40 for Both event types
        cache.removeEvent(allEventOnes.get(10));
        cache.removeEvent(allEventOnes.get(20));
        cache.removeEvent(allEventOnes.get(30));
        cache.removeEvent(allEventOnes.get(40));

        cache.removeEvent(allEventTwos.get(10));
        cache.removeEvent(allEventTwos.get(20));
        cache.removeEvent(allEventTwos.get(30));
        cache.removeEvent(allEventTwos.get(40));

        assertEquals(eventCount - 4, cache.getCache().size());

        //Test Remove only event 1, cache size should not change
        cache.removeEvent(allEventOnes.get(11));
        cache.removeEvent(allEventOnes.get(21));
        cache.removeEvent(allEventOnes.get(31));
        cache.removeEvent(allEventOnes.get(41));
        assertEquals(eventCount - 4, cache.getCache().size());


        //Test remove user from only 1 event at that time
        cache.removeUser(users.get(0).getId(), allEventOnes.get(1).getId());
        cache.removeUser(users.get(0).getId(), allEventOnes.get(2).getId());

        // Give the cleaner time to clean up and remove dead events
        this.timer.fastForwardTo(this.timer.currentTimeMillis() + (cleanerPeriod * 2));

        //No size change should occur
        assertEquals(eventCount - 4, cache.getCache().size());

        //Remove the user from the other event at that time and should see size change
        cache.removeUser(users.get(0).getId(), allEventTwos.get(1).getId());
        cache.removeUser(users.get(0).getId(), allEventTwos.get(2).getId());

        // Give the cleaner time to clean up and remove dead events
        this.timer.fastForwardTo(this.timer.currentTimeMillis() + (cleanerPeriod * 2));

        //No size change should occur
        assertEquals(eventCount - 4 - 2, cache.getCache().size());
    }



    @Test
    public void testRemoveEvents() throws InterruptedException {
        List<User> users = createUsers(1, PermissionHolder.SUPERADMIN_ROLE.get());
        int timeToLive = 1000000000;
        int cleanerPeriod = 100000000;
        int eventCount = 100;
        this.cache = new UserEventCache(timeToLive, cleanerPeriod);

        // Setup EventThread
        EventGeneratorThread egt = new EventGeneratorThread(this, eventCount, users);
        egt.setUserThreads(new ArrayList<>());
        cache.getAllEvents(users.get(0).getId());

        // Wait to shutdown
        synchronized (monitor) {
            // Start Event Thread
            egt.start();

            while (runningThreads.intValue() > 0) {
                monitor.wait();
            }
        }

        int removed = eventCount / 2;
        for (int i = 0; i < removed; i++)
            this.cache.removeEvent(egt.allEvents.get(i));

        // Count all the removed events
        assertEquals(eventCount - removed, this.cache.getCache().size());
    }

    @Test
    public void testRemoveEventsForSingleUser() throws InterruptedException {
        List<User> users = createUsers(1, PermissionHolder.SUPERADMIN_ROLE.get());
        int cleanerPeriod = 10000;
        int timeToLive = cleanerPeriod * 4;
        int eventCount = 100;
        this.cache = new UserEventCache(timeToLive, cleanerPeriod);

        // Setup EventThread
        EventGeneratorThread egt = new EventGeneratorThread(this, eventCount, users);
        egt.setUserThreads(new ArrayList<>());
        cache.getAllEvents(users.get(0).getId());

        // Wait to shutdown
        synchronized (monitor) {
            // Start Event Thread
            egt.start();

            while (runningThreads.intValue() > 0) {
                monitor.wait();
            }
        }

        // Since we are the only user then this should remove the full event
        int removed = eventCount / 2;
        for (int i = 0; i < removed; i++)
            this.cache.removeUser(users.get(0).getId(), egt.allEvents.get(i).getId());

        // Give the cleaner time to clean up and remove dead events
        this.timer.fastForwardTo(this.timer.currentTimeMillis() + (cleanerPeriod * 2));

        // Count all the removed events
        assertEquals(eventCount - removed, this.cache.getCache().size());
    }

    @Test
    public void testRemoveEventsForUsers() throws InterruptedException {
        List<User> users = createUsers(2, PermissionHolder.SUPERADMIN_ROLE.get());
        int cleanerPeriod = 10000;
        int timeToLive = cleanerPeriod * 4;
        int eventCount = 100;
        this.cache = new UserEventCache(timeToLive, cleanerPeriod);

        // Setup EventThread
        EventGeneratorThread egt = new EventGeneratorThread(this, eventCount, users);
        egt.setUserThreads(new ArrayList<>());

        //Preload cache for users
        for(User user : users)
            cache.getAllEvents(user.getId());

        // Wait to shutdown
        synchronized (monitor) {
            // Start Event Thread
            egt.start();

            while (runningThreads.intValue() > 0) {
                monitor.wait();
            }
        }

        // Remove User 1 from Events 10, 20 and 30
        cache.removeUser(users.get(0).getId(), egt.allEvents.get(10).getId());
        cache.removeUser(users.get(0).getId(), egt.allEvents.get(20).getId());
        cache.removeUser(users.get(0).getId(), egt.allEvents.get(30).getId());

        // Remove User 2 from Event 5, 10, 20 and 40
        cache.removeUser(users.get(1).getId(), egt.allEvents.get(5).getId());
        cache.removeUser(users.get(1).getId(), egt.allEvents.get(10).getId());
        cache.removeUser(users.get(1).getId(), egt.allEvents.get(20).getId());
        cache.removeUser(users.get(1).getId(), egt.allEvents.get(40).getId());

        // Give the cleaner time to clean up and remove dead events
        this.timer.fastForwardTo(this.timer.currentTimeMillis() + (cleanerPeriod * 2));

        // This should result in only 2 events being removed from the cache
        // Count all the removed events
        assertEquals(eventCount - 2, this.cache.getCache().size());
    }

    @Test
    public void testPurgeAccuracy() throws InterruptedException {
        int userCount = 10;
        List<User> users = createUsers(1, PermissionHolder.SUPERADMIN_ROLE.get());
        int eventCount = 10000;
        int timeToLive = eventCount + 1;
        int cleanerPeriod = eventCount * userCount + 100;
        this.cache = new UserEventCache(timeToLive, cleanerPeriod);

        // Setup EventThread
        EventGeneratorThread egt = new EventGeneratorThread(this, eventCount, users);

        // Setup User Threads
        List<UserThread> userThreads = new ArrayList<UserThread>();
        for (User user : users) {
            userThreads.add(new UserThread(user.getId(), 99, this, egt));
        }

        // Wait to shutdown
        synchronized (monitor) {
            // Start Event Thread
            egt.setUserThreads(userThreads);
            egt.start();

            // Start User Threads
            for (UserThread ut : userThreads)
                ut.start();

            while (runningThreads.intValue() > 0) {
                monitor.wait();
            }
        }

        // We have purged all but 100 by now since time is still at the time of the last inserted
        // Event
        assertEquals(100, this.cache.getEventCount());

        // Ensure the accuracy (The User Threads will have purged up to the last 100 events)
        egt.allEvents.sort((e1, e2) -> (int) (e2.getActiveTimestamp() - e1.getActiveTimestamp()));
        // Ensure we are in the proper order
        for (User user : users) {
            List<EventInstance> events = this.cache.getAllEvents(user.getId());
            assertTrue(events.size() > 1);
            EventInstance latest = events.get(0);
            for (int j = 1; j < events.size(); j++) {
                EventInstance evt = events.get(j);
                if (latest.getActiveTimestamp() < evt.getActiveTimestamp())
                    fail("Events (" + latest.getId() + "," + evt.getId()
                    + ") Timestamps out of order for user " + user.getId() + ": "
                    + latest.getActiveTimestamp() + " --> " + evt.getActiveTimestamp());
                assertEquals(egt.allEvents.get(j).getActiveTimestamp(), evt.getActiveTimestamp());
                latest = evt;
            }
        }

        // trigger Cleaner and then ensure all entries are removed
        this.timer.fastForwardTo(this.timer.currentTimeMillis() + timeToLive + cleanerPeriod);

        // should be totally empty
        assertEquals(0, this.cache.getCache().size());

    }

    @Test
    public void testInsertAccuracy() throws InterruptedException {
        int userCount = 10;
        List<User> users = createUsers(1, PermissionHolder.SUPERADMIN_ROLE.get());
        int eventCount = 10000;
        int timeToLive = eventCount + 1;
        int cleanerPeriod = eventCount * userCount + 100;
        this.cache = new UserEventCache(timeToLive, cleanerPeriod);

        // Setup EventThread
        EventGeneratorThread egt = new EventGeneratorThread(this, eventCount, users);

        // Setup User Threads
        List<UserThread> userThreads = new ArrayList<UserThread>();
        for (User user : users) {
            userThreads.add(new UserThread(user.getId(), null, this, egt));
        }

        // Wait to shutdown
        synchronized (monitor) {
            // Start Event Thread
            egt.setUserThreads(userThreads);
            egt.start();

            // Start User Threads
            for (UserThread ut : userThreads)
                ut.start();

            while (runningThreads.intValue() > 0) {
                monitor.wait();
            }
        }

        // Ensure the cache has all the events
        assertEquals(eventCount, this.cache.getEventCount());

        // Ensure there all the entries for the users
        egt.allEvents.sort((e1, e2) -> (int) (e2.getActiveTimestamp() - e1.getActiveTimestamp()));
        for (User user : users) {
            List<EventInstance> userEvents = this.cache.getAllEvents(user.getId());
            assertEquals(eventCount, userEvents.size());
            for (int j = 0; j < egt.allEvents.size(); j++) {
                assertEquals(egt.allEvents.get(j).getActiveTimestamp(),
                        userEvents.get(j).getActiveTimestamp());
                assertEquals(egt.allEvents.get(j).getId(), userEvents.get(j).getId());
            }
        }

        // trigger Cleaner and then ensure all entries are removed
        this.timer.fastForwardTo(this.timer.currentTimeMillis() + timeToLive + cleanerPeriod);

        // should be totally empty
        assertEquals(0, this.cache.getCache().size());

    }

    // Enable this test to do many iterations, useful in testing
    // synchronization bugs
    public void testALot() throws InterruptedException {

        int iterations = 100;
        for (int i = 0; i < iterations; i++) {
            System.out.println("iteration: " + i);
            testInsertAccuracy();
            cleanup();
            testPurgeAccuracy();
            cleanup();
        }
    }

    private void cleanup() {
        EventDao.getInstance().purgeAllEvents();
        this.cache.purgeAllEvents();
        this.cache.terminate();
        this.timer.reset();
    }

    class EventGeneratorThread extends Thread {

        private UserEventCacheTest parent;
        private int eventCount;
        private int totalCreated;
        List<EventInstance> allEvents;
        // Signal to user threads that they should stop too
        volatile AtomicBoolean generatorRunning = new AtomicBoolean(false);
        private volatile List<UserThread> userThreads;
        private int currentEventId = 1;
        EventType eventType;
        TranslatableMessage message;
        Map<String, Object> context;
        List<User> users;
        Set<Integer> userIds;

        public EventGeneratorThread(UserEventCacheTest parent, int eventCount, List<User> users) {
            super("Event Generator");
            this.parent = parent;
            this.eventCount = eventCount;
            this.parent.runningThreads.incrementAndGet();
            this.generatorRunning.set(true);
            this.currentEventId = 1;
            this.eventType = new DataPointEventType(1, 1);
            this.message = new TranslatableMessage("common.default", "not a real alarm");
            this.context = new HashMap<String, Object>();
            this.allEvents = new ArrayList<EventInstance>();
            this.users = users;
            this.userIds = new HashSet<>();
            for (User user : users)
                userIds.add(user.getId());

            insertEvents(10);
        }

        public void setUserThreads(List<UserThread> userThreads) {
            this.userThreads = userThreads;
        }

        public void insertEvents(int count) {

            for (int i = 0; i < count; i++) {
                timer.fastForwardTo(timer.currentTimeMillis() + 1);
                EventInstance e = new EventInstance(eventType, Common.timer.currentTimeMillis(),
                        true, AlarmLevels.CRITICAL, message, context);
                // Important
                EventDao.getInstance().saveEvent(e);
                EventDao.getInstance().insertUserEvents(e.getId(), new ArrayList<>(userIds), true);
                allEvents.add(e);
                currentEventId = e.getId();
                totalCreated++;
            }
            // Bump to next available id for thread to use
            if (count > 1)
                currentEventId++;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            // Wait for the user threads to start
            while (true) {
                int count = 0;
                for (UserThread t : userThreads)
                    if (t.running.get())
                        count++;
                if (count == userThreads.size())
                    break;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }

            // Pre-load all the users' caches to exist so we for sure insert values
            for (User user : users) {
                parent.cache.getAllEvents(user.getId());
            }


            // Raise Events
            for (; totalCreated < eventCount; totalCreated++) {
                timer.fastForwardTo(timer.currentTimeMillis() + 1);
                EventInstance e = new EventInstance(eventType, Common.timer.currentTimeMillis(),
                        true, AlarmLevels.CRITICAL, message, context);
                // Important
                e.setId(currentEventId);
                currentEventId++;
                allEvents.add(e);
                parent.cache.addEvent(userIds, e);
            }

            // Acknowledge some
            for (EventInstance e : allEvents) {
                e.setAcknowledgedByUserId(1);
                e.setAcknowledgedTimestamp(Common.timer.currentTimeMillis());
                parent.cache.updateEvent(userIds, e);
                timer.fastForwardTo(timer.currentTimeMillis() + 1);
            }

            // Randomly remove some, simulate Rtn
            for (EventInstance e : allEvents) {
                if (Math.random() > 0.5d)
                    parent.cache.updateEvent(userIds, e);
            }

            synchronized (parent.monitor) {
                generatorRunning.set(false);
                parent.runningThreads.decrementAndGet();
                parent.monitor.notify();
            }
        }
    }

    class UserThread extends Thread {

        private UserEventCacheTest parent;
        private Integer userId;
        private Integer purgeBeforeMs;
        private volatile EventGeneratorThread generator;
        volatile AtomicBoolean running = new AtomicBoolean(false);

        public UserThread(Integer userId, Integer purgeBeforeMs, UserEventCacheTest parent,
                EventGeneratorThread generator) {
            super("User Thread " + userId);
            this.parent = parent;
            this.userId = userId;
            this.purgeBeforeMs = purgeBeforeMs;
            this.generator = generator;
            this.parent.runningThreads.incrementAndGet();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            running.set(true);
            while (generator.generatorRunning.get() == true) {

                // Get our events events
                List<EventInstance> events = parent.cache.getAllEvents(userId);

                // Confirm they are in order
                if (events.size() > 0) {
                    EventInstance latest = events.get(0);
                    for (int i = 1; i < events.size(); i++) {
                        EventInstance evt = events.get(i);
                        if (latest.getActiveTimestamp() < evt.getActiveTimestamp())
                            LOG.warn("Events (" + latest.getId() + "," + evt.getId()
                            + ") Timestamps out of order for user " + userId + ": "
                            + latest.getActiveTimestamp() + " --> "
                            + evt.getActiveTimestamp());
                        if (purgeBeforeMs == null
                                && latest.getActiveTimestamp() - evt.getActiveTimestamp() > 1)
                            LOG.warn("Events (" + latest.getId() + "," + evt.getId()
                            + ") Timestamps not 1ms between for user " + userId + ": "
                            + latest.getActiveTimestamp() + " --> "
                            + evt.getActiveTimestamp());
                        latest = evt;
                    }
                }

                // Purge some
                if (purgeBeforeMs != null) {
                    if ((events.size() > 0)
                            && (events.get(0).getActiveTimestamp() > purgeBeforeMs)) {
                        parent.cache.purgeEventsBefore(
                                events.get(0).getActiveTimestamp() - purgeBeforeMs);
                    }
                }
            }

            // Do the final purge to ensure we have the proper number left over
            // Purge some
            if (purgeBeforeMs != null) {
                List<EventInstance> events = parent.cache.getAllEvents(userId);
                if ((events.size() > 0) && (events.get(0).getActiveTimestamp() > purgeBeforeMs)) {
                    parent.cache
                    .purgeEventsBefore(events.get(0).getActiveTimestamp() - purgeBeforeMs);
                }
            }

            synchronized (parent.monitor) {
                running.set(false);
                parent.runningThreads.decrementAndGet();
                parent.monitor.notify();
            }

        }
    }

}
