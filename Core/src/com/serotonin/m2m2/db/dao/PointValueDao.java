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

import java.util.List;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.infiniteautomation.mango.db.query.BookendQueryCallback;
import com.infiniteautomation.mango.db.query.PVTQueryCallback;
import com.serotonin.db.WideQueryCallback;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.pair.LongPair;

interface PointValueDao {

    /**
     * Only the PointValueCache should call this method during runtime. Do not use.
     */
    PointValueTime savePointValueSync(DataPointVO vo, PointValueTime pointValue, @Nullable SetPointSource source);

    /**
     * Only the PointValueCache should call this method during runtime. Do not use.
     */
    void savePointValueAsync(DataPointVO vo, PointValueTime pointValue, @Nullable SetPointSource source);

    /**
     * Get the point values >= since
     * @param vo
     * @param since
     * @return
     */
    List<PointValueTime> getPointValues(DataPointVO vo, long since);

    /**
     * Get point values >= from and < to
     * @param vo
     * @param from
     * @param to
     * @return
     */
    List<PointValueTime> getPointValuesBetween(DataPointVO vo, long from, long to);

    /**
     * Get point values >= from and < to
     * @param vo
     * @param from
     * @param to
     * @return
     */
    List<PointValueTime> getPointValuesBetween(DataPointVO vo, long from, long to, int limit);

    /**
     * Get point values in reverse time order
     * @param vo
     * @param limit
     * @return
     */
    List<PointValueTime> getLatestPointValues(DataPointVO vo, int limit);

    /**
     * Get point values < before in reverse time order
     * @param vo
     * @param limit
     * @param before
     * @return
     */
    List<PointValueTime> getLatestPointValues(DataPointVO vo, int limit, long before);

    /**
     * Get point values < before in reverse time order
     * @param vos - one or many data points
     * @param limit - null for no limit entire series in reverse order.  If orderById = true, limit is
     *     on a per point basis else limit is for entire results
     * @param before
     * @param orderById - return results in groups per id.
     * @param callback
     * @return
     */
    void getLatestPointValues(List<DataPointVO> vos, long before, boolean orderById, Integer limit, final PVTQueryCallback<IdPointValueTime> callback);

    /**
     * Get the latest point value for this point
     * @param vo
     * @return null or value
     */
    PointValueTime getLatestPointValue(DataPointVO vo);

    /**
     * Get the first point value < time
     * @param vo
     * @param time
     * @return null or value
     */
    PointValueTime getPointValueBefore(DataPointVO vo, long time);

    /**
     * Get the point value at or just after this time
     * @param vo
     * @param time
     * @return
     */
    PointValueTime getPointValueAfter(DataPointVO vo, long time);

    /**
     * Get the point value (if any) at this time.
     * @param vo
     * @param time
     * @return null or value
     */
    PointValueTime getPointValueAt(DataPointVO vo, long time);

    /**
     * Get point values >= from and < to
     * @param vo
     * @param from
     * @param to
     * @return
     */
    void getPointValuesBetween(DataPointVO vo, long from, long to, Consumer<PointValueTime> callback);

    /**
     * Get point values >= from and < to
     * @param vos
     * @param from
     * @param to
     * @return ordered list for all values by time
     */
    void getPointValuesBetween(List<DataPointVO> vos, long from, long to,
            Consumer<IdPointValueTime> callback);

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
    void getPointValuesBetween(List<DataPointVO> vos, long from, long to, boolean orderById, Integer limit, PVTQueryCallback<IdPointValueTime> callback);

    /**
     * Query the given time series, including the nearest sample both before the 'from' timestamp and after the 'to'
     * timestamp. This query facilitates charting of values, where for continuity in the chart the values immediately
     * before and after the time range are required.
     *
     * NOTE: The preQuery and postQuery callback methods are only called if there is data before/after the query
     *
     * @param vo
     *            the target data point
     * @param from
     *            the timestamp from which to query (inclusive)
     * @param to
     *            the timestamp to which to query (exclusive)
     * @param callback
     *            the query callback
     */
    void wideQuery(DataPointVO vo, long from, long to, final WideQueryCallback<PointValueTime> callback);

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
     *
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
     *            the query callback
     */
    void wideBookendQuery(List<DataPointVO> vos, long from, long to, boolean orderById, Integer limit, final BookendQueryCallback<IdPointValueTime> callback);

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
    long getStartTime(List<DataPointVO> vos);

    /**
     * Return the latest point value's time for all point IDs
     * @param vos
     * @return latest time or -1l
     */
    long getEndTime(List<DataPointVO> vos);

    /**
     * Return the latest and earliest point value times for this list of IDs
     * @param vos
     * @return null if none exists
     */
    LongPair getStartAndEndTime(List<DataPointVO> vos);

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
