/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.iterators;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Groups and combines values from another spliterator. The values to be grouped must be adjacent so the source
 * spliterator should be ordered appropriately for grouping.
 *
 * @author Jared Wiltshire
 */
public class GroupingSpliterator<T, R> implements Spliterator<R> {

    private static final int INHERITED_CHARACTERISTICS = ORDERED | IMMUTABLE | CONCURRENT;
    private final Spliterator<T> source;
    private final Combiner<T, R> combiner;

    /**
     * Lookahead slot
     */
    private T slot;

    public GroupingSpliterator(Spliterator<T> source, Combiner<T, R> combiner) {
        this.source = Objects.requireNonNull(source);
        this.combiner = Objects.requireNonNull(combiner);
    }

    @Override
    public boolean tryAdvance(Consumer<? super R> action) {
        R partition = null;
        do {
            if (slot != null) {
                R result = combiner.combineValue(partition, slot);
                if (result == null) {
                    action.accept(partition);
                    return true;
                }
                partition = result;
                this.slot = null;
            }
        } while (source.tryAdvance(value -> this.slot = value));

        return false;
    }

    @Override
    public Spliterator<R> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return source.characteristics() & INHERITED_CHARACTERISTICS | NONNULL;
    }

    @FunctionalInterface
    public interface Combiner<T, R> {
        /**
         * Combine a new value into the group.
         *
         * @param group the current group
         * @param value value to combine into the group
         * @return the group value. Return null if the current partition is complete and a new group should
         * be started (i.e. the value cannot be combined into the current group).
         */
        R combineValue(R group, T value);
    }

    public static <T, R> Stream<R> group(Stream<T> source, Combiner<T, R> combiner) {
        return StreamSupport.stream(new GroupingSpliterator<>(source.spliterator(), combiner), false)
                .onClose(source::close);
    }

}
