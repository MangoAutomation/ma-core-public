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
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.codahale.metrics.Meter;
import com.infiniteautomation.mango.db.iterators.MergingIterator;
import com.infiniteautomation.mango.db.iterators.PointValueIterator;
import com.infiniteautomation.mango.db.query.CountingConsumer;
import com.infiniteautomation.mango.db.query.LastValueConsumer;
import com.infiniteautomation.mango.db.query.SingleValueConsumer;
import com.infiniteautomation.mango.db.query.WideCallback;
import com.serotonin.m2m2.db.dao.pointvalue.AggregateDao;
import com.serotonin.m2m2.db.dao.pointvalue.DefaultAggregateDao;
import com.serotonin.m2m2.db.dao.pointvalue.StartAndEndTime;
import com.serotonin.m2m2.db.dao.pointvalue.TimeOrder;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.IdPointValueTime.MetaIdPointValueTime;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.bean.PointHistoryCount;

public interface PointValueDao {

    static void validateLimit(Integer limit) {
        if (limit != null) {
            if (limit < 0) {
                throw new IllegalArgumentException("Limit may not be negative");
            }
        }
    }

    static void validateChunkSize(int chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be greater than zero");
        }
    }

    static void validateTimePeriod(Long from, Long to) {
        if (from != null && to != null && to < from) {
            throw new IllegalArgumentException("To time must be greater than or equal to from time");
        }
    }

    static void validateNotNull(Object argument) {
        if (argument == null) {
            throw new IllegalArgumentException("Argument can't be null");
        }
    }

    /**
     * Save a stream of point values synchronously i.e. immediately.
     * This method blocks until all elements in the stream are consumed.
     *
     * @param pointValues stream of values to save
     * @throws IllegalArgumentException if pointValues is null
     */
    default void savePointValues(Stream<? extends BatchPointValue<PointValueTime>> pointValues) {
        savePointValues(pointValues, chunkSize());
    }

    /**
     * Save a stream of point values synchronously i.e. immediately.
     * This method blocks until all elements in the stream are consumed.
     *
     * @param pointValues stream of values to save
     * @param chunkSize chunk or batch size to save at once, this may be ignored by databases which support streams natively
     * @throws IllegalArgumentException if pointValues is null
     */
    default void savePointValues(Stream<? extends BatchPointValue<PointValueTime>> pointValues, int chunkSize) {
        PointValueDao.validateNotNull(pointValues);
        PointValueDao.validateChunkSize(chunkSize);
        pointValues.forEachOrdered(v -> savePointValueSync(v.getPoint(), v.getValue()));
    }

    default AggregateDao getAggregateDao(TemporalAmount aggregatePeriod) {
        return new DefaultAggregateDao(this, aggregatePeriod);
    }

    /**
     * Save a point value synchronously i.e. immediately.
     *
     * <p>If the point value implements {@link com.serotonin.m2m2.rt.dataImage.IAnnotated IAnnotated}
     * e.g. {@link com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime AnnotatedPointValueTime}, then the
     * annotation should also be stored in the database.</p>
     *
     * @param vo data point
     * @param pointValue value to save for point (may be annotated)
     * @throws IllegalArgumentException if vo or pointValue are null
     */
    PointValueTime savePointValueSync(DataPointVO vo, PointValueTime pointValue);

    /**
     * Save a point value asynchronously i.e. delayed.
     * The implementation may batch and save point values at a later time.
     *
     * <p>If the point value implements {@link com.serotonin.m2m2.rt.dataImage.IAnnotated IAnnotated}
     * e.g. {@link com.serotonin.m2m2.rt.dataImage.AnnotatedPointValueTime AnnotatedPointValueTime}, then the
     * annotation should also be stored in the database.</p>
     *
     * @param vo data point
     * @param pointValue value to save for point (may be annotated)
     * @throws IllegalArgumentException if vo or pointValue are null
     */
    void savePointValueAsync(DataPointVO vo, PointValueTime pointValue);

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
        PointValueDao.validateNotNull(vo);
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
        PointValueDao.validateNotNull(vo);
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
        PointValueDao.validateNotNull(vo);
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
        PointValueDao.validateNotNull(vo);
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
        PointValueDao.validateNotNull(vo);
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
        PointValueDao.validateNotNull(vo);
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
        PointValueDao.validateNotNull(vo);
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
        PointValueDao.validateNotNull(vo);
        PointValueDao.validateTimePeriod(from, to);
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
        PointValueDao.validateNotNull(vo);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateNotNull(callback);
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
        PointValueDao.validateNotNull(vos);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateNotNull(callback);
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
        PointValueDao.validateNotNull(vos);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateLimit(limit);
        PointValueDao.validateNotNull(sortOrder);
        PointValueDao.validateNotNull(callback);
        if (vos.isEmpty() || limit != null && limit == 0) return;

        int minChunkSize = 10;
        int maxChunkSize = chunkSize();
        // take a guess at a good chunk size to use based on number of points and total limit
        int chunkSize = limit == null ? maxChunkSize : Math.max(Math.min(limit / vos.size() + 1, maxChunkSize), minChunkSize);

        List<PointValueIterator> iterators = vos.stream()
                .map(p -> new PointValueIterator(this, p, from, to, null, sortOrder, chunkSize))
                .collect(Collectors.toList());
        Comparator<IdPointValueTime> comparator = sortOrder.getComparator().thenComparingInt(IdPointValueTime::getSeriesId);
        MergingIterator<IdPointValueTime> mergingIterator = new MergingIterator<>(iterators, comparator);

        for (int i = 0; (limit == null || i < limit) && mergingIterator.hasNext(); i++) {
            callback.accept(mergingIterator.next());
        }
    }

    /**
     * @see #streamPointValues(DataPointVO, Long, Long, Integer, TimeOrder, int)
     */
    default Stream<IdPointValueTime> streamPointValues(DataPointVO vo, @Nullable Long from, @Nullable Long to,
                                                       @Nullable Integer limit, TimeOrder sortOrder) {
        return streamPointValues(vo, from, to, limit, sortOrder, chunkSize());
    }

    /**
     * Stream the point values for a single point, for the time range {@code [from,to)}.
     * Values are streamed in either ascending or descending time order.
     *
     * <p>The values should be lazily fetched from the underlying database. If this is not supported, the values should be
     * pre-fetched in chunks of size {@link #chunkSize()} and buffered out.</p>
     *
     * <p>The limit can often be omitted, as it is only useful for implementations which pre-fetch and buffer
     * with small limits (i.e. less than the {@link #chunkSize()}).</p>
     *
     * <p>The returned {@link Stream} <strong>must</strong> be closed, use a try-with-resources block.</p>
     * <pre>{@code
     * try (var stream = streamPointValues(point, from, to, null, ASCENDING)) {
     *     // use stream
     * }
     * }</pre>
     *
     * @param vo the data point
     * @param from from time (epoch ms), inclusive
     * @param to to time (epoch ms), exclusive
     * @param limit maximum number of values to return (if null, no limit is applied)
     * @param sortOrder time order in which to return point values
     * @throws IllegalArgumentException if vo is null, if to is less than from
     */
    default Stream<IdPointValueTime> streamPointValues(DataPointVO vo, @Nullable Long from, @Nullable Long to,
                                                       @Nullable Integer limit, TimeOrder sortOrder, int chunkSize) {
        PointValueDao.validateNotNull(vo);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateLimit(limit);
        PointValueDao.validateNotNull(sortOrder);
        PointValueDao.validateChunkSize(chunkSize);

        return new PointValueIterator(this, vo, from, to, limit, sortOrder, chunkSize).toStream();
    }

    /**
     * Stream the point values for a collection of points, for the time range {@code [from,to)}.
     * Values are grouped by point, and streamed in either ascending or descending time order.
     *
     * <p>The order in which points are grouped and values are returned may not match the order of the passed in
     * collection, but is generally in order of the data point's seriesId.</p>
     *
     * <p>The values should be lazily fetched from the underlying database. If this is not supported, the values should be
     * pre-fetched in chunks of size {@link #chunkSize()} and buffered out.</p>
     *
     * <p>The limit can often be omitted, as it is only useful for implementations which pre-fetch and buffer
     * with small limits (i.e. less than the {@link #chunkSize()}).</p>
     *
     * <p>The returned {@link Stream} <strong>must</strong> be closed, use a try-with-resources block.</p>
     * <pre>{@code
     * try (var stream = streamPointValuesPerPoint(point, from, to, null, ASCENDING)) {
     *     // use stream
     * }
     * }</pre>
     *
     * @param vos data points
     * @param from from time (epoch ms), inclusive
     * @param to to time (epoch ms), exclusive
     * @param limit maximum number of values to return per point (if null, no limit is applied)
     * @param sortOrder time order in which to return point values
     * @throws IllegalArgumentException if vo is null, if to is less than from
     */
    default Stream<IdPointValueTime> streamPointValuesPerPoint(Collection<? extends DataPointVO> vos,
                                                               @Nullable Long from, @Nullable Long to,
                                                               @Nullable Integer limit, TimeOrder sortOrder) {

        if (vos.isEmpty()) return Stream.empty();
        if (vos.size() == 1) return streamPointValues(vos.iterator().next(), from, to, limit, sortOrder);

        return vos.stream().flatMap(vo -> streamPointValues(vo, from, to, limit, sortOrder));
    }

    /**
     * Stream the point values for a collection of points, for the time range {@code [from,to)}.
     * Values are streamed in either ascending or descending time order.
     *
     * <p>The values should be lazily fetched from the underlying database. If this is not supported, the values should be
     * pre-fetched in chunks of size {@link #chunkSize()} and buffered out.</p>
     *
     * <p>The limit can often be omitted, as it is only useful for implementations which pre-fetch and buffer
     * with small limits (i.e. less than the {@link #chunkSize()}).</p>
     *
     * <p>The returned {@link Stream} <strong>must</strong> be closed, use a try-with-resources block.</p>
     * <pre>{@code
     * try (var stream = streamPointValuesCombined(point, from, to, null, ASCENDING)) {
     *     // use stream
     * }
     * }</pre>
     *
     * @param vos data points
     * @param from from time (epoch ms), inclusive
     * @param to to time (epoch ms), exclusive
     * @param limit maximum number of values to return (if null, no limit is applied)
     * @param sortOrder time order in which to return point values
     * @throws IllegalArgumentException if vo is null, if to is less than from
     */
    default Stream<IdPointValueTime> streamPointValuesCombined(Collection<? extends DataPointVO> vos,
                                                               @Nullable Long from, @Nullable Long to,
                                                               @Nullable Integer limit, TimeOrder sortOrder) {

        if (vos.isEmpty()) return Stream.empty();
        if (vos.size() == 1) return streamPointValues(vos.iterator().next(), from, to, limit, sortOrder);

        Comparator<IdPointValueTime> comparator = sortOrder.getComparator().thenComparingInt(IdPointValueTime::getSeriesId);
        var streams = vos.stream()
                // limit is a total limit, but may as well limit per point
                // e.g. if user supplies a limit of 1, we may as well only retrieve a max of 1 per point
                .map(vo -> streamPointValues(vo, from, to, limit, sortOrder))
                .collect(Collectors.toList());
        var result = MergingIterator.mergeStreams(streams, comparator);
        return limit != null ? result.limit(limit) : result;
    }

    /**
     * Stream the point values for a single point, for the time range {@code [from,to)} with a limit.
     * The stream includes the point's value at the start and end of the time range ("bookend" values), for ease of charting.
     *
     * <p>The stream will always include bookend values, however the value contained within may be null.
     * If no real value exists at the bookend timestamp, then {@link PointValueTime#isBookend()} will return true.</p>
     *
     * @param point data point
     * @param from from time (epoch ms), inclusive
     * @param to to time (epoch ms), exclusive
     * @param limit maximum number of values to return (if null, no limit is applied). The limit does not apply to the bookend values.
     * @throws IllegalArgumentException if point is null, if limit is negative, if to is less than from
     * @return stream of point values in ascending time order, i.e. the oldest value first.
     */
    default Stream<IdPointValueTime> bookendStream(DataPointVO point, long from, long to, @Nullable Integer limit) {
        PointValueDao.validateNotNull(point);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateLimit(limit);

        // holds the last value in order to set the last bookend value
        LastValueConsumer<IdPointValueTime> lastValue = new LastValueConsumer<>();

        // search backwards for the first bookend value (might be at from)
        var firstBookend = Stream.generate(() -> initialValue(point, from))
                .limit(1);

        // search forwards for all values between from and to (value at exactly from will be the first bookend)
        var values = streamPointValues(point, from, to, limit, TimeOrder.ASCENDING)
                // exclude value at exactly "from" as it will be included as a bookend already
                .filter(v -> v.getTime() > from);

        // concat the first bookend and values,
        var firstBookendAndValues = Stream.concat(firstBookend, values)
                // hang onto the last seen value in the stream
                .peek(lastValue);

        // apply the last bookend
        var lastBookend = lastValue.streamValue().map(v -> v.withNewTime(to));
        return Stream.concat(firstBookendAndValues, lastBookend);
    }

    /**
     * Stream the point values for a collection of points, for the time range {@code [from,to)} with a limit.
     * The stream includes each point's value at the start and end of the time range ("bookend" values), for ease of charting.
     * Values are grouped by point, and streamed in ascending time order, i.e. the oldest value first.
     *
     * <p>Note: The order in which points are grouped and values are returned may not match the order of the passed in
     * collection</p>
     *
     * <p>The stream will always include bookend values, however the value contained within may be null.
     * If no real value exists at the bookend timestamp, then {@link PointValueTime#isBookend()} will return true.</p>
     *
     * @param points collection of data points
     * @param from from time (epoch ms), inclusive
     * @param to to time (epoch ms), exclusive
     * @param limit maximum number of values to return per point (if null, no limit is applied). The limit does not apply to the bookend values.
     * @throws IllegalArgumentException if points is null, if limit is negative, if to is less than from
     * @return stream of point values grouped by point then in ascending time order, i.e. the oldest value first.
     */
    default Stream<IdPointValueTime> bookendStreamPerPoint(Collection<? extends DataPointVO> points, long from, long to, @Nullable Integer limit) {
        PointValueDao.validateNotNull(points);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateLimit(limit);

        if (points.isEmpty()) return Stream.empty();
        if (points.size() == 1) return bookendStream(points.iterator().next(), from, to, limit);

        return points.stream().flatMap(p -> bookendStream(p, from, to, limit));
    }

    /**
     * Stream the point values for a collection of points, for the time range {@code [from,to)} with a limit.
     * The stream includes each point's value at the start and end of the time range ("bookend" values), for ease of charting.
     * Values are streamed in ascending time order, i.e. the oldest value first.
     *
     * <p>The stream will always include bookend values for each point, however the value contained within may be null.
     * If no real value exists at the bookend timestamp, then {@link PointValueTime#isBookend()} will return true.</p>
     *
     * @param points collection of data points
     * @param from from time (epoch ms), inclusive
     * @param to to time (epoch ms), exclusive
     * @param limit maximum number of values to return (if null, no limit is applied). The limit does not apply to the bookend values.
     * @throws IllegalArgumentException if points is null, if limit is negative, if to is less than from
     * @return stream of point values in ascending time order, i.e. the oldest value first.
     */
    default Stream<IdPointValueTime> bookendStreamCombined(Collection<? extends DataPointVO> points, long from, long to, @Nullable Integer limit) {
        PointValueDao.validateNotNull(points);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateLimit(limit);

        if (points.isEmpty()) return Stream.empty();
        if (points.size() == 1) return bookendStream(points.iterator().next(), from, to, limit);

        return Stream.generate(() -> {
            // search backwards for the first bookend value (might be at from)
            Map<Integer, IdPointValueTime> lastValue = initialValues(points, from);
            var firstBookend = lastValue.values().stream();

            // search forwards for all values between from and to (value at exactly from will be the first bookend)
            var values = streamPointValuesCombined(points, from, to, limit, TimeOrder.ASCENDING)
                    // exclude value at exactly "from" as it will be included as a bookend already
                    .filter(v -> v.getTime() > from)
                    // hang onto the last seen value in the stream
                    .peek(v -> lastValue.put(v.getSeriesId(), v));

            // concat the first bookend and values, hang onto the last seen value in the stream
            var firstBookendAndValues = Stream.concat(firstBookend, values);

            // apply the last bookend
            var lastBookend = lastValue.values().stream()
                    .map(v -> v.withNewTime(to));

            return Stream.concat(firstBookendAndValues, lastBookend);
        }).limit(1).flatMap(Function.identity());
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
        PointValueDao.validateNotNull(vo);
        PointValueDao.validateNotNull(callback);
        PointValueDao.validateTimePeriod(from, to);

        getPointValueBefore(vo, from).ifPresent(callback::firstValue);
        getPointValuesBetween(vo, from, to, callback);
        getPointValueAfter(vo, to).ifPresent(callback::lastValue);
    }

    /**
     * Retrieve the initial value for a set of points. That is, the value immediately prior to, or exactly at the given timestamp.
     * The returned map is guaranteed to contain an entry for every point, however the value contained inside
     * the {@link IdPointValueTime} may be null.
     *
     * <p>The returned point values have their timestamp set to the passed in timestamp, and
     * {@link PointValueTime#isBookend()} will return true if they are a synthetic "bookend" value,
     * i.e. an actual point value does not exist at this timestamp.</p>
     *
     * @param vos data points
     * @param time timestamp (epoch ms) to get the value at, inclusive
     * @return map of seriesId to point value
     */
    default Map<Integer, IdPointValueTime> initialValues(Collection<? extends DataPointVO> vos, long time) {
        PointValueDao.validateNotNull(vos);
        if (vos.isEmpty()) return Collections.emptyMap();

        Map<Integer, IdPointValueTime> values = new LinkedHashMap<>(vos.size());
        getPointValuesPerPoint(vos, null, time + 1, 1, TimeOrder.DESCENDING, v -> values.put(v.getSeriesId(), v.withNewTime(time)));
        for (DataPointVO vo : vos) {
            values.computeIfAbsent(vo.getSeriesId(), seriesId -> new MetaIdPointValueTime(seriesId, null, time, true, false));
        }
        return values;
    }

    /**
     * Retrieve the initial value for a point. That is, the value immediately prior to, or exactly at the given timestamp.
     * The return value is guaranteed to be non-null, however the value contained inside the {@link IdPointValueTime} may be null.
     *
     * <p>The returned point values have their timestamp set to the passed in timestamp, and
     * {@link PointValueTime#isBookend()} will return true if they are a synthetic "bookend" value,
     * i.e. an actual point value does not exist at this timestamp.</p>
     *
     * @param point the data point
     * @param time timestamp (epoch ms) to get the value at, inclusive
     * @return point value with time set to provided timestamp
     */
    default IdPointValueTime initialValue(DataPointVO point, long time) {
        SingleValueConsumer<IdPointValueTime> result = new SingleValueConsumer<>();
        // use Stream.generate() to make the stream lazy
        getPointValuesPerPoint(List.of(point), null, time + 1, 1, TimeOrder.DESCENDING, result);
        return result.getValue()
                .map(v -> v.withNewTime(time))
                .orElseGet(() -> new MetaIdPointValueTime(point.getSeriesId(), null, time, true, false));
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
        PointValueDao.validateNotNull(vos);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateLimit(limit);
        PointValueDao.validateNotNull(callback);
        if (vos.isEmpty()) return;

        Map<Integer, IdPointValueTime> values = initialValues(vos, from);

        LastValueConsumer<IdPointValueTime> lastValue = new LastValueConsumer<>();
        for (DataPointVO vo : vos) {
            var value = Objects.requireNonNull(values.get(vo.getSeriesId()));
            callback.firstValue(value, value.isBookend());
            lastValue.accept(value);
            getPointValuesPerPoint(Collections.singleton(vo), from, to, limit, TimeOrder.ASCENDING, v -> {
                lastValue.accept(v);
                // so we don't call row() for same value that was passed to firstValue()
                if (v.getTime() > from) {
                    callback.accept(v);
                }
            });
            callback.lastValue(lastValue.getValue().orElseThrow().withNewTime(to), true);
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
        PointValueDao.validateNotNull(vos);
        PointValueDao.validateTimePeriod(from, to);
        PointValueDao.validateLimit(limit);
        PointValueDao.validateNotNull(callback);
        if (vos.isEmpty()) return;

        Map<Integer, IdPointValueTime> values = initialValues(vos, from);
        for (IdPointValueTime value : values.values()) {
            callback.firstValue(value, value.isBookend());
        }
        getPointValuesCombined(vos, from, to, limit, TimeOrder.ASCENDING, value -> {
            values.put(value.getSeriesId(), value);
            // so we don't call row() for same value that was passed to firstValue()
            if (value.getTime() > from) {
                callback.accept(value);
            }
        });
        for (IdPointValueTime value : values.values()) {
            callback.lastValue(value.withNewTime(to), true);
        }
    }

    /**
     * Enable or disable the <strong>per point</strong> nightly purge of point values.
     * Typically, this setting should be disabled if:
     * <ul>
     *     <li>The database supports retention policies</li>
     *     <li>The database is inefficient at deleting values on a per-series basis</li>
     * </ul>
     *
     * <p>If per-point purge is disabled or the {@link #deletePointValuesBefore(com.serotonin.m2m2.vo.DataPointVO, long)}
     * method is not implemented, the data point and data source purge override settings will have no effect.</p>
     *
     * @return true to enable nightly purge of point values
     */
    default boolean enablePerPointPurge() {
        return true;
    }

    /**
     * Purge (delete) point values for all data points, for the time range {@code [-∞,endTime)}. This method is called
     * daily (typically at midnight) by the purge task. The implementation may choose to truncate the endTime to a
     * shard boundary but should never purge point values newer than endTime.
     *
     * <p>Typically, a database that supports retention policies should not implement this method,
     * i.e. throw an {@link UnsupportedOperationException}.</p>
     *
     * <p>This method is called regardless of the {@link #enablePerPointPurge()} setting.</p>
     *
     * @param endTime end of time range (epoch ms), exclusive
     * @return the number of point values deleted, return an empty optional if this will add additional overhead
     * @throws UnsupportedOperationException if the database does not support this operation
     */
    default Optional<Long> deletePointValuesBefore(long endTime) {
        throw new UnsupportedOperationException();
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
    default Optional<Long> deleteAllPointData() {
        throw new UnsupportedOperationException();
    }

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
        PointValueDao.validateNotNull(vo);
        PointValueDao.validateTimePeriod(from, to);
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
        PointValueDao.validateNotNull(vo);
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
        PointValueDao.validateNotNull(vos);
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
        PointValueDao.validateNotNull(vos);
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
        PointValueDao.validateNotNull(vos);
        if (vos.isEmpty()) return Optional.empty();
        return getStartTime(vos).flatMap(startTime -> getEndTime(vos).map(endTime -> new StartAndEndTime(startTime, endTime)));
    }

    /**
     * @return number of point values to read/write at once when streaming data
     */
    default int chunkSize() {
        return 1000;
    }

    /**
     * Retrieves series with the most point values, sorted by the number of point values.
     *
     * @param limit max number of series to return
     * @return list of points and their value counts
     */
    default List<PointHistoryCount> topPointHistoryCounts(int limit) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the write-speed in values/second. Generally a 1-minute moving average is suitable, see
     * {@link Meter#getOneMinuteRate()}.
     *
     * @return number of point values written per second
     */
    double writeSpeed();

    /**
     * @return number of point values queued for insertion via {@link #savePointValueAsync(DataPointVO, PointValueTime)}
     * that have not been written yet.
     */
    long queueSize();

    /**
     * @return number of active batch writer threads processing the queue.
     */
    int threadCount();
}
