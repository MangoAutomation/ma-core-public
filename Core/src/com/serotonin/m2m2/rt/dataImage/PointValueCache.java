/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.timer.AbstractTimer;

/**
 * This class maintains an ordered list of the most recent values for a data point. It will mirror values in the
 *  database, but provide a much faster lookup for a limited number of values.
 *  
 * The list is in time descending order with the latest value for a point at position 0
 * 
 * This is also used as a store for values that are queued to be saved but may not be saved yet i.e. in a batch 
 *  write queue allowing a place to access all saved values reliably.
 * 
 * @author Matthew Lohbihler, Terry Packer
 */
public class PointValueCache {
    private final Object lock = new Object();
    //Simulation Timer, or any timer implementation
    private final AbstractTimer timer;
    private final int dataPointId;
    private final int defaultSize;
    private int maxSize = 0;
    
    //This would not be the advised thing to do if we were to be deleting any data through here
    // as some properties of the delete are system settings that can be changed
    // However, since we're only querying, it should be more efficient to have only one static final reference
    protected static final PointValueDao dao = Common.databaseProxy.newPointValueDao();

    /**
     * IMPORTANT: The list object should never be written to! The implementation here is for performance. Never call
     * methods like add() or remove() on the cache object. Further, since the cache object can be replaced from time to
     * time, always use a local copy of the variable for read purposes.
     */
    private List<PointValueTime> cache;
    private final AtomicLong latestSavedValueTime;
    protected final Consumer<Long> valueSavedCallback;

    /**
     * Create a cache for a data point.
     * 
     * @param dataPointId
     * @param defaultSize - the base size of the cache, it will never be smaller than this
     * @param cache - initial cache, if null then the cache is populated to default size from the database contents
     */
    public PointValueCache(int dataPointId, int defaultSize, List<PointValueTime> cache, AbstractTimer timer) {
        this.timer = timer;
        this.dataPointId = dataPointId;
        this.defaultSize = defaultSize;
        this.latestSavedValueTime = new AtomicLong();
        this.valueSavedCallback = (time) -> {
            //Track latest value's time that is saved to db
            latestSavedValueTime.accumulateAndGet(time, (updated, current) ->{
                return updated > current ? updated : current;
            });
        };
        
        if (cache == null) {
            this.cache = new ArrayList<>();
            if (defaultSize > 0) {
                refreshCache(defaultSize);
            }
        } else {
            if (cache.size() > defaultSize) {
                // dont keep excess point values hanging around
                this.cache = new ArrayList<>(cache.subList(0, defaultSize));
            } else {
                this.cache = cache;
            }
            
            //Set our known state of saved values
            if (cache.size() > 0) {
                this.latestSavedValueTime.set(cache.get(0).getTime());
                this.maxSize = cache.size();
            }
        }
    }

    /**
     * Save a point value, placing it in the cache.
     * 
     * @param pvt - value to save
     * @param source - source to use for annotation
     * @param logValue - save to database or just cache it
     * @param async - queue this value into a batch for performance writes
     */
    public void savePointValue(PointValueTime pvt, SetPointSource source, boolean logValue, boolean async) {
        if (logValue) {
            if (async) {
                savePointValueAsync(pvt, source);
            }else
                pvt = savePointValueSync(pvt, source);
        }

        synchronized (lock) {
        
                List<PointValueTime> c = cache;
                List<PointValueTime> nc = new ArrayList<PointValueTime>(c.size() + 1);
                nc.addAll(c);
        
                // Insert the value in the cache.
                if (nc.size() == 0)
                    nc.add(pvt);
                else {
                    int pos = 0;
                    //Find where in the cache we want to place the new value
                    while (pos < nc.size() && nc.get(pos).getTime() > pvt.getTime())
                        pos++;
                    //TODO logValue with respect to Interval Logging
                    nc.add(pos, pvt);
                }
        
                safelyTrim(nc);
                cache = nc;
        }
    }

    void savePointValueAsync(PointValueTime pvt, SetPointSource source) {
        dao.savePointValueAsync(dataPointId, pvt, source, valueSavedCallback);
    }
    
    PointValueTime savePointValueSync(PointValueTime pvt, SetPointSource source) {
        return dao.savePointValueSync(dataPointId, pvt, source);
    }
    
    /**
     * Get the latest point value or null.  This will expand the cache to 1 if it is empty
     * @return
     */
    public PointValueTime getLatestPointValue() {
        if (maxSize == 0)
            refreshCache(1);

        List<PointValueTime> c = cache;
        if (c.size() > 0)
            return c.get(0);

        return null;
    }

    /**
     * Get the latest limit number of point values.  This call can expand the 
     * cache if limit is larger than the current cache size
     * @param limit
     * @return
     */
    public List<PointValueTime> getLatestPointValues(int limit) {
        if (maxSize < limit)
            refreshCache(limit);

        List<PointValueTime> c = cache;
        if (limit == c.size())
            return new ArrayList<PointValueTime>(c);

        if (limit > c.size())
            limit = c.size();
        return new ArrayList<PointValueTime>(c.subList(0, limit));
    }

    /**
     * Refresh the cache, keeping existing cached values if they are not already logged.
     * @param size
     */
    private void refreshCache(int size) {
        if (size > maxSize) {
            if (size == 1) {
                // Performance thingy
                PointValueTime pvt = dao.getLatestPointValue(dataPointId);
                if (pvt != null) {
                    synchronized(lock) {
                        List<PointValueTime> c = new ArrayList<PointValueTime>(cache);
                        int pos = 0;
                        //Find where in the cache we want to place the new value
                        while (pos < c.size() && c.get(pos).getTime() > pvt.getTime())
                            pos++;
                        if (pos < maxSize)
                            c.add(pos, pvt);
                        //Reset our latest time, 
                        latestSavedValueTime.accumulateAndGet(pvt.getTime(), (updated, current) ->{
                            return updated > current ? updated : current;
                        });
                        safelyTrim(c);
                        maxSize = c.size();
                        cache = c;
                    }
                }else {
                    synchronized(lock) {
                        List<PointValueTime> c = new ArrayList<PointValueTime>(cache);
                        safelyTrim(c);
                        latestSavedValueTime.set(Long.MIN_VALUE);
                        maxSize = c.size();
                        cache = c;
                    }
                }
            }
            else {
                List<PointValueTime> nc;
                synchronized(lock) {
                    List<PointValueTime> cc = new ArrayList<>(cache.size());
                    cc.addAll(cache);
                    nc = new ArrayList<PointValueTime>(size);
                    List<PointValueTime> latest = dao.getLatestPointValues(dataPointId, size, timer.currentTimeMillis() + 1);
                    //Reset our latest time, 
                    if(latest.size() > 0)
                        latestSavedValueTime.accumulateAndGet(latest.get(0).getTime(), (updated, current) ->{
                            return updated > current ? updated : current;
                        });

                    for(PointValueTime value : latest) {
                        //Cache is in same order as rows
                        if(nc.size() < size && cc.size() > 0 && cc.get(0).getTime() >= value.getTime()) {
                            //The cached value is newer or unsaved retain it
                            while((nc.size() < size && cc.size() > 0 && cc.get(0).getTime() > value.getTime()) || (cc.size() > 0 && cc.get(0).getTime() > latestSavedValueTime.get()))
                                nc.add(cc.remove(0));
                            
                            //should we replace this value?
                            if(cc.size() > 0 && cc.get(0).getTime() == value.getTime())
                                cc.remove(0);
                            
                            //add the value from the database if it doesn't make us too big
                            if(nc.size() < size)
                                nc.add(value);
                            
                        }else {
                            //Past cached value times
                            if(nc.size() < size)
                                nc.add(value);
                        }
                    }
                    //No values in database, make sure we keep the unsaved values
                    if(nc.size() == 0) {
                        while((nc.size() < size && cc.size() > 0) || (cc.size() > 0 && cc.get(0).getTime() > latestSavedValueTime.get()))
                            nc.add(cc.remove(0));
                        latestSavedValueTime.set(Long.MIN_VALUE);
                    }
                    maxSize = nc.size();
                    cache = nc;
                }
            }
        }
    }

    /**
     * Never manipulate the contents of this list!
     */
    public List<PointValueTime> getCacheContents() {
        return cache;
    }

    /**
     * Reset the cache.  This is used during a point purge to reset our state to 
     *  the database which was just fully purged.  Point values may be streaming in during 
     *  this time so we cannot assume the database is empty.
     */
    public void reset() {
        List<PointValueTime> nc = dao.getLatestPointValues(dataPointId, defaultSize);
        synchronized(lock) {
            maxSize = defaultSize;
            long latestTime = nc.size() > 0 ? nc.get(0).getTime() : Long.MIN_VALUE;
            
            //fill in backwards
            for(int i=cache.size()-1; i>=0; i--)
                if(cache.get(i).getTime() > latestTime)
                    nc.add(0, cache.get(i));

            //Reset our latest time, 
            latestSavedValueTime.accumulateAndGet(nc.size() > 0 ? nc.get(0).getTime() : Long.MIN_VALUE, (updated, current) ->{
                return updated > current ? updated : current;
            });
            safelyTrim(nc);
            cache = nc;
        }
    }
    
    /**
     * Reset the cache by dropping values before 'before', this is used during a point purge.  
     * @param before
     */
    public void reset(long before) {
        synchronized(lock) {
            List<PointValueTime> nc = new ArrayList<PointValueTime>(cache.size());
            nc.addAll(cache);
            Iterator<PointValueTime> iter = nc.iterator();
            while(iter.hasNext()) {
                long time = iter.next().getTime();
                //Remove old values that have been saved
                if(time < before && time <= latestSavedValueTime.get())
                    iter.remove();
            }
    
            if(nc.size() < defaultSize) {
                //So we expand the cache if necessary
                maxSize = nc.size();
                cache = nc;
                refreshCache(defaultSize);
            } else {
                safelyTrim(nc);
                maxSize = nc.size();
                cache = nc;
            }
        }
    }
    
    /**
     * Trim a cache and make sure to not lose values that have not been saved.
     * @param toTrim
     */
    private void safelyTrim(List<PointValueTime> toTrim) {
        while (toTrim.size() > maxSize) {
            //If the value has not been saved we will not trim it 
            //  and since we are in time order the remaining values will not have been saved either
            if(toTrim.get(toTrim.size() - 1).getTime() > latestSavedValueTime.get())
                break;
            toTrim.remove(toTrim.size() - 1);
        }
    }
}
