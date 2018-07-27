/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.MangoTestBase;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.rt.event.type.EventType;

/**
 * A benchmarking test to validate changes to the UserEventCache
 * 
 * 
 * @author Terry Packer
 *
 */
public class UserEventCacheTest extends MangoTestBase{
	
	//Settings
	static final int EVENT_COUNT = 100000;
	static final int USER_COUNT = 10;
	
	//Runtime 
	AtomicInteger runningThreads = new AtomicInteger(0);
	Object monitor = new Object();
	UserEventCache cache;
	//Signal to user threads that they should stop too
	AtomicBoolean generatorRunning = new AtomicBoolean(true);
	
    @Before
    @Override
    public void before() {
        super.before();
    }

	/**
	 * To simulate Mango we will have 1 thread generating events 
	 * and occasionally while several other threads read their user's events out.
	 */
    @Test
	public void testInsertAndClean() throws InterruptedException {
		
        int timeToLive = 1000;
        int cleanerPeriod = EVENT_COUNT * USER_COUNT + 100;
		this.cache = new UserEventCache(timeToLive,  cleanerPeriod);

		//Setup EventThread
		EventGeneratorThread egt = new EventGeneratorThread(this, EVENT_COUNT);
		
		//Setup User Threads
		List<UserThread> userThreads = new ArrayList<UserThread>();
		for(int i=0; i<USER_COUNT; i++){
			userThreads.add(new UserThread(i, false, this));
		}
		
		//Start User Threads
		for(UserThread ut : userThreads)
			ut.start();
		
		//Start Event Thread
		egt.start();
		
		while(runningThreads.intValue() > 0){
		    Thread.sleep(100);
		}

		//Ensure there area all the entries for the users
		assertEquals(USER_COUNT, this.cache.getCache().size());
		
		//trigger Cleaner and then ensure all entries are removed
		this.timer.fastForwardTo(this.timer.currentTimeMillis() + timeToLive + cleanerPeriod);
		
		//should be totally empty
		assertEquals(0, this.cache.getCache().size());
		
	}
	
    @Test
    public void testPurgeAccuracy() throws InterruptedException {
        
        int timeToLive = 1000;
        int cleanerPeriod = EVENT_COUNT * USER_COUNT + 100;
        this.cache = new UserEventCache(timeToLive,  cleanerPeriod);

        //Setup EventThread
        EventGeneratorThread egt = new EventGeneratorThread(this, EVENT_COUNT);
        
        //Setup User Threads
        List<UserThread> userThreads = new ArrayList<UserThread>();
        for(int i=0; i<USER_COUNT; i++){
            userThreads.add(new UserThread(i, true, this));
        }
        
        //Start User Threads
        for(UserThread ut : userThreads)
            ut.start();
        
        //Start Event Thread
        egt.start();
        
        while(runningThreads.intValue() > 0){
            Thread.sleep(100);
        }

        //Ensure there all the entries for the users
        assertEquals(USER_COUNT, this.cache.getCache().size());
        
        egt.allEvents.sort((e1,e2) -> (int)(e2.getActiveTimestamp() - e1.getActiveTimestamp()));
        this.cache.getCache().forEach((k,v) -> {
            //TODO be more accurate
            assertTrue(v.size() < EVENT_COUNT);
            //Ensure accuracy
            List<EventInstance> events = v.getEvents();
            for(int i=0; i<events.size(); i++) {
                assertEquals(egt.allEvents.get(i).getActiveTimestamp(), events.get(i).getActiveTimestamp()); 
            }
        });
        
        //trigger Cleaner and then ensure all entries are removed
        this.timer.fastForwardTo(this.timer.currentTimeMillis() + timeToLive + cleanerPeriod);
        
        //should be totally empty
        assertEquals(0, this.cache.getCache().size());
        
    }
    
    @Test
    public void testInsertAccuracy() throws InterruptedException {
        
        int timeToLive = 1000;
        int cleanerPeriod = EVENT_COUNT * USER_COUNT + 100;
        this.cache = new UserEventCache(timeToLive,  cleanerPeriod);

        //Setup EventThread
        EventGeneratorThread egt = new EventGeneratorThread(this, EVENT_COUNT);
        
        //Setup User Threads
        List<UserThread> userThreads = new ArrayList<UserThread>();
        for(int i=0; i<USER_COUNT; i++){
            userThreads.add(new UserThread(i, false, this));
        }
        
        //Start User Threads
        for(UserThread ut : userThreads)
            ut.start();
        
        //Start Event Thread
        egt.start();
        
        while(runningThreads.intValue() > 0){
            Thread.sleep(100);
        }

        //Ensure there all the entries for the users
        assertEquals(USER_COUNT, this.cache.getCache().size());
        
        egt.allEvents.sort((e1,e2) -> (int)(e2.getActiveTimestamp() - e1.getActiveTimestamp()));
        this.cache.getCache().forEach((k,v) -> {
            assertTrue(v.size() == EVENT_COUNT);
            //Ensure accuracy
            List<EventInstance> events = v.getEvents();
            for(int i=0; i<events.size(); i++) {
                assertEquals(egt.allEvents.get(i).getActiveTimestamp(), events.get(i).getActiveTimestamp()); 
            }
        });
        
        //trigger Cleaner and then ensure all entries are removed
        this.timer.fastForwardTo(this.timer.currentTimeMillis() + timeToLive + cleanerPeriod);
        
        //should be totally empty
        assertEquals(0, this.cache.getCache().size());
        
    }
    
	class EventGeneratorThread extends Thread{
		
		private UserEventCacheTest parent;
		private int eventCount;
		List<EventInstance> allEvents;
		
		public EventGeneratorThread(UserEventCacheTest parent, int eventCount){
			super("Event Generator");
			this.parent = parent;
			this.eventCount = eventCount;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			parent.runningThreads.incrementAndGet();
			
			EventType eventType = new DataPointEventType(1, 1);
            TranslatableMessage message = new TranslatableMessage("common.default", "not a real alarm");
            Map<String, Object> context = new HashMap<String,Object>();
			
            allEvents = new ArrayList<EventInstance>();
			//Raise Events
			for(int i=0; i<eventCount; i++){
				EventInstance e = new EventInstance(
						eventType,
						Common.timer.currentTimeMillis(), 
						true,
						AlarmLevels.CRITICAL,
						message,
						context);
				//Important
				e.setId(i);
				allEvents.add(e);
				for(int userId=0; userId<USER_COUNT; userId++)
					parent.cache.addEvent(userId, e);
				timer.fastForwardTo(timer.currentTimeMillis() + 1);
			}
			
			//Return them to normal
			for(EventInstance e : allEvents){
				e.setAcknowledgedByUserId(1);
				e.setAcknowledgedTimestamp(Common.timer.currentTimeMillis());
				for(int userId=0; userId<USER_COUNT; userId++)
					parent.cache.updateEvent(userId, e);
				timer.fastForwardTo(timer.currentTimeMillis() + 1);
			}
			
			//Randomly remove some, simulate acknowledge
			for(EventInstance e : allEvents){
				if(Math.random() > 0.5d)
					for(int userId=0; userId<USER_COUNT; userId++)
						parent.cache.updateEvent(userId, e);
			}
			
			synchronized(parent.monitor){
				parent.runningThreads.decrementAndGet();
				parent.monitor.notify();
				parent.generatorRunning.set(false);
			}
		}
	}
	
	class UserThread extends Thread{
		
		private UserEventCacheTest parent;
		private Integer userId;
		private boolean purge;
		
		public UserThread(Integer userId, boolean purge, UserEventCacheTest parent){
			super("User Thread " + userId);
			this.parent = parent;
			this.userId = userId;
			this.purge = purge;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			parent.runningThreads.incrementAndGet();
			
			while(parent.generatorRunning.get() == true){
				
				//Get our events events
				parent.cache.getAllEvents(userId);
				//Purge some 
				if(purge) {
				    List<EventInstance> events = parent.cache.getAllEvents(userId);
				    if(events.size() > 0) {
				        parent.cache.purgeEventsBefore(events.get(0).getActiveTimestamp() - 100);
				    }
				}
			}
			
			synchronized(parent.monitor){
				parent.runningThreads.decrementAndGet();
				parent.monitor.notify();
			}
			
		}
	}

}
