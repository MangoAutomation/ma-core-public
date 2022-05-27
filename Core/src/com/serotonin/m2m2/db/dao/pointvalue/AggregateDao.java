/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.pointvalue;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.collections4.iterators.PeekingIterator;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.infiniteautomation.mango.db.iterators.StatisticsAggregator;
import com.infiniteautomation.mango.quantize.AbstractPointValueTimeQuantizer;
import com.infiniteautomation.mango.quantize.AnalogStatisticsQuantizer;
import com.infiniteautomation.mango.quantize.BucketCalculator;
import com.infiniteautomation.mango.quantize.StartsAndRuntimeListQuantizer;
import com.infiniteautomation.mango.quantize.TemporalAmountBucketCalculator;
import com.infiniteautomation.mango.quantize.ValueChangeCounterQuantizer;
import com.serotonin.m2m2.db.dao.PointValueDao;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.view.stats.DefaultSeriesValueTime;
import com.serotonin.m2m2.view.stats.IValueTime;
import com.serotonin.m2m2.view.stats.SeriesValueTime;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * @author Jared Wiltshire
 */
public interface AggregateDao {

    PointValueDao getPointValueDao();

    /**
     * Query for aggregates in a time range.
     *
     * @param point data point
     * @param from from time (inclusive)
     * @param to to time (exclusive)
     * @param limit limit the number of returned aggregates (may be null)
     * @param aggregationPeriod aggregation period (bucket/window size)
     * @return stream of aggregate values
     */
    default Stream<SeriesValueTime<AggregateValue>> query(DataPointVO point, ZonedDateTime from, ZonedDateTime to, @Nullable Integer limit, TemporalAmount aggregationPeriod) {
        if (from.isEqual(to)) {
            return Stream.empty();
        }

        var previousValue = Stream.generate(() -> getPointValueDao().getPointValueBefore(point, from.toInstant().toEpochMilli()))
                .limit(1)
                .flatMap(Optional::stream);

        var rawValues = getPointValueDao().streamPointValues(point,
                from.toInstant().toEpochMilli(),
                to.toInstant().toEpochMilli(),
                null, TimeOrder.ASCENDING);

        var aggregates = aggregate(point, from, to, Stream.concat(previousValue, rawValues), aggregationPeriod);
        return limit == null ? aggregates : aggregates.limit(limit);
    }

    /**
     * Aggregate a stream of raw point values into aggregate statistics. Mango statistics rely on knowing the initial
     * value of the point before the "from" time, you must include an initial start value in the stream (if one exists).
     * The timestamp of this start value should be less than the "from" time.
     *
     * @param point data point
     * @param from from time (inclusive)
     * @param to to time (exclusive)
     * @param pointValues stream of point values, must include a start value (at time < from) for accurate statistics
     * @param aggregationPeriod aggregation period (bucket/window size)
     * @return stream of aggregates
     */
    default Stream<SeriesValueTime<AggregateValue>> aggregate(DataPointVO point, ZonedDateTime from, ZonedDateTime to,
                                                              Stream<? extends PointValueTime> pointValues, TemporalAmount aggregationPeriod) {

        BucketCalculator bucketCalc = new TemporalAmountBucketCalculator(from, to, aggregationPeriod);

        AbstractPointValueTimeQuantizer<?> quantizer;
        switch (point.getPointLocator().getDataType()) {
            case BINARY:
            case MULTISTATE:
                quantizer = new StartsAndRuntimeListQuantizer(bucketCalc);
                break;
            case NUMERIC:
                quantizer = new AnalogStatisticsQuantizer(bucketCalc);
                break;
            case ALPHANUMERIC:
                quantizer = new ValueChangeCounterQuantizer(bucketCalc);
                break;
            default:
                throw new IllegalStateException("Unknown data type: " + point.getPointLocator().getDataType());
        }

        Stream<AggregateValue> aggregateStream = StatisticsAggregator.aggregate(pointValues, quantizer)
                .filter(v -> v instanceof AggregateValue)
                .map(v -> (AggregateValue) v);

        return aggregateStream
                .map(v -> new DefaultSeriesValueTime<>(point.getSeriesId(), v.getPeriodStartTime(), v));
    }

    /**
     * Resamples aggregates to the aggregation period, fills in any missing periods with empty aggregates.
     *
     * <p>Currently only supports {@link com.serotonin.m2m2.DataType#NUMERIC NUMERIC} data points.</p>
     *
     * @param aggregates input aggregates
     * @param aggregationPeriod aggregation period (bucket/window size)
     * @return resampled aggregates
     * @throws IllegalArgumentException if point is not a numeric data point
     */
    default Stream<SeriesValueTime<AggregateValue>> resample(DataPointVO point, ZonedDateTime from, ZonedDateTime to,
                                                             Stream<? extends SeriesValueTime<? extends AggregateValue>> aggregates, TemporalAmount aggregationPeriod) {

        try {
            // get iterator where we can peek at the next value
            var iterator = new PeekingIterator<>(aggregates.iterator());

            // generate a stream of empty aggregates
            var initial = new NumericMultiAggregate(from.toInstant(), from.plus(aggregationPeriod).toInstant());

            var initialZoneId = from.getZone();
            var resampled = Stream.iterate(initial,
                    agg -> agg.getPeriodStartInstant().isBefore(to.toInstant()),
                    agg -> {
                        var zonedStart = agg.getPeriodEndInstant().atZone(initialZoneId);
                        return new NumericMultiAggregate(zonedStart.toInstant(), zonedStart.plus(aggregationPeriod).toInstant());
                    }
            ).peek(agg -> {
                // populate the aggregates with children from the original stream
                while (iterator.hasNext()) {
                    var next = iterator.peek();
                    if (agg.isInPeriod(next.getValue())) {
                        agg.accumulate(iterator.next().getValue());
                    } else {
                        break;
                    }
                }
            }).onClose(aggregates::close);

            return resampled
                    .map(agg -> new DefaultSeriesValueTime<>(point.getSeriesId(), agg.getPeriodStartTime(), agg));

        } catch (Exception e) {
            aggregates.close();
            throw e;
        }
    }

    default void save(DataPointVO point, Stream<? extends IValueTime<? extends AggregateValue>> aggregates, int chunkSize) {
        throw new UnsupportedOperationException();
    }

    /**
     * Truncates a date-time to align with a given period.
     *
     * @param boundary raw date-time
     * @param period period to truncate to
     * @return truncated date-time
     */
    default ZonedDateTime truncateToPeriod(ZonedDateTime boundary, TemporalAmount period) {
        var minusOnePeriod = boundary.minus(period);
        var truncated = boundary;
        for (var unit : List.of(ChronoUnit.SECONDS, ChronoUnit.MINUTES, ChronoUnit.HOURS, ChronoUnit.DAYS, ChronoUnit.MONTHS, ChronoUnit.YEARS)) {
            if (unit == ChronoUnit.MONTHS) {
                truncated = truncated.with(DAY_OF_MONTH, 1);
            } else if (unit == ChronoUnit.YEARS) {
                truncated = truncated.with(MONTH_OF_YEAR, 1);
            } else {
                truncated = truncated.truncatedTo(unit);
            }

            if (truncated.isBefore(minusOnePeriod) || truncated.isEqual(minusOnePeriod)) {
                break;
            }
        }
        while (truncated.plus(period).isBefore(boundary)) {
            truncated = truncated.plus(period);
        }
        return truncated;
    }

    /**
     * Check if pre-aggregation is supported.
     * @return true if pre-aggregation is supported
     */
    default boolean supportsPreAggregation() {
        return false;
    }

    /**
     * Enable/disable pre-aggregation.
     * @param enabled true to enable pre-aggregation
     */
    default void setPreAggregationEnabled(boolean enabled) {
        // do nothing
    }

    /**
     * Check if pre-aggregation is enabled.
     * @return true if pre-aggregation is enabled
     */
    default boolean isPreAggregationEnabled() {
        return false;
    }

    /**
     * Update the aggregates, typically called in a periodic fashion.
     * @throws UnsupportedOperationException if this AggregateDao doesn't support pre-aggregation.
     */
    default void updateAggregates() {
        throw new UnsupportedOperationException();
    }

}
