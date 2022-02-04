/*
 * Copyright (C) 2022 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.iterators;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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

    private R group;

    public GroupingSpliterator(Spliterator<T> source, Combiner<T, R> combiner) {
        this.source = Objects.requireNonNull(source);
        this.combiner = Objects.requireNonNull(combiner);
    }

    @Override
    public boolean tryAdvance(Consumer<? super R> action) {
        R prevGroup = group;
        while (source.tryAdvance(this::combineValue)) {
            if (group != prevGroup && prevGroup != null) {
                action.accept(prevGroup);
                return true;
            }
            prevGroup = group;
        }

        this.group = null;
        if (prevGroup != null) {
            action.accept(prevGroup);
            return true;
        }

        return false;
    }

    private void combineValue(T value) {
        this.group = Objects.requireNonNull(combiner.combineValue(group, value));
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
         * Combine a new value into the group, or start a new group.
         *
         * @param group the current group (null if opening first group)
         * @param value value to combine into the group
         * @return the current group or a new group. If a new group is returned, the previous group will be closed out.
         */
        @NonNull R combineValue(@Nullable R group, @Nullable T value);
    }

    public static <T, R> Stream<R> group(Stream<T> source, Combiner<T, R> combiner) {
        return StreamSupport.stream(new GroupingSpliterator<>(source.spliterator(), combiner), false)
                .onClose(source::close);
    }

}
