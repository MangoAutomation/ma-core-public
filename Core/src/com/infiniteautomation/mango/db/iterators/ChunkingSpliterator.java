/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.iterators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ChunkingSpliterator<T> implements Spliterator<List<T>> {

    private static final int INHERITED_CHARACTERISTICS = ORDERED | DISTINCT | SIZED | SUBSIZED | IMMUTABLE | CONCURRENT;

    private final Spliterator<T> source;
    private final int chunkSize;

    public ChunkingSpliterator(Spliterator<T> source, int chunkSize) {
        this.source = source;
        this.chunkSize = chunkSize;
    }

    @Override
    public boolean tryAdvance(Consumer<? super List<T>> action) {
        List<T> chunk = new ArrayList<>((int) Math.min(source.estimateSize(), chunkSize));
        while (source.tryAdvance(chunk::add)) {
            if (chunk.size() == chunkSize) {
                break;
            }
        }

        if (!chunk.isEmpty()) {
            action.accept(Collections.unmodifiableList(chunk));
            return true;
        }

        return false;
    }

    @Override
    public Spliterator<List<T>> trySplit() {
        Spliterator<T> split = source.trySplit();
        return split != null ? new ChunkingSpliterator<>(split, chunkSize) : null;
    }

    @Override
    public long estimateSize() {
        long sourceSize = source.estimateSize();
        if (sourceSize == 0L) return 0L;
        if (sourceSize == Long.MAX_VALUE) return Long.MAX_VALUE;

        long mod = source.estimateSize() % chunkSize;
        return source.estimateSize() / chunkSize + (mod > 0 ? 1 : 0);
    }

    @Override
    public int characteristics() {
        // our elements are never null, always a list
        return source.characteristics() & INHERITED_CHARACTERISTICS | NONNULL;
    }

    public static <X> Stream<List<X>> chunkStream(Stream<X> source, int chunkSize) {
        return StreamSupport.stream(new ChunkingSpliterator<>(source.spliterator(), chunkSize), source.isParallel())
                .onClose(source::close);
    }
}
