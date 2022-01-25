/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.iterators;

import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.serotonin.m2m2.rt.dataImage.PointValueTime;

public class WindowAggregator implements Spliterator<WindowAggregate> {

    private final Spliterator<? extends PointValueTime> source;

    private final ZonedDateTime from;
    private final ZonedDateTime to;
    private final TemporalAmount windowPeriod;
    private PointValueTime next;
    private WindowAggregate window;

    public WindowAggregator(Spliterator<? extends PointValueTime> source, ZonedDateTime from, ZonedDateTime to, TemporalAmount windowPeriod) {
        this.source = Objects.requireNonNull(source);
        this.from = Objects.requireNonNull(from);
        this.to = Objects.requireNonNull(to);
        this.windowPeriod = Objects.requireNonNull(windowPeriod);

        if (!from.getZone().equals(to.getZone())) {
            throw new IllegalArgumentException("Zones are different");
        }
    }

    private ZonedDateTime truncate(ZonedDateTime from) {
        // TODO
        return from.truncatedTo(windowPeriod.getUnits().get(0));
    }

    @Override
    public boolean tryAdvance(Consumer<? super WindowAggregate> action) {
        ZonedDateTime windowStart = window == null ? truncate(from) : window.getEnd();
        if (windowStart.isEqual(to) || windowStart.isAfter(to)) {
            return false;
        }
        this.window = new WindowAggregate(windowStart, windowStart.plus(windowPeriod));

        do {
            if (next != null) {
                if (!window.accumulate(next)) {
                    break;
                }
            }
        } while (source.tryAdvance(this::setNext));

        action.accept(window);
        return true;
    }

    private void setNext(PointValueTime value) {
        this.next = value;
    }

    @Override
    public Spliterator<WindowAggregate> trySplit() {
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

    public static Stream<WindowAggregate> aggregate(Stream<? extends PointValueTime> source,
                                                    ZonedDateTime from,
                                                    ZonedDateTime to,
                                                    TemporalAmount windowPeriod) {
        WindowAggregator spliterator = new WindowAggregator(source.spliterator(), from, to, windowPeriod);
        return StreamSupport.stream(spliterator, false).onClose(source::close);
    }

}
