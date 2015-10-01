/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.rt.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.serotonin.m2m2.db.dao.EventDao;
import com.serotonin.m2m2.util.timeout.TimeoutClient;
import com.serotonin.m2m2.util.timeout.TimeoutTask;
import com.serotonin.m2m2.web.taglib.Functions;
import com.serotonin.timer.FixedRateTrigger;
import com.serotonin.timer.TimerTask;

/**
 * @author Terry Packer
 *
 */
public class UserEventCache implements TimeoutClient{
	private final Log LOG = LogFactory.getLog(UserEventCache.class);
	
    private long timeToLive;
    private HashMap<Integer, UserEventCacheEntry> cacheMap;
    private final EventDao dao;
    private TimerTask timerTask;
    private volatile Thread jobThread; //So we don't run multiple cleanups at once
    
    protected class UserEventCacheEntry {
        public long lastAccessed = System.currentTimeMillis();
        public CopyOnWriteArrayList<EventInstance> events;
        
        protected UserEventCacheEntry(CopyOnWriteArrayList<EventInstance> events) {
            this.events = events;
        }
        
        public void addEvent(EventInstance event){
        	this.events.add(event);
        	this.lastAccessed = System.currentTimeMillis();
        }

		/**
		 * @param evt
		 */
		public void replace(EventInstance evt) {
        	for(int i=0; i<events.size(); i++){
        		EventInstance ue  = events.get(i);
        		if(ue.getId() == evt.getId()){
        			events.add(i, ue);
        			events.remove(i+1);
        			break;
        		}
        	}
        	this.lastAccessed = System.currentTimeMillis();
		}

		/**
		 * @param evt
		 */
		public void remove(EventInstance evt) {
        	for(int i=0; i<events.size(); i++){
        		EventInstance ue  = events.get(i);
        		if(ue.getId() == evt.getId()){
        			events.remove(i);
        			break;
        		}
        	}
        	this.lastAccessed = System.currentTimeMillis();
        }

		public void purgeBefore(long time){
			List<EventInstance> toRemove = new ArrayList<EventInstance>();
			for(EventInstance e : events){
				if(e.getActiveTimestamp() < time){
					toRemove.add(e);
				}
			}
			events.removeAll(toRemove);
		}
		public void purgeBefore(long time, int alarmLevel){
			List<EventInstance> toRemove = new ArrayList<EventInstance>();
			for(EventInstance e : events){
				if((e.getActiveTimestamp() < time)&&(e.getAlarmLevel() == alarmLevel)){
					toRemove.add(e);
				}
			}
			events.removeAll(toRemove);
		}
		public void purgeBefore(long time, String typeName){
			List<EventInstance> toRemove = new ArrayList<EventInstance>();
			for(EventInstance e : events){
				if((e.getActiveTimestamp() < time)&&(e.getEventType().getEventType().equals(typeName))){
					toRemove.add(e);
				}
			}
			events.removeAll(toRemove);
		}
		/**
		 * Dump our entire cache
		 */
		public void purge() {
			this.events.clear();
			this.lastAccessed = System.currentTimeMillis();
		}
    }
    
    /**
     * 
     * @param timeToLive
     * @param timeInterval
     * @param max
     */
    public UserEventCache(long timeToLive, final long timeInterval) {
        this.timeToLive = timeToLive;
        
        cacheMap = new HashMap<Integer, UserEventCacheEntry>();
        this.dao = new EventDao();
        
        timerTask = new TimeoutTask(new FixedRateTrigger(500, timeInterval), this);
    }
    
    
    public void addEvent(Integer userId, EventInstance value) {
        synchronized (cacheMap) {
            UserEventCacheEntry entry = cacheMap.get(userId);
            if(entry != null){
            	entry.addEvent(value);
            }
        }
    }
    
	/**
	 * @param id
	 * @param evt
	 */
	public void updateEvent(int userId, EventInstance evt) {
        synchronized (cacheMap) {
            UserEventCacheEntry entry = cacheMap.get(userId);
            if(entry != null){
            	entry.replace(evt);
            }
        }
	}

    public void removeEvent(Integer userId, EventInstance evt) {
        synchronized (cacheMap) {
            synchronized (cacheMap) {
                UserEventCacheEntry entry = cacheMap.get(userId);
                if(entry != null){
                	entry.remove(evt);
                }
            }
        }
    }

    /**
     * Get all events if no events exist in cache then an entry is created.
     * @param userId
     * @return
     */
    public List<EventInstance> getAllEvents(Integer userId) {
        synchronized (cacheMap) {
            UserEventCacheEntry c = cacheMap.get(userId);
            
            if (c == null){
                List<EventInstance> userEvents = dao.getAllUnsilencedEvents(userId);
                CopyOnWriteArrayList<EventInstance> list = new CopyOnWriteArrayList<EventInstance>(userEvents);
            	cacheMap.put(userId, new UserEventCacheEntry(list));
            	return userEvents;
            }else {
                c.lastAccessed = System.currentTimeMillis();
                return c.events;
            }
        }
    }
    
    /**
     * 
     * @param time
     */
    public void purgeEventsBefore(long time){
		synchronized (cacheMap) {
			Iterator<Integer> it = cacheMap.keySet().iterator();
			while(it.hasNext()){
				cacheMap.get(it.next()).purgeBefore(time);
			}
		}
    }
    public void purgeEventsBefore(long time, int alarmLevel){
		synchronized (cacheMap) {
			Iterator<Integer> it = cacheMap.keySet().iterator();
			while(it.hasNext()){
				cacheMap.get(it.next()).purgeBefore(time, alarmLevel);
			}
		}
    }
    public void purgeEventsBefore(long time, String typeName){
		synchronized (cacheMap) {
			Iterator<Integer> it = cacheMap.keySet().iterator();
			while(it.hasNext()){
				cacheMap.get(it.next()).purgeBefore(time, typeName);
			}
		}
    }
	/**
	 * Clear Events for all users
	 */
	public void purgeAllEvents() {
		synchronized (cacheMap) {
			Iterator<Integer> it = cacheMap.keySet().iterator();
			while(it.hasNext()){
				cacheMap.get(it.next()).purge();
			}
		}
	}
    
    // CLEANUP method
    public void cleanup() {
        
        long now = System.currentTimeMillis();
        ArrayList<Integer> deleteKey = null;
        
        synchronized (cacheMap) {
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
        }
        
        for (Integer key : deleteKey) {
            synchronized (cacheMap) {
                cacheMap.remove(key);
            }
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
                    + " aborted because a previous Clenaup is still running");
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
        Thread.yield();
	}
	
	public void terminate(){
        if (timerTask != null)
            timerTask.cancel();  
	}
}
