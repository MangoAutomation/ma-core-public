/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.pointvalue;

import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.serotonin.m2m2.view.stats.SeriesValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * {@link AggregateDao} that can query from both pre-aggregated and raw values (on-the-fly aggregation), switching over at a
 * boundary point (may be static or relative to current time).
 *
 * @author Jared Wiltshire
 */
public interface BoundaryAggregateDao extends AggregateDao {

    /**
     * @return boundary at which to switch between querying pre-aggregated and raw values
     */
    ZonedDateTime boundary();

    /**
     * @param unit TemporalUnit
     * @return value of requested unit from boundary
     */
    long fromBoundary(TemporalUnit unit);

    /**
     * @return the period of the stored pre-aggregated values
     */
    TemporalAmount preAggregationPeriod();

    /**
     * Used to filter points for which pre-aggregation is supported, if not supported only on-the-fly aggregation is performed for this point
     * @param point point to test
     * @return true if pre-aggregation is supported for point
     */
    boolean preAggregationSupported(DataPointVO point);

    /**
     * Query the pre-aggregated values only.
     *
     * @param point data point
     * @param from from time (inclusive)
     * @param to to time (exclusive)
     * @return stream of aggregate values
     */
    Stream<SeriesValueTime<AggregateValue>> queryPreAggregated(DataPointVO point, ZonedDateTime from, ZonedDateTime to);

    @Override
    default Stream<SeriesValueTime<AggregateValue>> query(DataPointVO point, ZonedDateTime from, ZonedDateTime to, @Nullable Integer limit, TemporalAmount aggregationPeriod) {
        if (from.isEqual(to)) {
            return Stream.empty();
        }

        // boundary time at which transition occurs between aggregate database and raw value database
        ZonedDateTime boundary = boundary();

        // if pre-aggregation is not supported, or if we are querying after the boundary, fall back to real time aggregation
        if (!preAggregationSupported(point) || from.isEqual(boundary) || from.isAfter(boundary)) {
            return queryRealtime(point, from, to, limit, aggregationPeriod);
        }

        var preAggregationPeriod = preAggregationPeriod();

        /*
         * Truncates the boundary time between the aggregate and raw tables for querying across both tables.
         * This is required so that:
         * 1. The resampled aggregates contain only whole multiples of individual aggregates.
         * 2. The querying and aggregation of raw values is aligned with the start of a period.
         */
        ZonedDateTime truncatedBoundary = truncateToPeriod(boundary, preAggregationPeriod);

        Stream<SeriesValueTime<AggregateValue>> preAggregatedValues = queryPreAggregated(point,
                        min(from, truncatedBoundary),
                        min(to, truncatedBoundary));

        Stream<SeriesValueTime<AggregateValue>> onTheFlyValues = queryRealtime(point,
                max(from, truncatedBoundary), max(to, truncatedBoundary), null, preAggregationPeriod);

        Stream<SeriesValueTime<AggregateValue>> combined = Stream.concat(preAggregatedValues, onTheFlyValues);

        // Aggregator might be returning aggregates for smaller time windows, resample and combine into larger time windows.
        // Also fills in gaps where continuous aggregates are not present in the database.
        Stream<SeriesValueTime<AggregateValue>> resampled = aggregationPeriod.equals(preAggregationPeriod) ?
                combined :
                resample(point, from, to, combined, aggregationPeriod);
        return limit == null ? resampled : resampled.limit(limit);
    }

    default ZonedDateTime min(ZonedDateTime a, ZonedDateTime b) {
        return a.isBefore(b) ? a : b;
    }

    default ZonedDateTime max(ZonedDateTime a, ZonedDateTime b) {
        return a.isAfter(b) ? a : b;
    }

}
