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

import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.infiniteautomation.mango.db.query.CountingConsumer;
import com.infiniteautomation.mango.db.query.SingleValueConsumer;
import com.infiniteautomation.mango.db.query.WideCallback;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.SetPointSource;
import com.serotonin.m2m2.vo.DataPointVO;

public interface PointValueDao {

    default void checkLimit(Integer limit) {
        if (limit != null) {
            if (limit < 0) {
                throw new IllegalArgumentException("Limit may not be negative");
            }
        }
    }

    default void checkTimePeriod(Long from, Long to) {
        if (from != null && to != null && to < from) {
            throw new IllegalArgumentException("To time must be greater than or equal to from time");
        }
    }

    default void checkNull(Object argument) {
        if (argument == null) {
            throw new IllegalArgumentException("Argument may not be null");
        }
    }

    enum TimeOrder {
        /**
         * Ascending time order, i.e. oldest values first
         */
        ASCENDING(Comparator.comparingLong(IdPointValueTime::getTime).thenComparingInt(IdPointValueTime::getSeriesId)),

        /**
         * Descending time order, i.e. newest values first
         */
        DESCENDING(Comparator.comparingLong(IdPointValueTime::getTime).reversed().thenComparingInt(IdPointValueTime::getSeriesId));


        private final Comparator<IdPointValueTime> comparator;

        TimeOrder(Comparator<IdPointValueTime> comparator) {
            this.comparator = comparator;
        }

        public Comparator<IdPointValueTime> getComparator() {
            return comparator;
        }
    }

    class StartAndEndTime {
        private final long startTime;
        private final long endTime;

        public StartAndEndTime(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
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
        getPointValuesPerPoint(Collections.singleton(vo), null, null, limit, TimeOrder.DESCENDING, (Consumer<? super IdPointValueTime>) values::add);
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
        getPointValuesPerPoint(Collections.singleton(vo), null, to, limit, TimeOrder.DESCENDING, (Consumer<? super IdPointValueTime>) values::add);
        return values;
    }

    /**
     * Get the latest point value for a single point.
     *
     * @param vo data point
     * @return the latest point value, i.e. the newest value.
     * @throws IllegalArgumentException if vo is null
     */
    default Optional<PointValueTime> getLatestPointValue(DataPointVO vo) {
        checkNull(vo);
        SingleValueConsumer<PointValueTime> holder = new SingleValueConsumer<>();
        getPointValuesPerPoint(Collections.singleton(vo), null, Long.MAX_VALUE, 1, TimeOrder.DESCENDING, holder);
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
        SingleValueConsumer<PointValueTime> holder = new SingleValueConsumer<>();
        getPointValuesPerPoint(Collections.singleton(vo), null, time, 1, TimeOrder.DESCENDING, holder);
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
    default Optional<PointValueTime> getPointValueAfter(DataPointVO vo, long time) {
        checkNull(vo);
        SingleValueConsumer<PointValueTime> holder = new SingleValueConsumer<>();
        getPointValuesPerPoint(Collections.singleton(vo), time, null, 1, TimeOrder.ASCENDING, holder);
        return holder.getValue();
    }

    /**
     * Get the point value at exactly the given time, for a single point.
     *
     * @param vo data point
     * @param time the time (epoch ms)
     * @return the point value exactly at the given time
     * @throws IllegalArgumentException if vo is null
     */
    default Optional<PointValueTime> getPointValueAt(DataPointVO vo, long time) {
        checkNull(vo);
        SingleValueConsumer<PointValueTime> holder = new SingleValueConsumer<>();
        getPointValuesPerPoint(Collections.singleton(vo), time, time, 1, TimeOrder.ASCENDING, holder);
        return holder.getValue();
    }

    /**
     * Get point values for a single point, for the time range {@code [from,∞)}.
     *
     * @param vo data point
     * @param from from time (epoch ms), inclusive
     * @return list of point values, in ascending time order, i.e. the oldest value first.
     * @throws IllegalArgumentException if vo is null
     */
    default List<PointValueTime> getPointValues(DataPointVO vo, long from) {
        checkNull(vo);
        List<PointValueTime> values = new ArrayList<>();
        getPointValuesPerPoint(Collections.singleton(vo), from, null, null, TimeOrder.ASCENDING, (Consumer<? super IdPointValueTime>) values::add);
        return values;
    }

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
        checkNull(vo);
        checkTimePeriod(from, to);
        List<PointValueTime> values = new ArrayList<>();
        getPointValuesPerPoint(Collections.singleton(vo), from, to, null, TimeOrder.ASCENDING, (Consumer<? super IdPointValueTime>) values::add);
        return values;
    }

    /**
     * Get point values for a single point, for the time range {@code [from,to)}.
     *
     * @param vo data point
     * @param from from time (epoch ms), inclusive
     * @param to to time (epoch ms), exclusive
     * @param callback callback to return point values, in ascending time order, i.e. the oldest value first.
     * @throws IllegalArgumentException if vo or callback are null
     */
    default void getPointValuesBetween(DataPointVO vo, long from, long to, Consumer<? super PointValueTime> callback) {
        checkNull(vo);
        checkTimePeriod(from, to);
        checkNull(callback);
        getPointValuesPerPoint(Collections.singleton(vo), from, to, null, TimeOrder.ASCENDING, callback);
    }

    /**
     * Get the point values for a collection of points, for the time range {@code [from,to)}.
     *
     * @param vos data points
     * @param from from time (epoch ms), inclusive
     * @param to to time (epoch ms), exclusive
     * @param callback callback to return point values, in ascending time order, i.e. the oldest value first.
     * @throws IllegalArgumentException if vo or callback are null
     */
    default void getPointValuesBetween(Collection<? extends DataPointVO> vos, long from, long to, Consumer<? super IdPointValueTime> callback) {
        checkNull(vos);
        checkTimePeriod(from, to);
        checkNull(callback);
        getPointValuesPerPoint(vos, from, to, null, TimeOrder.ASCENDING, callback);
    }

    /**
     * Get the point values for a collection of points, for the time range {@code [from,to)} with a limit.
     * Values are grouped by point, and returned (via callback) in either ascending or descending time order.
     *
     * <p>The order in which points are grouped and values are returned may not match the order of the passed in
     * collection, but is generally in order of the data point's seriesId.</p>
     *
     * @param vos data points
     * @param from from time (epoch ms), inclusive
     * @param to to time (epoch ms), exclusive
     * @param limit maximum number of values to return per point (if null, no limit is applied)
     * @param sortOrder time order in which to return point values
     * @param callback callback to return point values, in ascending time order, i.e. the oldest value first.
     * @throws IllegalArgumentException if vo or callback are null, if limit is negative, if to is less than from
     */
    void getPointValuesPerPoint(Collection<? extends DataPointVO> vos, @Nullable Long from, @Nullable Long to, @Nullable Integer limit, TimeOrder sortOrder, Consumer<? super IdPointValueTime> callback);

    /**
     * Get the point values for a collection of points, for the time range {@code [from,to)} with a limit.
     * Values are returned (via callback) in either ascending or descending time order.
     *
     * @param vos data points
     * @param from from time (epoch ms), inclusive
     * @param to to time (epoch ms), exclusive
     * @param limit maximum number of values to return (if null, no limit is applied)
     * @param sortOrder time order in which to return point values
     * @param callback callback to return point values, in ascending time order, i.e. the oldest value first.
     * @throws IllegalArgumentException if vo or callback are null, if limit is negative, if to is less than from
     */
    default void getPointValuesCombined(Collection<? extends DataPointVO> vos, @Nullable Long from, @Nullable Long to, @Nullable Integer limit, TimeOrder sortOrder, Consumer<? super IdPointValueTime> callback) {
        checkNull(vos);
        checkTimePeriod(from, to);
        checkLimit(limit);
        checkNull(sortOrder);
        checkNull(callback);
        if (vos.isEmpty() || limit != null && limit == 0) return;

        int minChunkSize = 10;
        int maxChunkSize = 100;
        // take a guess at a good chunk size to use based on number of points and total limit
        int chunkSize = limit == null ? maxChunkSize : Math.max(Math.min(limit / vos.size() + 1, maxChunkSize), minChunkSize);

        Comparator<IdPointValueTime> comparator = sortOrder.getComparator();
        // iterators are polled in order of their heads, have to ensure we don't add an empty iterator, or we will get NPE
        Queue<PointValueIterator> iterators = new PriorityQueue<>(vos.size(), (a, b) -> comparator.compare(a.peek(), b.peek()));

        // add iterators for each point to a priority queue
        for (var vo : vos) {
            PointValueIterator it = new PointValueIterator(this, vo, from, to, chunkSize, sortOrder);
            // only add iterators with data, our comparator does not support nulls
            if (it.hasNext()) {
                iterators.offer(it);
            }
        }

        int i = 0;
        PointValueIterator it;
        while ((it = iterators.poll()) != null) {
            // we know our iterators always have data
            callback.accept(it.next());
            if (limit != null && ++i == limit) {
                break;
            }

            // only re-add iterators with data, our comparator does not support nulls
            if (it.hasNext()) {
                // unfortunately, PriorityQueue does not allow access to it's siftDown method
                // we have to remove (poll method above) and re-add the iterator which is less efficient
                iterators.offer(it);
            }
        }
    }

    default Stream<IdPointValueTime> getPointValues(DataPointVO vo, @Nullable Long from, @Nullable Long to, TimeOrder sortOrder, int chunkSize) {
        PointValueIterator it = new PointValueIterator(this, vo, from, to, chunkSize, sortOrder);
        Spliterator<IdPointValueTime> spliterator = Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.DISTINCT | Spliterator.SORTED);
        return StreamSupport.stream(spliterator, false);
    }

    /**
     * Get the point values for a collection of points, for the time range {@code [from,to)}, while also
     * returning the value immediately prior and after the given time range.
     *
     * This query facilitates charting of values, where for continuity in the chart the values immediately
     * before and after the time range are required.
     *
     * NOTE: The preQuery and postQuery callback methods are only called if there is data before/after the time range.
     *
     * @param vo data point
     * @param from from time (epoch ms), inclusive
     * @param to to time (epoch ms), exclusive
     * @param callback callback to return point values, in ascending time order, i.e. the oldest value first.
     * @throws IllegalArgumentException if vo or callback are null, if to is less than from
     */
    default void wideQuery(DataPointVO vo, long from, long to, WideCallback<? super PointValueTime> callback) {
        checkNull(vo);
        checkNull(callback);
        checkTimePeriod(from, to);

        getPointValueBefore(vo, from).ifPresent(callback::firstValue);
        getPointValuesBetween(vo, from, to, callback);
        getPointValueAfter(vo, to).ifPresent(callback::lastValue);
    }

    /**
     * Retrieve the initial value for a set of points. That is, the value immediately prior to, or exactly at the given timestamp.
     * The returned map is guaranteed to contain an entry for every point, however the value may be null.
     *
     * @param vos data points
     * @param time timestamp (epoch ms) to get the value at, inclusive
     * @return map of seriesId to point value
     */
    default Map<Integer, IdPointValueTime> initialValues(Collection<? extends DataPointVO> vos, long time) {
        checkNull(vos);
        if (vos.isEmpty()) return Collections.emptyMap();

        Map<Integer, IdPointValueTime> values = new HashMap<>(vos.size());
        getPointValuesPerPoint(vos, null, time + 1, 1, TimeOrder.DESCENDING, v -> values.put(v.getSeriesId(), v));
        for (DataPointVO vo : vos) {
            values.computeIfAbsent(vo.getSeriesId(), seriesId -> new IdPointValueTime(seriesId, null, time));
        }
        return values;
    }

    /**
     * Get the point values for a collection of points, for the time range {@code [from,to)} with a limit.
     * Also notifies the callback of each point's value at the start and end of the time range ("bookend" values), for ease of charting.
     * Values are grouped by point, and returned (via callback) in ascending time order, i.e. the oldest value first.
     *
     * <p>The order in which points are grouped and values are returned may not match the order of the passed in
     * collection, but is generally in order of the data point's seriesId.</p>
     *
     * <p>The callback's firstValue and lastValue method will always be called for each point, the value however
     * may be null.</p>
     *
     * @param vos data points
     * @param from from time (epoch ms), inclusive
     * @param to to time (epoch ms), exclusive
     * @param limit maximum number of values to return per point (if null, no limit is applied). The limit does not apply to the "bookend" values.
     * @param callback callback to return point values, in ascending time order, i.e. the oldest value first.
     * @throws IllegalArgumentException if vo or callback are null, if limit is negative, if to is less than from
     */
    default void wideBookendQueryPerPoint(Collection<? extends DataPointVO> vos, long from, long to, @Nullable Integer limit, WideCallback<? super IdPointValueTime> callback) {
        checkNull(vos);
        checkTimePeriod(from, to);
        checkLimit(limit);
        checkNull(callback);
        if (vos.isEmpty()) return;

        Map<Integer, IdPointValueTime> values = initialValues(vos, from);

        for (DataPointVO vo : vos) {
            var first = values.get(vo.getSeriesId());
            callback.firstValue(first.withNewTime(from), first.getValue() == null || first.getTime() != from);
            getPointValuesPerPoint(Collections.singleton(vo), from, to, limit, TimeOrder.ASCENDING, v -> {
                var previousValue = Objects.requireNonNull(values.put(v.getSeriesId(), v));
                // so we don't call row() for same value that was passed to firstValue()
                if (v.getTime() > previousValue.getTime()) {
                    callback.accept(v);
                }
            });
            callback.lastValue(values.get(vo.getSeriesId()).withNewTime(to), true);
        }
    }

    /**
     * Get the point values for a collection of points, for the time range {@code [from,to)} with a limit.
     * Also notifies the callback of each point's value at the start and end of the time range ("bookend" values), for ease of charting.
     * Values are returned (via callback) in ascending time order, i.e. the oldest value first.
     *
     * <p>The callback's firstValue and lastValue method will always be called for each point, the value however
     * may be null.</p>
     *
     * @param vos data points
     * @param from from time (epoch ms), inclusive
     * @param to to time (epoch ms), exclusive
     * @param limit maximum number of values to return (if null, no limit is applied). The limit does not apply to the "bookend" values.
     * @param callback callback to return point values, in ascending time order, i.e. the oldest value first.
     * @throws IllegalArgumentException if vo or callback are null, if limit is negative, if to is less than from
     */
    default void wideBookendQueryCombined(Collection<? extends DataPointVO> vos, long from, long to, @Nullable Integer limit, WideCallback<? super IdPointValueTime> callback) {
        checkNull(vos);
        checkTimePeriod(from, to);
        checkLimit(limit);
        checkNull(callback);
        if (vos.isEmpty()) return;

        Map<Integer, IdPointValueTime> values = initialValues(vos, from);

        for (IdPointValueTime value : values.values()) {
            callback.firstValue(value.withNewTime(from), value.getValue() == null || value.getTime() != from);
        }
        getPointValuesCombined(vos, from, to, limit, TimeOrder.ASCENDING, value -> {
            var previousValue = Objects.requireNonNull(values.put(value.getSeriesId(), value));
            // so we don't call row() for same value that was passed to firstValue()
            if (value.getTime() > previousValue.getTime()) {
                callback.accept(value);
            }
        });
        for (IdPointValueTime value : values.values()) {
            callback.lastValue(value.withNewTime(to), true);
        }
    }

    /**
     * Enable or disable the nightly purge of point values.
     * Typically, a database that supports retention policies should disable the nightly purge.
     *
     * @return true to enable nightly purge of point values
     */
    default boolean enableNightlyPurge() {
        return true;
    }

    /**
     * Set a retention policy for the entire database. This method will be called after initialization and
     * whenever the point value purge settings are configured.
     *
     * @param period period for which to retain point values
     * @throws UnsupportedOperationException if this database does not support setting a retention policy
     */
    default void setRetentionPolicy(Period period) {
        throw new UnsupportedOperationException();
    }

    /**
     * Delete point values for a data point, for the time range {@code [startTime,endTime)}.
     * @param vo data point
     * @param startTime start of time range (epoch ms), inclusive
     * @param endTime end of time range (epoch ms), exclusive
     * @return the number of point values deleted, return an empty optional if this will add additional overhead
     * @throws UnsupportedOperationException if the database does not support delete
     * @throws IllegalArgumentException if vo is null
     */
    Optional<Long> deletePointValuesBetween(DataPointVO vo, @Nullable Long startTime, @Nullable Long endTime);

    /**
     * Delete point values for a data point, for the time range {@code [-∞,endTime)}.
     * @param vo data point
     * @param endTime end of time range (epoch ms), exclusive
     * @return the number of point values deleted, return an empty optional if this will add additional overhead
     * @throws UnsupportedOperationException if the database does not support delete
     * @throws IllegalArgumentException if vo is null
     */
    default Optional<Long> deletePointValuesBefore(DataPointVO vo, long endTime) {
        return deletePointValuesBetween(vo, null, endTime);
    }

    /**
     * Delete a point value for a data point at exactly the given time.
     *
     * @param vo data point
     * @param ts time (epoch ms) at which to delete point value
     * @return the number of point values deleted, return an empty optional if this will add additional overhead
     * @throws UnsupportedOperationException if the database does not support delete
     * @throws IllegalArgumentException if vo is null
     */
    default Optional<Long> deletePointValue(DataPointVO vo, long ts) {
        return deletePointValuesBetween(vo, ts, ts);
    }

    /**
     * Delete all point values for a data point, i.e. for the time range {@code [-∞,∞)}.
     * @param vo data point
     * @return the number of point values deleted, return an empty optional if this will add additional overhead
     * @throws UnsupportedOperationException if the database does not support delete
     * @throws IllegalArgumentException if vo is null
     */
    default Optional<Long> deletePointValues(DataPointVO vo) {
        return deletePointValuesBetween(vo, null, null);
    }

    /**
     * Delete all point values for all data points, i.e. for the time range {@code [-∞,∞)}.
     * @return the number of point values deleted, return an empty optional if this will add additional overhead
     * @throws UnsupportedOperationException if the database does not support delete
     * @throws IllegalArgumentException if vo is null
     */
    Optional<Long> deleteAllPointData();

    /**
     * Delete any point values that are no longer tied to a point in the {@link com.infiniteautomation.mango.db.tables.DataPoints} table.
     * @return the number of point values deleted, return an empty optional if this will add additional overhead
     * @throws UnsupportedOperationException if the database does not support delete
     */
    Optional<Long> deleteOrphanedPointValues();

    /**
     * Count the number of point values for a point, for the time range {@code [from,to)}.
     *
     * @param vo data point
     * @param from from time (epoch ms), inclusive
     * @param to to time (epoch ms), exclusive
     * @return number of point values in the time range
     * @throws IllegalArgumentException if vo is null, if to is less than from
     */
    default long dateRangeCount(DataPointVO vo, @Nullable Long from, @Nullable Long to) {
        checkNull(vo);
        checkTimePeriod(from, to);
        CountingConsumer<PointValueTime> counter = new CountingConsumer<>();
        getPointValuesPerPoint(Collections.singleton(vo), from, to, null, TimeOrder.ASCENDING, counter);
        return counter.getCount();
    }

    /**
     * Get the time of the earliest recorded point value for this point.
     *
     * @param vo data point
     * @return timestamp (epoch ms) of the first point value recorded for the point
     * @throws IllegalArgumentException if vo is null
     */
    default Optional<Long> getInceptionDate(DataPointVO vo) {
        checkNull(vo);
        return getStartTime(Collections.singleton(vo));
    }

    /**
     * Get the time of the earliest recorded point value for this collection of points.
     *
     * @param vos data points
     * @return timestamp (epoch ms) of the first point value recorded
     * @throws IllegalArgumentException if vos are null
     */
    default Optional<Long> getStartTime(Collection<? extends DataPointVO> vos) {
        checkNull(vos);
        if (vos.isEmpty()) return Optional.empty();
        SingleValueConsumer<IdPointValueTime> consumer = new SingleValueConsumer<>();
        getPointValuesPerPoint(vos, null, null, 1, TimeOrder.ASCENDING, consumer);
        return consumer.getValue().map(PointValueTime::getTime);
    }

    /**
     * Get the time of the latest recorded point value for this collection of points.
     *
     * @param vos data points
     * @return timestamp (epoch ms) of the last point value recorded
     * @throws IllegalArgumentException if vos are null
     */
    default Optional<Long> getEndTime(Collection<? extends DataPointVO> vos) {
        checkNull(vos);
        if (vos.isEmpty()) return Optional.empty();
        SingleValueConsumer<IdPointValueTime> consumer = new SingleValueConsumer<>();
        getPointValuesPerPoint(vos, null, null, 1, TimeOrder.DESCENDING, consumer);
        return consumer.getValue().map(PointValueTime::getTime);
    }

    /**
     * Get the earliest and latest timestamps for this collection of data points.
     *
     * @param vos data points
     * @return first and last timestamp (epoch ms) for the given set of points
     * @throws IllegalArgumentException if vos are null
     */
    default Optional<StartAndEndTime> getStartAndEndTime(Collection<? extends DataPointVO> vos) {
        checkNull(vos);
        if (vos.isEmpty()) return Optional.empty();
        return getStartTime(vos).flatMap(startTime -> getEndTime(vos).map(endTime -> new StartAndEndTime(startTime, endTime)));
    }

    /**
     * Get the FileData ids for point values types with corresponding files.
     * @param vo data point
     * @return list of ids
     */
    List<Long> getFiledataIds(DataPointVO vo);

}
