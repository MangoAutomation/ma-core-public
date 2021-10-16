/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.db.iterators;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.collections4.iterators.PeekingIterator;

public class MergingIterator<T> implements Iterator<T> {
    private final Queue<PeekingIterator<T>> queue;
    private final Collection<? extends Iterator<? extends T>> inputs;

    private boolean initialized = false;

    /**
     * @param inputs iterators to be merged
     * @param comparator compares values from input iterators
     */
    public MergingIterator(Collection<? extends Iterator<? extends T>> inputs, Comparator<? super T> comparator) {
        this.inputs = inputs;

        // iterators are polled in order of their heads, have to ensure we don't add an empty iterator, or we will get NPE
        this.queue = new PriorityQueue<>(inputs.size(), (a, b) -> comparator.compare(a.peek(), b.peek()));
    }

    @Override
    public boolean hasNext() {
        if (!initialized) {
            initialize();
        }
        return queue.peek() != null;
    }

    @Override
    public T next() {
        if (!initialized) {
            initialize();
        }
        return nextValue(queue.remove());
    }

    /**
     * Gets the next value from the iterator, and re-inserts the iterator into the priority queue if there are more
     * values available.
     *
     * @param it iterator
     * @return iterator's next value
     */
    private T nextValue(PeekingIterator<T> it) {
        T result = it.next();
        // only re-insert iterators with data
        if (it.hasNext()) {
            // unfortunately, PriorityQueue does not allow access to it's siftDown method
            // we have to remove (remove method above) and re-add the iterator which is less efficient
            queue.offer(it);
        }
        return result;
    }

    /**
     * Creates all the iterators and adds them to the priority queue if they have a value
     */
    private void initialize() {
        // add iterators for each point to a priority queue
        for (var inputIterator : inputs) {
            PeekingIterator<T> it = new PeekingIterator<>(inputIterator);
            // only insert iterators with data
            if (it.hasNext()) {
                queue.offer(it);
            }
        }
        this.initialized = true;
    }

    public static <S> Stream<S> mergeStreams(Collection<? extends Stream<? extends S>> streams, Comparator<? super S> comparator) {
        var iterators = streams.stream().map(BaseStream::iterator).collect(Collectors.toList());
        MergingIterator<S> it = new MergingIterator<>(iterators, comparator);
        Spliterator<S> spliterator = Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.SORTED);
        return StreamSupport.stream(spliterator, false).onClose(() -> {
            RuntimeException firstException = null;
            for (var stream : streams) {
                try {
                    stream.close();
                } catch (RuntimeException e) {
                    if (firstException == null) {
                        firstException = e;
                    } else {
                        firstException.addSuppressed(e);
                    }
                }
            }
            if (firstException != null) {
                throw firstException;
            }
        });
    }
}
