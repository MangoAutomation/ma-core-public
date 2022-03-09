/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao.pointvalue;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
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
    TemporalAmount getAggregationPeriod();

    default Stream<SeriesValueTime<AggregateValue>> query(DataPointVO point, ZonedDateTime from, ZonedDateTime to, @Nullable Integer limit) {
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

        var aggregates = aggregate(point, from, to, Stream.concat(previousValue, rawValues));
        return limit == null ? aggregates : aggregates.limit(limit);
    }

    default Stream<SeriesValueTime<AggregateValue>> aggregate(DataPointVO point, ZonedDateTime from, ZonedDateTime to,
                                                              Stream<? extends PointValueTime> pointValues) {

        BucketCalculator bucketCalc = new TemporalAmountBucketCalculator(from, to, getAggregationPeriod());

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
     * Resamples aggregates to this DAO's {@link #getAggregationPeriod() aggregation period}, fills in any missing
     * periods with empty aggregates.
     *
     * <p>Currently only supports {@link com.serotonin.m2m2.DataType#NUMERIC NUMERIC} data points.</p>
     *
     * @param aggregates input aggregates
     * @return resampled aggregates
     * @throws IllegalArgumentException if point is not a numeric data point
     */
    default Stream<SeriesValueTime<AggregateValue>> resample(DataPointVO point, ZonedDateTime from, ZonedDateTime to,
                                                             Stream<? extends SeriesValueTime<? extends AggregateValue>> aggregates) {

        try {
            // get iterator where we can peek at the next value
            var iterator = new PeekingIterator<>(aggregates.iterator());

            // generate a stream of empty aggregates
            var initial = new NumericMultiAggregate(from.toInstant(), from.plus(getAggregationPeriod()).toInstant());
            var resampled = Stream.iterate(initial,
                    agg -> agg.getPeriodStartInstant().isBefore(to.toInstant()),
                    agg -> {
                        var start = agg.getPeriodEndInstant();
                        return new NumericMultiAggregate(start, start.plus(getAggregationPeriod()));
                    }
            ).peek(agg -> {
                // populate the aggregates with children from the original stream
                while (iterator.hasNext()) {
                    var next = iterator.peek();
                    if (agg.isInPeriod(next.getValue())) {
                        agg.addChild(iterator.next().getValue());
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

}
