/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataImage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.infiniteautomation.mango.pointvaluecache.PointValueCache;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.view.stats.ITime;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * This class maintains an ordered list of the most recent values for a data point. It will mirror values in the
 * database, but provide a much faster lookup for a limited number of values.
 *
 * Because there is not a significant performance problem for time-based lookups, they are not handled here, but rather
 * are still handled by the database.
 *
 * @author Matthew Lohbihler
 * @author Jared Wiltshire
 */
public class DataPointRTPointValueCache {
    private final DataPointVO vo;
    private final int defaultSize;
    private final PointValueDao dao;
    private final PointValueCache pointValueCache;

    private volatile List<PointValueTime> cache;

    public DataPointRTPointValueCache(DataPointVO vo, int defaultSize, @Nullable List<PointValueTime> initialCache, PointValueDao dao, PointValueCache pointValueCache) {
        this.vo = vo;
        this.defaultSize = defaultSize;
        this.dao = dao;
        this.pointValueCache = pointValueCache;
        if (initialCache != null) {
            if (initialCache.size() > defaultSize) {
                this.cache = initialCache.subList(0, defaultSize);
            } else {
                this.cache = initialCache;
            }
        }
    }

    void savePointValueAsync(PointValueTime pvt) {
        dao.savePointValueAsync(vo, pvt);
    }

    PointValueTime savePointValueSync(PointValueTime pvt) {
        return dao.savePointValueSync(vo, pvt);
    }

    public void savePointValue(PointValueTime pvt, @Nullable SetPointSource source, boolean logValue, boolean async) {
        pvt = pvt.withAnnotationFromSource(source);
        if (logValue) {
            if (async) {
                savePointValueAsync(pvt);
            } else {
                pvt = savePointValueSync(pvt);
            }
        }

        synchronized (this) {
            if (defaultSize == 0) {
                this.cache = Collections.emptyList();
            } else if (defaultSize == 1) {
                var existing = cache;
                if (existing == null || existing.isEmpty() || existing.get(0).getTime() <= pvt.getTime()) {
                    this.cache = Collections.singletonList(pvt);
                }
            } else {
                var existing = cache;
                this.cache = new ArrayList<>(existing.size() + 1);
                cache.add(pvt);
                cache.addAll(existing);
                cache.sort(ITime.COMPARATOR.reversed());

                // remove items from end of list until list is under the size limit
                while (cache.size() > defaultSize) {
                    cache.remove(cache.size() - 1);
                }
            }

            pointValueCache.updateCache(vo, cache);
        }
    }

    /**
     * @return the latest point value, or null if none
     */
    @Nullable
    public PointValueTime getLatestPointValue() {
        List<PointValueTime> cache = getCacheContents();
        return !cache.isEmpty() ? cache.get(0) : null;
    }

    /**
     * @param limit max number of point values to return
     * @return latest point values, from cache if possible (unmodifiable)
     */
    public List<PointValueTime> getLatestPointValues(int limit) {
        List<PointValueTime> cache = getCacheContents();
        if (cache.size() >= limit) {
            return Collections.unmodifiableList(cache.subList(0, limit));
        }
        return Collections.unmodifiableList(dao.getLatestPointValues(vo, limit));
    }

    /**
     * @return unmodifiable list of cache contents, causes cache load if not already loaded.
     */
    public List<PointValueTime> getCacheContents() {
        var cache = this.cache;
        if (cache == null) {
            synchronized (this) {
                cache = this.cache;
                if (cache == null) {
                    this.cache = cache = pointValueCache.loadCache(vo, defaultSize);
                }
            }
        }
        return Collections.unmodifiableList(cache);
    }

    /**
     * Invalidate the cache, so it will be reloaded on next access.
     */
    public void invalidate(boolean invalidatePersisted) {
        synchronized (this) {
            this.cache = null;
            if (invalidatePersisted) {
                pointValueCache.deleteCache(vo);
            }
        }
    }

}
