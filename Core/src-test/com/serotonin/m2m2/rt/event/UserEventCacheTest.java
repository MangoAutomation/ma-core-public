/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
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

import org.junit.Before;
import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.rt.event.type.EventType;

/**
 * A benchmarking test to validate changes to the UserEventCache
 * 
 * TODO Mango 3.5 Test cache.removeEvent()
 * TODO Mango 3.5 Test multiple inserters
 * TODO Mango 3.5 Test mutiple events at same time
 * 
 * TODO Mango 3.5 Test ack times
 * TODO Mango 3.5 Test rnt times
 * 
 * @author Terry Packer
 *
 */
public class UserEventCacheTest extends MangoTestBase{
	
	//Settings
	static final int EVENT_COUNT = 100000;
	static final int USER_COUNT = 10;
    Set<Integer> userIds;
	
    //Runtime 
	AtomicInteger runningThreads = new AtomicInteger(0);
	Object monitor = new Object();
	UserEventCache cache;

	public UserEventCacheTest() {
	    userIds = new HashSet<>();
        for(int i=1; i<USER_COUNT+1; i++)
            userIds.add(i);
	}
	
    @Before
    @Override
    public void before() {
        super.before();
        createUsers(USER_COUNT, SuperadminPermissionDefinition.GROUP_NAME);
    }
	
    @Test
    public void testPurgeAccuracy() throws InterruptedException {
        int timeToLive = EVENT_COUNT + 1;
        int cleanerPeriod = EVENT_COUNT * USER_COUNT + 100;
        this.cache = new UserEventCache(timeToLive,  cleanerPeriod);

        //Setup EventThread
        EventGeneratorThread egt = new EventGeneratorThread(this, EVENT_COUNT);
        
        //Setup User Threads
        List<UserThread> userThreads = new ArrayList<UserThread>();
        for(Integer userId : userIds){
            userThreads.add(new UserThread(userId, 99, this, egt));
        }
        
        //Wait to shutdown
        synchronized(monitor) {
            //Start Event Thread
            egt.setUserThreads(userThreads);
            egt.start();

            //Start User Threads
            for(UserThread ut : userThreads)
                ut.start();
            
            while(runningThreads.intValue() > 0) {
                monitor.wait();
            }
        }

        //We have purged all but 100 by now since time is still at the time of the last inserted Event
        assertEquals(100, this.cache.getEventCount());
        
        //Ensure the accuracy (The User Threads will have purged up to the last 100 events)
        egt.allEvents.sort((e1,e2) -> (int)(e2.getActiveTimestamp() - e1.getActiveTimestamp()));
        //Ensure we are in the proper order
        for(Integer userId : userIds){
            List<EventInstance> events = this.cache.getAllEvents(userId);
            assertTrue(events.size() > 1);
            EventInstance latest = events.get(0);
            for(int j=1; j<events.size(); j++) {
                EventInstance evt = events.get(j);
                if(latest.getActiveTimestamp() < evt.getActiveTimestamp())
                    fail("Events (" + latest.getId() + "," + evt.getId() + ") Timestamps out of order for user " + userId + ": " + latest.getActiveTimestamp() + " --> " + evt.getActiveTimestamp());
                assertEquals(egt.allEvents.get(j).getActiveTimestamp(), evt.getActiveTimestamp());
                latest = evt;
            }
        }
        
        //trigger Cleaner and then ensure all entries are removed
        this.timer.fastForwardTo(this.timer.currentTimeMillis() + timeToLive + cleanerPeriod);
        
        //should be totally empty
        assertEquals(0, this.cache.getCache().size());
        
    }
    
    @Test
    public void testInsertAccuracy() throws InterruptedException {
        int timeToLive = EVENT_COUNT + 1;
        int cleanerPeriod = EVENT_COUNT * USER_COUNT + 100;
        this.cache = new UserEventCache(timeToLive,  cleanerPeriod);

        //Setup EventThread
        EventGeneratorThread egt = new EventGeneratorThread(this, EVENT_COUNT);
        
        //Setup User Threads
        List<UserThread> userThreads = new ArrayList<UserThread>();
        for(Integer userId : userIds){
            userThreads.add(new UserThread(userId, null, this, egt));
        }
        
        //Wait to shutdown
        synchronized(monitor) {
            //Start Event Thread
            egt.setUserThreads(userThreads);
            egt.start();

            //Start User Threads
            for(UserThread ut : userThreads)
                ut.start();
            
            while(runningThreads.intValue() > 0) {
                monitor.wait();
            }
        }

        //Ensure the cache has all the events
        assertEquals(EVENT_COUNT, this.cache.getEventCount());

        //Ensure there all the entries for the users
        egt.allEvents.sort((e1,e2) -> (int)(e2.getActiveTimestamp() - e1.getActiveTimestamp()));
        for(Integer userId : userIds) {
            List<EventInstance> userEvents = this.cache.getAllEvents(userId);
            assertEquals(EVENT_COUNT, userEvents.size());
            for(int j=0; j<egt.allEvents.size(); j++) {
                assertEquals(egt.allEvents.get(j).getActiveTimestamp(), userEvents.get(j).getActiveTimestamp());
                assertEquals(egt.allEvents.get(j).getId(), userEvents.get(j).getId());
            }
        }

        //trigger Cleaner and then ensure all entries are removed
        this.timer.fastForwardTo(this.timer.currentTimeMillis() + timeToLive + cleanerPeriod);
        
        //should be totally empty
        assertEquals(0, this.cache.getCache().size());
        
    }
    
    //Enable this test to do many iterations, useful in testing
    // synchronization bugs
    public void testALot() throws InterruptedException {
        
        int iterations = 100;
        for(int i=0; i<iterations; i++) {
            System.out.println("iteration: " + i);
            testInsertAccuracy();
            cleanup();
            testPurgeAccuracy();
            cleanup();
        }
    }
    
    private void cleanup() {
        EventDao.instance.purgeAllEvents();
        this.cache.purgeAllEvents();
        this.cache.terminate();
        this.timer.reset();
    }
    
	class EventGeneratorThread extends Thread{
		
		private UserEventCacheTest parent;
		private int eventCount;
		private int totalCreated;
		List<EventInstance> allEvents;
		  //Signal to user threads that they should stop too
	    volatile AtomicBoolean generatorRunning = new AtomicBoolean(false);
	    private volatile List<UserThread> userThreads;
	    private int currentEventId = 1;
	    EventType eventType;
	    TranslatableMessage message;
	    Map<String, Object> context;
	    
		public EventGeneratorThread(UserEventCacheTest parent, int eventCount){
			super("Event Generator");
			this.parent = parent;
			this.eventCount = eventCount;
	        this.parent.runningThreads.incrementAndGet();
	        this.generatorRunning.set(true);  
            this.currentEventId = 1;
            this.eventType = new DataPointEventType(1, 1);
            this.message =
                    new TranslatableMessage("common.default", "not a real alarm");
            this.context = new HashMap<String, Object>();
            this.allEvents = new ArrayList<EventInstance>();

	        insertEvents(10);
		}
		
		public void setUserThreads(List<UserThread> userThreads) {
		    this.userThreads = userThreads;
		}
		
		public void insertEvents(int count) {
            
            for(int i=0; i<count; i++) {
                timer.fastForwardTo(timer.currentTimeMillis() + 1);
                EventInstance e = new EventInstance(
                        eventType,
                        Common.timer.currentTimeMillis(), 
                        true,
                        AlarmLevels.CRITICAL,
                        message,
                        context);
                //Important
                EventDao.instance.saveEvent(e);
                EventDao.instance.insertUserEvents(e.getId(), new ArrayList<>(userIds), true);
                allEvents.add(e);
                currentEventId = e.getId();
                totalCreated++;
            }
            //Bump to next available id for thread to use
            if(count > 1)
                currentEventId++;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {    
	        //Wait for the user threads to start
		    while(true) {
		        int count = 0;
		        for(UserThread t : userThreads)
		            if(t.running.get())
		                count++;
		        if(count == userThreads.size())
		            break;
		        try {Thread.sleep(100);}catch(InterruptedException e) {}
		    }
		    
		    //Pre-load all the users' caches to exist so we for sure insert values
			for(int userId=1; userId<USER_COUNT+1; userId++) {
			    parent.cache.getAllEvents(userId);
			}
			
			
			//Raise Events
			for(; totalCreated<eventCount; totalCreated++){
	            timer.fastForwardTo(timer.currentTimeMillis() + 1);
				EventInstance e = new EventInstance(
						eventType,
						Common.timer.currentTimeMillis(), 
						true,
						AlarmLevels.CRITICAL,
						message,
						context);
				//Important
				e.setId(currentEventId);
				currentEventId++;
				allEvents.add(e);
				parent.cache.addEvent(userIds, e);
			}
			
			//Return them to normal
			for(EventInstance e : allEvents){
				e.setAcknowledgedByUserId(1);
				e.setAcknowledgedTimestamp(Common.timer.currentTimeMillis());
				parent.cache.updateEvent(userIds, e);
				timer.fastForwardTo(timer.currentTimeMillis() + 1);
			}
			
			//Randomly remove some, simulate acknowledge
			for(EventInstance e : allEvents){
				if(Math.random() > 0.5d)
				    parent.cache.updateEvent(userIds, e);
			}
			
			synchronized(parent.monitor){
	            generatorRunning.set(false);
				parent.runningThreads.decrementAndGet();
				parent.monitor.notify();
			}
		}
	}
	
	class UserThread extends Thread{
		
		private UserEventCacheTest parent;
		private Integer userId;
		private Integer purgeBeforeMs;
		private volatile EventGeneratorThread generator;
		volatile AtomicBoolean running = new AtomicBoolean(false);
		
		public UserThread(Integer userId, Integer purgeBeforeMs, UserEventCacheTest parent, EventGeneratorThread generator){
			super("User Thread " + userId);
			this.parent = parent;
			this.userId = userId;
			this.purgeBeforeMs = purgeBeforeMs;
			this.generator = generator;
			this.parent.runningThreads.incrementAndGet();
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
	        running.set(true);
			while(generator.generatorRunning.get() == true){
				
				//Get our events events
			    List<EventInstance> events = parent.cache.getAllEvents(userId);
			    
			    //Confirm they are in order 
			    if(events.size() > 0) {
    			    EventInstance latest = events.get(0);
    			    for(int i=1; i<events.size(); i++) {
    			        EventInstance evt = events.get(i);
    			        if(latest.getActiveTimestamp() < evt.getActiveTimestamp())
    			            LOG.warn("Events (" + latest.getId() + "," + evt.getId() + ") Timestamps out of order for user " + userId + ": " + latest.getActiveTimestamp() + " --> " + evt.getActiveTimestamp());
    			        if(purgeBeforeMs == null && latest.getActiveTimestamp() - evt.getActiveTimestamp() > 1)
    			            LOG.warn("Events (" + latest.getId() + "," + evt.getId() + ") Timestamps not 1ms between for user "  + userId + ": " + latest.getActiveTimestamp() + " --> " + evt.getActiveTimestamp());
    			        latest = evt;
    			    }
			    }
			    
                //Purge some 
                if(purgeBeforeMs != null) {
                    if((events.size() > 0)&&(events.get(0).getActiveTimestamp() > purgeBeforeMs)) {
                        parent.cache.purgeEventsBefore(events.get(0).getActiveTimestamp() - purgeBeforeMs);
                    }
                }
			}
			
			//Do the final purge to ensure we have the proper number left over
            //Purge some 
            if(purgeBeforeMs != null) {
                List<EventInstance> events = parent.cache.getAllEvents(userId);
                if((events.size() > 0)&&(events.get(0).getActiveTimestamp() > purgeBeforeMs)) {
                    parent.cache.purgeEventsBefore(events.get(0).getActiveTimestamp() - purgeBeforeMs);
                }
            }
			
			synchronized(parent.monitor){
			    running.set(false);
				parent.runningThreads.decrementAndGet();
				parent.monitor.notify();
			}
			
		}
	}

}
