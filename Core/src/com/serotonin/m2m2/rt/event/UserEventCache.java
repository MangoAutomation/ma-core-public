/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        this.cacheMap = new ConcurrentHashMap<Integer, UserEventCacheEntry>();
        this.dao = EventDao.instance;
        this.timerTask = new TimeoutTask(new FixedRateTrigger(timeInterval, timeInterval), this);
        this.running = true;
    }
    
    /**
     * Add event for a user
     * @param userId
     * @param value
     */
    public void addEvent(Integer userId, EventInstance value) {
        UserEventCacheEntry entry = cacheMap.get(userId);
        if (entry != null) {
            entry.addEvent(value);
        }
    }
    
	/**
	 * @param id
	 * @param evt
	 */
	public void updateEvent(int userId, EventInstance evt) {
        UserEventCacheEntry entry = cacheMap.get(userId);
        if (entry != null) {
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
        if (entry != null) {
            entry.remove(evt);
        }
    }

    /**
     * Get all events if no events exist in cache then an entry is created.
     * @param userId
     * @return
     */
    public List<EventInstance> getAllEvents(Integer userId) {
        UserEventCacheEntry c = cacheMap.computeIfAbsent(userId, (k) -> {
            List<EventInstance> userEvents = dao.getAllUnsilencedEvents(userId);
            return new UserEventCacheEntry(userEvents);
        });
        return c.getEvents();
    }
    
    /**
     * 
     * @param time
     */
    public void purgeEventsBefore(long time){
        cacheMap.forEach((k,v) ->{
            v.purgeBefore(time);
        });
    }
    
    public void purgeEventsBefore(long time, int alarmLevel){
        cacheMap.forEach((k,v) ->{
            v.purgeBefore(time, alarmLevel);
        });
    }
    
    public void purgeEventsBefore(long time, String typeName){
        cacheMap.forEach((k,v) ->{
            v.purgeBefore(time, typeName);
        });
    }
    
	/**
	 * Clear Events for all users
	 */
	public void purgeAllEvents() {
        cacheMap.forEach((k,v) ->{
            v.purge();
        });
	}
    
    // CLEANUP method
    public void cleanup() {
        if (!running)
            return;
        long now = Common.timer.currentTimeMillis();
        cacheMap.values().removeIf(v -> {
            return now > (timeToLive + v.lastAccessed);
        });
        
    }
    
    protected Map<Integer, UserEventCacheEntry> getCache() {
        return cacheMap;
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

	
    public class UserEventCacheEntry {
    	
        private volatile long lastAccessed = Common.timer.currentTimeMillis();
        private Map<Integer, EventInstance> events;
        
        protected UserEventCacheEntry(List<EventInstance> events) {
        	//Make a local copy, we don't know what will go on with the list outside our little world.
            this.events = new ConcurrentHashMap<>(events.size());
            for(EventInstance event : events)
                this.events.put(event.getId(), event);
        }
        
        /**
		 * @return
		 */
		public List<EventInstance> getEvents() {
            try {
                // Make a copy order by Ts desc as like the query that for unsilenced events
                List<EventInstance> list = new ArrayList<>(this.events.values());
                list.sort((e1, e2) -> (int)(e2.getActiveTimestamp() - e1.getActiveTimestamp()));
                return list;
            } finally {
                this.lastAccessed = Common.timer.currentTimeMillis();
            }
		}

		public void addEvent(EventInstance event){
            try {
                this.events.put(event.getId(), event);
            } finally {
                this.lastAccessed = Common.timer.currentTimeMillis();
            }
        }

		/**
		 * @param evt
		 */
		public void replace(EventInstance evt) {
			try{
				this.events.put(evt.getId(), evt);
			}finally{
				this.lastAccessed = Common.timer.currentTimeMillis();
			}
        	
		}

		/**
		 * @param evt
		 */
		public void remove(EventInstance evt) {
			try{
			    this.events.remove(evt.getId());
			}finally{
				this.lastAccessed = Common.timer.currentTimeMillis();
			}
        }

		public void purgeBefore(long time){
			try{
			    //Not using removeIf https://bugs.openjdk.java.net/browse/JDK-8078645
			    this.events.forEach((k,ue)->{
			        if(ue.getActiveTimestamp() < time){
                        this.events.remove(k);
                    }
			    });
			}finally{
				this.lastAccessed = Common.timer.currentTimeMillis();
			}
			
		}
		public void purgeBefore(long time, int alarmLevel){
            try {
                //Not using removeIf https://bugs.openjdk.java.net/browse/JDK-8078645
                this.events.forEach((k,ue)->{
                    if ((ue.getActiveTimestamp() < time) && (ue.getAlarmLevel() == alarmLevel)) {
                        this.events.remove(k);
                    }
                });
            } finally {
                this.lastAccessed = Common.timer.currentTimeMillis();
            }
		}
		public void purgeBefore(long time, String typeName){
            try {
                //Not using removeIf https://bugs.openjdk.java.net/browse/JDK-8078645
                this.events.forEach((k,ue)-> {
                    if ((ue.getActiveTimestamp() < time)
                            && (ue.getEventType().getEventType().equals(typeName))) {
                        this.events.remove(k);
                    }
                });
            } finally {
                this.lastAccessed = Common.timer.currentTimeMillis();
            }
		}
		/**
		 * Dump our entire cache
		 */
		public void purge() {
            try {
                this.events.clear();
            } finally {
                this.lastAccessed = Common.timer.currentTimeMillis();
            }
        }
		
		public int size() {
		    try {
		        return this.events.size();
		    } finally {
		        this.lastAccessed = Common.timer.currentTimeMillis();
		    }
		}
    }
}
