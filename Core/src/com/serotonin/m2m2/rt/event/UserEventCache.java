/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.event;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
 * If a user has not accessed their cache in timeToLive ms then the entry is cleaned up.
 * Stale entries are discarded every timeInterval.
 * 
 * @author Terry Packer
 *
 */
public class UserEventCache extends TimeoutClient{
	private final Log LOG = LogFactory.getLog(UserEventCache.class);
	
    private long timeToLive;
    private Map<Integer, UserEventCacheEntry> cacheMap;
    private final EventDao dao;
    private TimerTask timerTask;
    private volatile Thread jobThread; //So we don't run multiple cleanups at once
    
    /**
     * 
     * @param timeToLive
     * @param timeInterval
     */
    public UserEventCache(long timeToLive, final long timeInterval) {
        this.timeToLive = timeToLive;
        this. cacheMap = new ConcurrentHashMap<Integer, UserEventCacheEntry>();
        this.dao = new EventDao();
        this.timerTask = new TimeoutTask(new FixedRateTrigger(500, timeInterval), this);
    }

    public UserEventCache(long timeToLive, final long timeInterval, EventDao dao) {
        this.timeToLive = timeToLive;
        this. cacheMap = new ConcurrentHashMap<Integer, UserEventCacheEntry>();
        this.dao = dao;
        this.timerTask = new TimeoutTask(new FixedRateTrigger(500, timeInterval), this);
    }
    
    
    /**
     * Add event for a user
     * @param userId
     * @param value
     */
    public void addEvent(Integer userId, EventInstance value) {
        UserEventCacheEntry entry = cacheMap.get(userId);
        if(entry != null){
        	entry.addEvent(value);
        }
    }
    
	/**
	 * @param id
	 * @param evt
	 */
	public void updateEvent(int userId, EventInstance evt) {
        UserEventCacheEntry entry = cacheMap.get(userId);
        if(entry != null){
        	entry.replace(evt);
        }
	}

	/**
	 * Remove Event for User
	 * @param userId
	 * @param evt
	 */
    public void removeEvent(Integer userId, EventInstance evt) {
        UserEventCacheEntry entry = cacheMap.get(userId);
        if(entry != null){
        	entry.remove(evt);
        }
    }

    /**
     * Get all events if no events exist in cache then an entry is created.
     * @param userId
     * @return
     */
    public List<EventInstance> getAllEvents(Integer userId) {
        UserEventCacheEntry c = cacheMap.get(userId);
        if (c == null){
            List<EventInstance> userEvents = dao.getAllUnsilencedEvents(userId);
        	cacheMap.put(userId, new UserEventCacheEntry(userEvents));
        	return userEvents;
        }else {
            return c.getEvents();
        }
    }
    
    /**
     * 
     * @param time
     */
    public void purgeEventsBefore(long time){
		Iterator<Integer> it = cacheMap.keySet().iterator();
		while(it.hasNext()){
			cacheMap.get(it.next()).purgeBefore(time);
		}
    }
    
    public void purgeEventsBefore(long time, int alarmLevel){
			Iterator<Integer> it = cacheMap.keySet().iterator();
			while(it.hasNext()){
				cacheMap.get(it.next()).purgeBefore(time, alarmLevel);
			}
    }
    
    public void purgeEventsBefore(long time, String typeName){
		Iterator<Integer> it = cacheMap.keySet().iterator();
		while(it.hasNext()){
			cacheMap.get(it.next()).purgeBefore(time, typeName);
		}
    }
    
	/**
	 * Clear Events for all users
	 */
	public void purgeAllEvents() {
		Iterator<Integer> it = cacheMap.keySet().iterator();
		while(it.hasNext()){
			cacheMap.get(it.next()).purge();
		}
	}
    
    // CLEANUP method
    public void cleanup() {
        
        long now = Common.backgroundProcessing.currentTimeMillis();
        ArrayList<Integer> deleteKey = null;
        

        Iterator<Entry<Integer, UserEventCacheEntry>> itr = cacheMap.entrySet().iterator();
        
        deleteKey = new ArrayList<Integer>((cacheMap.size() / 2) + 1);
        Entry<Integer, UserEventCacheEntry> entry = null;
        UserEventCacheEntry cacheEntry = null;
        while (itr.hasNext()) {
            entry = itr.next();
            cacheEntry = entry.getValue();
            if (cacheEntry != null && (now > (timeToLive + cacheEntry.lastAccessed))) {
                deleteKey.add(entry.getKey());
            }
        }
        
        for (Integer key : deleteKey) {
        	cacheMap.remove(key);
        }
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

	
    private class UserEventCacheEntry {
    	
        private volatile long lastAccessed = Common.backgroundProcessing.currentTimeMillis();
        private List<EventInstance> events;
        private ReadWriteLock lock;
        
        protected UserEventCacheEntry(List<EventInstance> events) {
        	//Make a local copy, we don't know what will go on with the list outside our little world.
            this.events = new ArrayList<>(events);
            this.lock = new ReentrantReadWriteLock();
        }
        
        /**
		 * @return
		 */
		public List<EventInstance> getEvents() {
			this.lock.readLock().lock();
        	try{
        		//Make a copy
        		return new ArrayList<>(this.events);
        	}finally{
        		this.lock.readLock().unlock();
        		this.lastAccessed = Common.backgroundProcessing.currentTimeMillis();
        	}
		}

		public void addEvent(EventInstance event){
        	this.lock.writeLock().lock();
        	try{
        		this.events.add(event);
        	}finally{
        		this.lock.writeLock().unlock();
        		this.lastAccessed = Common.backgroundProcessing.currentTimeMillis();
        	}
        }

		/**
		 * @param evt
		 */
		public void replace(EventInstance evt) {
			this.lock.writeLock().lock();
			try{
				ListIterator<EventInstance> it = this.events.listIterator();
	        	while(it.hasNext()){
	        		EventInstance ue  = it.next();
	        		if(ue.getId() == evt.getId()){
	        			it.set(evt);
	        			break;
	        		}
	        	}
			}finally{
				this.lock.writeLock().unlock();
				this.lastAccessed = Common.backgroundProcessing.currentTimeMillis();
			}
        	
		}

		/**
		 * @param evt
		 */
		public void remove(EventInstance evt) {
			this.lock.writeLock().lock();
			try{
				ListIterator<EventInstance> it = this.events.listIterator();
	        	while(it.hasNext()){
	        		EventInstance ue  = it.next();
	        		if(ue.getId() == evt.getId()){
	        			it.remove();
	        			break;
	        		}
	        	}
			}finally{
				this.lock.writeLock().unlock();
				this.lastAccessed = Common.backgroundProcessing.currentTimeMillis();
			}
        }

		public void purgeBefore(long time){
			this.lock.writeLock().lock();
			try{
				ListIterator<EventInstance> it = this.events.listIterator();
	        	while(it.hasNext()){
	        		EventInstance ue  = it.next();
	        		if(ue.getActiveTimestamp() < time){
	        			it.remove();
	        		}
	        	}
			}finally{
				this.lock.writeLock().unlock();
				this.lastAccessed = Common.backgroundProcessing.currentTimeMillis();
			}
			
		}
		public void purgeBefore(long time, int alarmLevel){
			this.lock.writeLock().lock();
			try{
				ListIterator<EventInstance> it = this.events.listIterator();
	        	while(it.hasNext()){
	        		EventInstance ue  = it.next();
	        		if((ue.getActiveTimestamp() < time)&&(ue.getAlarmLevel() == alarmLevel)){
	        			it.remove();
	        		}
	        	}
			}finally{
        		this.lock.writeLock().unlock();
        		this.lastAccessed = Common.backgroundProcessing.currentTimeMillis();
        	}
		}
		public void purgeBefore(long time, String typeName){
			
			this.lock.writeLock().lock();
			try{
				ListIterator<EventInstance> it = this.events.listIterator();
	        	while(it.hasNext()){
	        		EventInstance ue  = it.next();
	        		if((ue.getActiveTimestamp() < time)&&(ue.getEventType().getEventType().equals(typeName))){
	        			it.remove();
	        		}
	        	}
			}finally{
        		this.lock.writeLock().unlock();
        		this.lastAccessed = Common.backgroundProcessing.currentTimeMillis();
        	}
		}
		/**
		 * Dump our entire cache
		 */
		public void purge() {
			this.lock.writeLock().lock();
			try{
				this.events.clear();
			}finally{
        		this.lock.writeLock().unlock();
        		this.lastAccessed = Common.backgroundProcessing.currentTimeMillis();
        	}
		}
    }
}
