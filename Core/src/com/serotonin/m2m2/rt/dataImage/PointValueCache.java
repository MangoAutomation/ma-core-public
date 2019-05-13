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

/**
 * This class maintains an ordered list of the most recent values for a data point. It will mirror values in the
 *  database, but provide a much faster lookup for a limited number of values.
 * 
 * This is also used as a store for values that are queued to be saved but may not be saved yet i.e. in a batch 
 *  write queue allowing a place to access all saved values reliably.
 * 
 * @author Matthew Lohbihler, Terry Packer
 */
public class PointValueCache {
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
    public PointValueCache(int dataPointId, int defaultSize, List<PointValueTime> cache) {
        this.dataPointId = dataPointId;
        this.defaultSize = defaultSize;
        this.latestSavedValueTime = new AtomicLong();
        this.valueSavedCallback = (time) -> {
            //Prune cache
            //TODO Synchronization
            latestSavedValueTime.set(time);
            List<PointValueTime> c = this.cache;
            if(c.size() > maxSize) {
                List<PointValueTime> newCache = new ArrayList<PointValueTime>(c.size() + 1);
                newCache.addAll(c);
                
                while (newCache.size() > maxSize) {
                    //If the value has not been saved we are not discarding any data
                    if(newCache.get(newCache.size() - 1).getTime() > latestSavedValueTime.get())
                        break;
                    newCache.remove(newCache.size() - 1);
                }
                this.cache = newCache;
            }
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
            if (cache.size() > 0)
                this.latestSavedValueTime.set(cache.get(0).getTime());
            
            this.maxSize = defaultSize;
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

        //TODO Synchronization
        List<PointValueTime> c = cache;
        List<PointValueTime> newCache = new ArrayList<PointValueTime>(c.size() + 1);
        newCache.addAll(c);

        // Insert the value in the cache.
        int pos = 0;
        if (newCache.size() == 0)
            newCache.add(pvt);
        else {
            //Find where in the cache we want to place the new value
            while (pos < newCache.size() && newCache.get(pos).getTime() > pvt.getTime())
                pos++;
            newCache.add(pos, pvt);
        }

        // Check if we need to clean up the list
        while (newCache.size() > maxSize) {
            //If the value has not been saved we are not discarding any data
            if(newCache.get(newCache.size() - 1).getTime() > latestSavedValueTime.get())
                break;
            newCache.remove(newCache.size() - 1);
        }

        cache = newCache;
    }

    void savePointValueAsync(PointValueTime pvt, SetPointSource source) {
        dao.savePointValueAsync(dataPointId, pvt, source, valueSavedCallback);
    }
    
    PointValueTime savePointValueSync(PointValueTime pvt, SetPointSource source) {
        return dao.savePointValueSync(dataPointId, pvt, source);
    }
    
    public PointValueTime getLatestPointValue() {
        if (maxSize == 0)
            refreshCache(1);

        List<PointValueTime> c = cache;
        if (c.size() > 0)
            return c.get(0);

        return null;
    }

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
            maxSize = size;
            if (size == 1) {
                // Performance thingy
                PointValueTime pvt = dao.getLatestPointValue(dataPointId);
                if (pvt != null) {
                    List<PointValueTime> c = new ArrayList<PointValueTime>();
                    c.add(pvt);
                    cache = c;
                    latestSavedValueTime.set(pvt.getTime());
                }
            }
            else {
                List<PointValueTime> c = cache;
                List<PointValueTime> cc = new ArrayList<>(c.size());
                cc.addAll(c);
                List<PointValueTime> nc = new ArrayList<PointValueTime>(size);
                dao.getLatestPointValues(dataPointId, Common.timer.currentTimeMillis() + 1, size, (value, index) -> {
                    //Cache is in same order as rows
                    if(nc.size() < size && cc.size() > 0 && cc.get(0).getTime() >= value.getTime()) {
                        //The cached value is newer so add it
                        while(nc.size() < size && cc.size() > 0 && cc.get(0).getTime() > value.getTime())
                            nc.add(cc.remove(0));
                        if(cc.size() > 0 && cc.get(0).getTime() == value.getTime())
                            cc.remove(0);
                        if(nc.size() < size)
                            nc.add(value);
                    }else {
                        //Past cached value times
                        if(nc.size() < size)
                            nc.add(value);
                    }
                });
                cache = nc;
                if(nc.size() > 0)
                    latestSavedValueTime.set(nc.get(0).getTime());
            }
        }
    }

    /**
     * Never manipulate the contents of this list!
     */
    public List<PointValueTime> getCacheContents() {
        return cache;
    }

    public void reset() {
        List<PointValueTime> nc = dao.getLatestPointValues(dataPointId, defaultSize);
        maxSize = defaultSize;
        cache = nc;
    }
    
    public void reset(long before) {
        List<PointValueTime> nc = new ArrayList<PointValueTime>(cache.size());
        nc.addAll(cache);
        Iterator<PointValueTime> iter = nc.iterator();
        while(iter.hasNext())
            if(iter.next().getTime() < before)
                iter.remove();

        if(nc.size() < defaultSize) {
            maxSize = 0;
            cache = nc;
            refreshCache(defaultSize);
        } else
            cache = nc;
    }
}
