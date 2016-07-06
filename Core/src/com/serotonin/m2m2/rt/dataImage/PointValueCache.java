/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import com.serotonin.m2m2.db.dao.DaoRegistry;
import com.serotonin.m2m2.db.dao.PointValueDao;

/**
 * This class maintains an ordered list of the most recent values for a data point. It will mirror values in the
 * database, but provide a much faster lookup for a limited number of values.
 * 
 * Because there is not a significant performance problem for time-based lookups, they are not handled here, but rather
 * are still handled by the database.
 * 
 * @author Matthew Lohbihler
 */
public class PointValueCache {
    private final int dataPointId;
    private final int defaultSize;
    private final PointValueDao dao;
    private int maxSize = 0;

    /**
     * IMPORTANT: The list object should never be written to! The implementation here is for performance. Never call
     * methods like add() or remove() on the cache object. Further, since the cache object can be replaced from time to
     * time, always use a local copy of the variable for read purposes.
     */
    private List<PointValueTime> cache;

    public PointValueCache(int dataPointId, int defaultSize, List<PointValueTime> cache) {
        this.dataPointId = dataPointId;
        this.defaultSize = defaultSize;
        
        dao = DaoRegistry.pointValueDao;
        
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
            this.maxSize = defaultSize;
        }
    }
    
    void savePointValueAsync(PointValueTime pvt, SetPointSource source) {
        dao.savePointValueAsync(dataPointId, pvt, source);
    }
    
    PointValueTime savePointValueSync(PointValueTime pvt, SetPointSource source) {
        return dao.savePointValueSync(dataPointId, pvt, source);
    }
    
    void updatePointValueAsync(PointValueTime pvt, SetPointSource source) {
        dao.updatePointValueAsync(dataPointId, pvt, source);
    }
    
    PointValueTime updatePointValueSync(PointValueTime pvt, SetPointSource source) {
        return dao.updatePointValueSync(dataPointId, pvt, source);
    }

    /**
     * Update a value in the system
     * @param pvt
     * @param source
     * @param logValue - Store in DB and Cache or Just Cache
     * @param async
     * @return true if point value existed and was updated, false if value DNE in cache
     * 
     */
    public void updatePointValue(PointValueTime pvt, SetPointSource source, boolean logValue, boolean async){
    	
        if (logValue) {
            if (async)
                updatePointValueAsync(pvt, source);
            else
                pvt = updatePointValueSync(pvt, source);
        }
    	
        
        //Update our point in the cache if it exists
        List<PointValueTime> c = cache;
        List<PointValueTime> newCache = new ArrayList<PointValueTime>(c.size() + 1);
        newCache.addAll(c);

        // Insert the value in the cache.
        if (newCache.size() == 0)
            return; //Empty anyway
        else {
            ListIterator<PointValueTime> it = newCache.listIterator();
            while (it.hasNext()) {
                PointValueTime cachedValue = it.next();
                if(cachedValue.getTime() == pvt.getTime()){
                    it.set(pvt); //Replace it and break out
                    break;
                }
            }
        }

        cache = newCache;
    	return;
    }
    
    public void savePointValue(PointValueTime pvt, SetPointSource source, boolean logValue, boolean async) {
        if (logValue) {
            if (async)
                savePointValueAsync(pvt, source);
            else
                pvt = savePointValueSync(pvt, source);
        }

        List<PointValueTime> c = cache;
        List<PointValueTime> newCache = new ArrayList<PointValueTime>(c.size() + 1);
        newCache.addAll(c);

        // Insert the value in the cache.
        int pos = 0;
        if (newCache.size() == 0)
            newCache.add(pvt);
        else {
            while (pos < newCache.size() && newCache.get(pos).getTime() > pvt.getTime())
                pos++;
            if (pos < maxSize)
                newCache.add(pos, pvt);
        }

        // Check if we need to clean up the list
        while (newCache.size() > maxSize)
            newCache.remove(newCache.size() - 1);
        // if (newCache.size() > maxSize - 1)
        // newCache = new ArrayList<PointValueTime>(newCache.subList(0, maxSize));

        cache = newCache;
    }

    /**
     * Saves the given value to the database without adding it to the cache.
     */
    void logPointValueAsync(PointValueTime pointValue, SetPointSource source) {
        // Save the new value and get a point value time back that has the id and annotations set, as appropriate.
        savePointValueAsync(pointValue, source);
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
                }
            }
            else
                cache = dao.getLatestPointValues(dataPointId, size);
        }
    }

    /**
     * Never manipulate the contents of this list!
     */
    public List<PointValueTime> getCacheContents() {
        return cache;
    }

    public void reset() {
        List<PointValueTime> c = cache;

        int size = defaultSize;
        if (c.size() < size)
            size = c.size();

        List<PointValueTime> nc = new ArrayList<PointValueTime>(size);
        nc.addAll(c.subList(0, size));

        maxSize = size;
        cache = nc;
    }
}
