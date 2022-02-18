/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.iterators;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.infiniteautomation.mango.quantize.AbstractPointValueTimeQuantizer;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.stats.StatisticsGenerator;

/**
 * Uses a {@link AbstractPointValueTimeQuantizer} to convert a stream of {@link PointValueTime} to a stream of
 * {@link StatisticsGenerator}.
 *
 * @param <T>
 */
public class StatisticsAggregator<T extends StatisticsGenerator> implements Spliterator<T> {

    private final Spliterator<? extends PointValueTime> source;
    private final long from;
    private final long to;
    private final AbstractPointValueTimeQuantizer<T> quantizer;
    private final Queue<T> stats = new ArrayDeque<>();

    private PointValueTime next;
    private boolean exhausted;
    private boolean firstValueCalled;

    public StatisticsAggregator(Spliterator<? extends PointValueTime> source,
                                AbstractPointValueTimeQuantizer<T> quantizer) {
        this.source = Objects.requireNonNull(source);
        this.quantizer = Objects.requireNonNull(quantizer);
        this.from = quantizer.getBucketCalculator().getStartTime().toInstant().toEpochMilli();
        this.to = quantizer.getBucketCalculator().getEndTime().toInstant().toEpochMilli();
        this.quantizer.setCallback(stats::add);
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        while (!exhausted && stats.isEmpty()) {
            if (!source.tryAdvance(value -> this.next = value)) {
                ensureFirstValue();
                quantizer.done();
                this.exhausted = true;
            } else if (next.getTime() < from) {
                quantizer.firstValue(next, true);
                this.firstValueCalled = true;
            } else if (next.getTime() < to) {
                ensureFirstValue();
                quantizer.accept(next);
            }
            // values >= to are consumed and ignored
        }

        T nextValue = stats.poll();
        if (nextValue != null) {
            action.accept(nextValue);
            return true;
        }
        return false;
    }

    private void ensureFirstValue() {
        if (!firstValueCalled) {
            quantizer.firstValue(new PointValueTime((DataValue) null, from), true);
            this.firstValueCalled = true;
        }
    }

    @Override
    public Spliterator<T> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return ORDERED;
    }

    public static <T extends StatisticsGenerator> Stream<T> aggregate(Stream<? extends PointValueTime> source,
                                                                      AbstractPointValueTimeQuantizer<T> quantizer) {
        StatisticsAggregator<T> spliterator = new StatisticsAggregator<>(source.spliterator(), quantizer);
        return StreamSupport.stream(spliterator, false).onClose(source::close);
    }

}
