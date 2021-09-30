/*
    Mango - Open Source M2M - http://mango.serotoninsoftware.com
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc.
    @author Matthew Lohbihler

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General License for more details.

    You should have received a copy of the GNU General License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.infiniteautomation.mango.db.query.BookendQueryCallback;
import com.infiniteautomation.mango.db.query.PVTQueryCallback;
import com.infiniteautomation.mango.db.query.SinglePVTCallback;
import com.serotonin.db.WideQueryCallback;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.pair.LongPair;

public interface PointValueDao {

    default void checkLimit(Integer limit) {
        if (limit != null) {
            if (limit < 0) {
                throw new IllegalArgumentException("Limit may not be negative");
            }
        }
    }

    default void checkToFrom(long from, long to) {
        if (to < from) {
            throw new IllegalArgumentException("To time must be greater than or equal to from time");
        }
    }

    default void checkNull(Object argument) {
        if (argument == null) {
            throw new IllegalArgumentException("Argument may not be null");
        }
    }

    /**
     * Save a point value synchronously i.e. immediately.
     *
     * @throws IllegalArgumentException if vo or pointValue are null
     */
    PointValueTime savePointValueSync(DataPointVO vo, PointValueTime pointValue, @Nullable SetPointSource source);

    /**
     * Save a point value asynchronously i.e. delayed.
     * The implementation may batch and save point values at a later time.
     *
     * @throws IllegalArgumentException if vo or pointValue are null
     */
    void savePointValueAsync(DataPointVO vo, PointValueTime pointValue, @Nullable SetPointSource source);

    /**
     * Get point values for a single point, for the time range {@code [from,∞)}.
     *
     * @param vo data point
     * @param from from time (epoch ms), inclusive
     * @return list of point values, in ascending time order, i.e. the oldest value first.
     * @throws IllegalArgumentException if vo is null
     */
    List<PointValueTime> getPointValues(DataPointVO vo, long from);

    /**
     * Get the latest point values for a single point, with a limit.
     * Values are returned in descending time order, i.e. newest values first.
     *
     * @param vo data point
     * @param limit maximum number of values to return
     * @return list of point values, in descending time order, i.e. the newest value first.
     * @throws IllegalArgumentException if vo is null, if limit is negative
     */
    default List<PointValueTime> getLatestPointValues(DataPointVO vo, int limit) {
        checkNull(vo);
        List<PointValueTime> values = new ArrayList<>(limit);
        getLatestPointValuesPerPoint(Collections.singleton(vo), null, limit, values::add);
        return values;
    }

    /**
     * Get the latest point values for a single point, for the time range {@code [-∞,to)} with a limit.
     * Values are returned in descending time order, i.e. the newest value first.
     *
     * @param vo data point
     * @param to to time (epoch ms), exclusive
     * @param limit maximum number of values to return
     * @return list of point values, in descending time order, i.e. the newest value first.
     * @throws IllegalArgumentException if vo is null, if limit is negative
     */
    default List<PointValueTime> getLatestPointValues(DataPointVO vo, long to, int limit) {
        checkNull(vo);
        List<PointValueTime> values = new ArrayList<>(limit);
        getLatestPointValuesPerPoint(Collections.singleton(vo), to, limit, values::add);
        return values;
    }

    /**
     * Get the latest point values for a collection of points, for the time range {@code [-∞,to)} with a limit (per point).
     * Values are grouped by point, and returned (via callback) in descending time order, i.e. the newest value for each point first.
     *
     * <p>The order in which points are grouped and values are returned may not match the order of the passed in
     * collection, but is generally in order of the data point's seriesId.</p>
     *
     * @param vos data points
     * @param to to time (epoch ms), exclusive (if null, no time bound is applied)
     * @param limit maximum number of values to return (per point)
     * @param callback callback to return point values, grouped by point and in descending time order, i.e. the newest value for each point first.
     * @throws IllegalArgumentException if vos or callback are null, if limit is negative
     */
    void getLatestPointValuesPerPoint(Collection<? extends DataPointVO> vos, @Nullable Long to, int limit, PVTQueryCallback<? super IdPointValueTime> callback);

    /**
     * Get the latest point values for a collection of points, for the time range {@code [-∞,to)} with a limit (total).
     * Values are returned (via callback) in descending time order, i.e. the newest value first.
     *
     * @param vos data points
     * @param to to time (epoch ms), exclusive (if null, no time bound is applied)
     * @param limit maximum number of values to return (total)
     * @param callback callback to return point values, in descending time order, i.e. the newest value first.
     * @throws IllegalArgumentException if vos or callback are null, if limit is negative
     */
    void getLatestPointValuesCombined(Collection<? extends DataPointVO> vos, @Nullable Long to, int limit, PVTQueryCallback<? super IdPointValueTime> callback);

    /**
     * Get the latest point value for a single point.
     *
     * @param vo data point
     * @return the latest point value, i.e. the newest value.
     * @throws IllegalArgumentException if vo is null
     */
    default Optional<PointValueTime> getLatestPointValue(DataPointVO vo) {
        checkNull(vo);
        SinglePVTCallback<PointValueTime> holder = new SinglePVTCallback<>();
        getLatestPointValuesPerPoint(Collections.singleton(vo), Long.MAX_VALUE, 1, holder);
        return holder.getValue();
    }

    /**
     * Get the point value prior to the given time, for a single point.
     *
     * @param vo data point
     * @param time the time (epoch ms), exclusive
     * @return the point value prior to the given time
     * @throws IllegalArgumentException if vo is null
     */
    default Optional<PointValueTime> getPointValueBefore(DataPointVO vo, long time) {
        checkNull(vo);
        SinglePVTCallback<PointValueTime> holder = new SinglePVTCallback<>();
        getLatestPointValuesPerPoint(Collections.singleton(vo), time, 1, holder);
        return holder.getValue();
    }

    /**
     * Get the point value at, or after the given time, for a single point.
     *
     * @param vo data point
     * @param time the time (epoch ms), inclusive
     * @return the point value at, or after the given time
     * @throws IllegalArgumentException if vo is null
     */
    Optional<PointValueTime> getPointValueAfter(DataPointVO vo, long time);

    /**
     * Get the point value at exactly the given time, for a single point.
     *
     * @param vo data point
     * @param time the time (epoch ms)
     * @return the point value exactly at the given time
     * @throws IllegalArgumentException if vo is null
     */
    Optional<PointValueTime> getPointValueAt(DataPointVO vo, long time);

    /**
     * Get point values for a single point, for the time range {@code [from,to)}.
     *
     * @param vo data point
     * @param from from time (epoch ms), inclusive
     * @param to to time (epoch ms), exclusive
     * @return list of point values, in ascending time order, i.e. the oldest value first.
     * @throws IllegalArgumentException if vo is null
     */
    default List<PointValueTime> getPointValuesBetween(DataPointVO vo, long from, long to) {
        return getPointValuesBetween(vo, from, to, (Integer) null);
    }

    /**
     * Get point values for a single point, for the time range {@code [from,to)} with a limit.
     *
     * @param vo data point
     * @param from from time (epoch ms), inclusive
     * @param to to time (epoch ms), exclusive
     * @param limit maximum number of values to return (if null, no limit is applied)
     * @return list of point values, in ascending time order, i.e. the oldest value first.
     * @throws IllegalArgumentException if vo is null, if limit is negative
     */
    List<PointValueTime> getPointValuesBetween(DataPointVO vo, long from, long to, @Nullable Integer limit);

    /**
     * Get point values for a single point, for the time range {@code [from,to)}.
     *
     * @param vo data point
     * @param from from time (epoch ms), inclusive
     * @param to to time (epoch ms), exclusive
     * @param callback callback to return point values, in ascending time order, i.e. the oldest value first.
     * @throws IllegalArgumentException if vo or callback are null
     */
    void getPointValuesBetween(DataPointVO vo, long from, long to, PVTQueryCallback<? super PointValueTime> callback);

    /**
     * Get the point values for a collection of points, for the time range {@code [from,to)}.
     *
     * @param vos data points
     * @param from from time (epoch ms), inclusive
     * @param to to time (epoch ms), exclusive
     * @param callback callback to return point values, in ascending time order, i.e. the oldest value first.
     * @throws IllegalArgumentException if vo or callback are null
     */
    void getPointValuesBetween(Collection<? extends DataPointVO> vos, long from, long to, PVTQueryCallback<? super IdPointValueTime> callback);

    /**
     * Get point values >= from and < to
     * @param vos
     * @param from
     * @param to
     * @param orderById - return the list in time order per data point
     * @param limit - optional limit.  If orderById = true then limit is per id, else limit is on total results
     * @param callback
     * @return ordered list for all values by time
     */
    void getPointValuesBetween(Collection<? extends DataPointVO> vos, long from, long to, boolean orderById, Integer limit, PVTQueryCallback<? super IdPointValueTime> callback);

    /**
     * Query the given time series, including the nearest sample both before the 'from' timestamp and after the 'to'
     * timestamp. This query facilitates charting of values, where for continuity in the chart the values immediately
     * before and after the time range are required.
     *
     * NOTE: The preQuery and postQuery callback methods are only called if there is data before/after the query
     * @param vo
     *            the target data point
     * @param from
     *            the timestamp from which to query (inclusive)
     * @param to
*            the timestamp to which to query (exclusive)
     * @param callback
     */
    default void wideQuery(DataPointVO vo, long from, long to, WideQueryCallback<? super PointValueTime> callback) {
        getPointValueBefore(vo, from).ifPresent(callback::preQuery);
        getPointValuesBetween(vo, from, to, callback::row);
        getPointValueAfter(vo, to).ifPresent(callback::postQuery);
    }

    /**
     * Get point values >= from and < to, bookend the query by calling:
     *
     *   callback.preQuery with either the value exactly at from or the value
     *    before from with from as the timestamp (can be null if nothing at or before from)
     *
     *   callback.postQuery with either the value the value most recently
     *    before 'to' with 'to' as the timestamp (can be null if nothing before to)
     *
     * NOTE: The beforeQuery and afterQuery methods are called once for every data point ID
     * @param vos
     *            the target data points
     * @param from
     *            the timestamp from which to query (inclusive)
     * @param to
*            the timestamp to which to query (exclusive)
     * @param orderById
*            should the results also be ordered by data point id
     * @param limit
*            the limit of results, null for no limit (Limits do not include the bookends so the returned count can be up to limit + 2)
     * @param callback
     */
    void wideBookendQuery(Collection<? extends DataPointVO> vos, long from, long to, boolean orderById, Integer limit, final BookendQueryCallback<? super IdPointValueTime> callback);

    /**
     * Delete startTime <= values < endTime
     * @param vo
     * @param startTime
     * @param endTime
     * @return
     */
    long deletePointValuesBetween(DataPointVO vo, long startTime, long endTime);

    /**
     * Delete values < time
     * @param vo
     * @param time
     * @return
     */
    long deletePointValuesBefore(DataPointVO vo, long time);

    /**
     * Delete values < time and don't count what was deleted
     * @param vo
     * @param time
     * @return
     */
    boolean deletePointValuesBeforeWithoutCount(DataPointVO vo, long time);

    /**
     * Delete all values
     * @param vo
     * @return
     */
    long deletePointValues(DataPointVO vo);

    /**
     * Delete all values
     * @param vo
     * @return true if any data was deleted
     */
    boolean deletePointValuesWithoutCount(DataPointVO vo);

    /**
     * Delete values for all points
     * @return
     */
    long deleteAllPointData();

    /**
     * Delete values for all points without counting them
     * @return
     */
    void deleteAllPointDataWithoutCount();

    /**
     * Delete any point values that are no longer tied to a point in the Data Points table
     * @return
     */
    long deleteOrphanedPointValues();

    /**
     * Delete any point values that are no longer tied to a point in the Data Points table but don't count the amount deleted
     */
    void deleteOrphanedPointValuesWithoutCount();

    /**
     * SQL Specific to delete annotations if they are stored elsewhere
     */
    void deleteOrphanedPointValueAnnotations();

    /**
     * Count the values >= from and < to
     * @param vo
     * @param from
     * @param to
     * @return
     */
    long dateRangeCount(DataPointVO vo, long from, long to);

    /**
     * Get the earliest timestamp for this point
     * @param vo
     * @return
     */
    long getInceptionDate(DataPointVO vo);

    /**
     * Return the earliest point value's time for all point IDs
     * @param vos
     * @return earliest ts or 0
     */
    long getStartTime(Collection<? extends DataPointVO> vos);

    /**
     * Return the latest point value's time for all point IDs
     * @param vos
     * @return latest time or -1l
     */
    long getEndTime(Collection<? extends DataPointVO> vos);

    /**
     * Return the latest and earliest point value times for this list of IDs
     * @param vos
     * @return null if none exists
     */
    LongPair getStartAndEndTime(Collection<? extends DataPointVO> vos);

    /**
     * Get the FileData ids for point values types with corresponding files.
     * @param vo
     * @return
     */
    List<Long> getFiledataIds(DataPointVO vo);

    /**
     * Delete all data point values at a time
     * @param vo
     * @param ts
     * @return
     */
    long deletePointValue(DataPointVO vo, long ts);

}
